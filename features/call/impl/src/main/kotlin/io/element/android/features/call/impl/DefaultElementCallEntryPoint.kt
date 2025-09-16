/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl

import android.content.Context
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.features.call.api.CallType
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.call.impl.notifications.CallNotificationData
import io.element.android.features.call.impl.utils.ActiveCallManager
import io.element.android.features.call.impl.utils.IntentProvider
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.isDm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "CallEntry"

@ContributesBinding(AppScope::class)
class DefaultElementCallEntryPoint @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeCallManager: ActiveCallManager,
    private val matrixClientProvider: MatrixClientProvider,
    private val coroutineScope: CoroutineScope,
) : ElementCallEntryPoint {
    companion object {
        const val EXTRA_CALL_TYPE = "EXTRA_CALL_TYPE"
        const val IS_AUDIO_CALL = "IS_AUDIO_CALL"
        const val IS_INCOMING_CALL = "IS_INCOMING_CALL"
        const val REQUEST_CODE = 2255
    }

    override fun startCall(callType: CallType, isAudioCall: Boolean?) {
        Timber.tag(TAG).i("🚀 Starting INSTANT call of type: $callType (WhatsApp-like speed)")
        
        when (callType) {
            is CallType.RoomCall -> {
                // Launch coroutine to fetch room name from Matrix SDK before starting call
                coroutineScope.launch {
                    try {
                        val matrixClient = matrixClientProvider.getOrRestore(callType.sessionId).getOrNull()
                        val room = matrixClient?.getRoom(callType.roomId)
                        val roomName = room?.displayName ?: room?.roomId?.value ?: "Conference"
                        Timber.tag(TAG).i("Starting call with room name: $roomName for roomId: ${callType.roomId}")
                        
                        // Now start the activity with the proper room name available
                        context.startActivity(IntentProvider.createIntent(context, callType, isAudioCall, false, roomName))
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error fetching room name, starting call anyway")
                        // Fallback: start call without room name
                        context.startActivity(IntentProvider.createIntent(context, callType, isAudioCall, false))
                    }
                }
            }
            else -> {
                // For non-room calls, proceed immediately
                context.startActivity(IntentProvider.createIntent(context, callType, isAudioCall, false))
            }
        }
    }

    override fun handleIncomingCall(
        callType: CallType.RoomCall,
        eventId: EventId,
        senderId: UserId,
        roomName: String?,
        senderName: String?,
        avatarUrl: String?,
        timestamp: Long,
        notificationChannelId: String,
        textContent: String?,
    ) {
        Timber.tag(TAG).i("Handling incoming call from: $senderId in room: ${callType.roomId}")
        
        // Launch a coroutine to determine if this is a DM call
        coroutineScope.launch {
            try {
                val matrixClient = matrixClientProvider.getOrRestore(callType.sessionId).getOrNull()
                val room = matrixClient?.getRoom(callType.roomId)
                val isDm = room?.isDm ?: false
                Timber.tag(TAG).d("Room isDm: $isDm for roomId: ${callType.roomId}")
                
                val incomingCallNotificationData = CallNotificationData(
                    sessionId = callType.sessionId,
                    roomId = callType.roomId,
                    eventId = eventId,
                    senderId = senderId,
                    roomName = roomName,
                    senderName = senderName,
                    avatarUrl = avatarUrl,
                    timestamp = timestamp,
                    notificationChannelId = notificationChannelId,
                    textContent = textContent,
                    isDm = isDm,
                )
                activeCallManager.registerIncomingCall(notificationData = incomingCallNotificationData)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error determining room type, defaulting to group call")
                // Fallback: create notification data with isDm = false (assume group call)
                val fallbackNotificationData = CallNotificationData(
                    sessionId = callType.sessionId,
                    roomId = callType.roomId,
                    eventId = eventId,
                    senderId = senderId,
                    roomName = roomName,
                    senderName = senderName,
                    avatarUrl = avatarUrl,
                    timestamp = timestamp,
                    notificationChannelId = notificationChannelId,
                    textContent = textContent,
                    isDm = false, // Default to group call if we can't determine
                )
                activeCallManager.registerIncomingCall(notificationData = fallbackNotificationData)
            }
        }
    }
}
