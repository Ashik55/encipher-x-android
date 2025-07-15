/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.appconfig.ElementCallConfig
import io.element.android.features.call.api.CallType
import io.element.android.features.call.api.CurrentCall
import io.element.android.features.call.impl.notifications.CallNotificationData
import io.element.android.features.call.impl.notifications.RingingCallNotificationCreator
import io.element.android.features.call.impl.services.IncomingCallForegroundService
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.push.api.notifications.ForegroundServiceType
import io.element.android.libraries.push.api.notifications.NotificationIdProvider
import io.element.android.libraries.push.api.notifications.OnMissedCallNotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Manages the active call state.
 */
interface ActiveCallManager {
    /**
     * The active call state flow, which will be updated when the active call changes.
     */
    val activeCall: StateFlow<ActiveCall?>

    /**
     * Registers an incoming call if there isn't an existing active call and posts a [CallState.Ringing] notification.
     *
     * @param notificationData The data for the incoming call notification.
     */
    fun registerIncomingCall(notificationData: CallNotificationData)

    /**
     * Called when the active call has been hung up. It will remove any existing UI and the active call.
     * @param callType The type of call that the user hung up, either an external url one or a room one.
     */
    fun hungUpCall(callType: CallType)

    /**
     * Called after the user joined a call. It will remove any existing UI and set the call state as [CallState.InCall].
     *
     * @param callType The type of call that the user joined, either an external url one or a room one.
     */
    fun joinedCall(callType: CallType)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultActiveCallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onMissedCallNotificationHandler: OnMissedCallNotificationHandler,
    private val ringingCallNotificationCreator: RingingCallNotificationCreator,
    private val notificationManagerCompat: NotificationManagerCompat,
    private val matrixClientProvider: MatrixClientProvider,
    private val defaultCurrentCallService: DefaultCurrentCallService,
) : ActiveCallManager {
    private var timedOutCallJob: Job? = null
    // Store the most recently processed event ID to avoid duplicate notifications
    private var lastProcessedEventId: String? = null
    // Track when the current call started ringing to avoid reacting to old events
    private var callRingingStartTime: Long? = null

    override val activeCall = MutableStateFlow<ActiveCall?>(null)

    init {
        observeRingingCall()
        observeCurrentCall()
        observeCallEndedMessages()
    }

    override fun registerIncomingCall(notificationData: CallNotificationData) {
        // Check if this is the same event we just processed
        if (notificationData.eventId.value == lastProcessedEventId) {
            Timber.d("Duplicate call notification event detected, ignoring: ${notificationData.eventId.value}")
            return
        }
        
        // Additional time-based check to prevent rapid successive calls
        val currentTime = System.currentTimeMillis()
        if (callRingingStartTime != null && (currentTime - callRingingStartTime!!) < 1000) {
            Timber.d("Rapid successive call detected, ignoring: ${notificationData.eventId.value}")
            return
        }
        
        // Store this event ID to prevent duplicate processing
        lastProcessedEventId = notificationData.eventId.value
        
        if (activeCall.value != null) {
            displayMissedCallNotification(notificationData)
            Timber.w("Already have an active call, ignoring incoming call: $notificationData")
            return
        }
        
        activeCall.value = ActiveCall(
            callType = CallType.RoomCall(
                sessionId = notificationData.sessionId,
                roomId = notificationData.roomId,
            ),
            callState = CallState.Ringing(notificationData),
        )

        // Track when this call started ringing
        callRingingStartTime = System.currentTimeMillis()

        timedOutCallJob?.cancel()
        timedOutCallJob = coroutineScope.launch {
            // Start the foreground service for persistent notification (with sound as fallback)
            startIncomingCallForegroundService(notificationData)
            
            // Don't show the regular ringing notification to avoid double sound
            // The foreground service handles both notification and ringtone
            // showIncomingCallNotification(notificationData)

            // Wait for the ringing call to time out
            delay(ElementCallConfig.RINGING_CALL_DURATION_SECONDS.seconds)
            incomingCallTimedOut(displayMissedCallNotification = true)
        }
    }

    /**
     * Called when the incoming call timed out. It will remove the active call and remove any associated UI, adding a 'missed call' notification.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun incomingCallTimedOut(displayMissedCallNotification: Boolean) {
        val previousActiveCall = activeCall.value ?: return
        val notificationData = (previousActiveCall.callState as? CallState.Ringing)?.notificationData ?: return
        activeCall.value = null

        cancelIncomingCallNotification()
        stopIncomingCallForegroundService()

        if (displayMissedCallNotification) {
            displayMissedCallNotification(notificationData)
        }
        
        // Clear both ringing start time and last processed event ID when call times out
        callRingingStartTime = null
        lastProcessedEventId = null
    }

    override fun hungUpCall(callType: CallType) {
        if (activeCall.value?.callType != callType) {
            Timber.w("Call type $callType does not match the active call type, ignoring")
            return
        }
        
        // Send call cancellation event to notify other participants
        if (callType is CallType.RoomCall) {
            sendCallCancellationEvent(callType.sessionId, callType.roomId)
        }
        
        cancelIncomingCallNotification()
        stopIncomingCallForegroundService()
        timedOutCallJob?.cancel()
        activeCall.value = null
        
        // Reset the last processed event ID and ringing start time when call ends
        lastProcessedEventId = null
        callRingingStartTime = null
    }

    override fun joinedCall(callType: CallType) {
        cancelIncomingCallNotification()
        stopIncomingCallForegroundService()
        timedOutCallJob?.cancel()

        activeCall.value = ActiveCall(
            callType = callType,
            callState = CallState.InCall,
        )
        
        // Reset the last processed event ID and ringing start time when call state changes
        lastProcessedEventId = null
        callRingingStartTime = null
    }

    /**
     * Sends a call cancellation event to the room to notify other participants that the call has ended.
     * This will help stop ringtones on other devices.
     */
    private fun sendCallCancellationEvent(sessionId: SessionId, roomId: RoomId) {
        coroutineScope.launch {
            try {
                val matrixClient = matrixClientProvider.getOrRestore(sessionId).getOrNull()
                val room = matrixClient?.getRoom(roomId)
                
                if (room != null) {
                    // Send a simple message to indicate call ended
                    room.sendMessage(
                        body = "Call ended",
                        htmlBody = null,
                        intentionalMentions = emptyList()
                    ).onSuccess {
                        // Only log when the message send operation is successful
                        // The actual timeline appearance will be handled by the room state monitoring
                        Timber.d("Call ended message queued for room: $roomId")
                    }.onFailure { error ->
                        Timber.e(error, "Failed to send call cancellation message to room: $roomId")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error sending call cancellation message")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun showIncomingCallNotification(notificationData: CallNotificationData) {
        val notification = ringingCallNotificationCreator.createNotification(
            sessionId = notificationData.sessionId,
            roomId = notificationData.roomId,
            eventId = notificationData.eventId,
            senderId = notificationData.senderId,
            roomName = notificationData.roomName,
            senderDisplayName = notificationData.senderName ?: notificationData.senderId.value,
            roomAvatarUrl = notificationData.avatarUrl,
            notificationChannelId = notificationData.notificationChannelId,
            timestamp = notificationData.timestamp,
            textContent = notificationData.textContent,
        ) ?: return
        runCatching {
            notificationManagerCompat.notify(
                NotificationIdProvider.getForegroundServiceNotificationId(ForegroundServiceType.INCOMING_CALL),
                notification,
            )
        }.onFailure {
            Timber.e(it, "Failed to publish notification for incoming call")
        }
    }

    private fun cancelIncomingCallNotification() {
        notificationManagerCompat.cancel(NotificationIdProvider.getForegroundServiceNotificationId(ForegroundServiceType.INCOMING_CALL))
    }

    private fun displayMissedCallNotification(notificationData: CallNotificationData) {
        coroutineScope.launch {
            onMissedCallNotificationHandler.addMissedCallNotification(
                sessionId = notificationData.sessionId,
                roomId = notificationData.roomId,
                eventId = notificationData.eventId,
            )
        }
    }

    /**
     * Starts the foreground service for persistent incoming call notification.
     */
    private fun startIncomingCallForegroundService(notificationData: CallNotificationData) {
        try {
            IncomingCallForegroundService.startService(context, notificationData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start incoming call foreground service")
        }
    }

    /**
     * Stops the foreground service for persistent incoming call notification.
     */
    private fun stopIncomingCallForegroundService() {
        try {
            IncomingCallForegroundService.stopService(context)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop incoming call foreground service")
        }
    }

    /**
     * Enhanced observation of ringing calls to detect when calls are cancelled by other participants.
     * This will ensure ringtones stop immediately when the call is cancelled.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeRingingCall() {
        // This will observe ringing calls and ensure they're terminated if the room call is cancelled or if the user
        // has joined the call from another session.
        activeCall
            .filterNotNull()
            .filter { it.callState is CallState.Ringing && it.callType is CallType.RoomCall }
            .flatMapLatest { activeCall ->
                val callType = activeCall.callType as CallType.RoomCall
                // Get a flow of updated `hasRoomCall` and `activeRoomCallParticipants` values for the room
                matrixClientProvider.getOrRestore(callType.sessionId).getOrNull()
                    ?.getRoom(callType.roomId)
                    ?.roomInfoFlow
                    ?.map {
                        it.hasRoomCall to (callType.sessionId in it.activeRoomCallParticipants)
                    }
                    ?: flowOf()
            }
            // We only want to check if the room active call status changes
            .distinctUntilChanged()
            // Skip the first one, we're not interested in it (if the check below passes, it had to be active anyway)
            .drop(1)
            .onEach { (roomHasActiveCall, userIsInTheCall) ->
                if (!roomHasActiveCall) {
                    // The call was cancelled - stop the ringtone immediately
                    Timber.d("Call was cancelled by another participant, stopping ringtone")
                    timedOutCallJob?.cancel()
                    incomingCallTimedOut(displayMissedCallNotification = true)
                } else if (userIsInTheCall) {
                    // The user joined the call from another session
                    Timber.d("User joined call from another session, stopping ringtone")
                    timedOutCallJob?.cancel()
                    incomingCallTimedOut(displayMissedCallNotification = false)
                }
            }
            .launchIn(coroutineScope)
    }

    private fun observeCurrentCall() {
        activeCall
            .onEach { value ->
                if (value == null) {
                    defaultCurrentCallService.onCallEnded()
                } else {
                    when (value.callState) {
                        is CallState.Ringing -> {
                            // Nothing to do
                        }
                        is CallState.InCall -> {
                            when (val callType = value.callType) {
                                is CallType.ExternalUrl -> defaultCurrentCallService.onCallStarted(CurrentCall.ExternalUrl(callType.url))
                                is CallType.RoomCall -> defaultCurrentCallService.onCallStarted(CurrentCall.RoomCall(callType.roomId))
                            }
                        }
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    /**
     * Observes room state changes for "Call ended" messages and automatically stops ringing notifications.
     * This ensures that when one participant cancels/ends a call, other participants' ringtones stop immediately.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCallEndedMessages() {
        activeCall
            .filterNotNull()
            .filter { it.callState is CallState.Ringing && it.callType is CallType.RoomCall }
            .flatMapLatest { activeCall ->
                val callType = activeCall.callType as CallType.RoomCall
                val ringingStartTime = callRingingStartTime ?: System.currentTimeMillis()
                
                // Monitor room state for call cancellation by observing room info changes
                // Only react to changes that happen after the call started ringing
                matrixClientProvider.getOrRestore(callType.sessionId).getOrNull()
                    ?.getRoom(callType.roomId)
                    ?.roomInfoFlow
                    ?.drop(1) // Skip the initial state to only react to changes that happen after ringing starts
                    ?.filter { 
                        // Only react to state changes that happen after ringing started
                        System.currentTimeMillis() - ringingStartTime > 1000 // At least 1 second after ringing started
                    }
                    ?.map { roomInfo ->
                        // If the room no longer has an active call, it means the call was cancelled
                        !roomInfo.hasRoomCall
                    }
                    ?: flowOf(false)
            }
            .filter { callWasCancelled -> callWasCancelled }
            .onEach {
                Timber.d("Call ended message delivered - ringtone stopped for room")
                timedOutCallJob?.cancel()
                incomingCallTimedOut(displayMissedCallNotification = false)
            }
            .launchIn(coroutineScope)
    }
}

/**
 * Represents an active call.
 */
data class ActiveCall(
    val callType: CallType,
    val callState: CallState,
)

/**
 * Represents the state of an active call.
 */
sealed interface CallState {
    /**
     * The call is in a ringing state.
     * @param notificationData The data for the incoming call notification.
     */
    data class Ringing(val notificationData: CallNotificationData) : CallState

    /**
     * The call is in an in-call state.
     */
    data object InCall : CallState
}
