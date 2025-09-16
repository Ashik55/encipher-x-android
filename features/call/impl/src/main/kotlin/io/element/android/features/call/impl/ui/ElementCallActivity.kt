/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.ui

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.IntentCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import io.element.android.features.call.api.CallType
import io.element.android.features.call.api.CallType.ExternalUrl
import io.element.android.features.call.impl.DefaultElementCallEntryPoint
import io.element.android.features.call.impl.di.CallBindings
import io.element.android.features.call.impl.pip.PictureInPictureEvents
import io.element.android.features.call.impl.pip.PictureInPicturePresenter
import io.element.android.features.call.impl.pip.PictureInPictureState
import io.element.android.features.call.impl.pip.PipView
import io.element.android.features.call.impl.services.CallForegroundService
import io.element.android.features.call.impl.utils.CallIntentDataParser
import io.element.android.features.call.impl.utils.ActiveCallManager
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.core.log.logger.LoggerTag
import io.element.android.libraries.designsystem.theme.ElementThemeApp
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.matrix.impl.call.model.CallRequestBody
import io.element.android.libraries.matrix.impl.call.model.EndCallRequest
import io.element.android.libraries.matrix.impl.call.services.CallApiService
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import io.element.android.features.call.impl.config.JitsiConfigurationBuilder

private val loggerTag = LoggerTag("ElementCallActivity")

class ElementCallActivity :
    AppCompatActivity(),
    CallScreenNavigator,
    PipView {

    private val broadcastReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onBroadcastReceived(intent)
        }
    }
    @Inject lateinit var callIntentDataParser: CallIntentDataParser
    @Inject lateinit var presenterFactory: CallScreenPresenter.Factory
    @Inject lateinit var appPreferencesStore: AppPreferencesStore
    @Inject lateinit var enterpriseService: EnterpriseService
    @Inject lateinit var pictureInPicturePresenter: PictureInPicturePresenter
    @Inject lateinit var activeCallManager: ActiveCallManager

    @Inject lateinit var callApiService: CallApiService
    @Inject lateinit var authenticationService: io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService

    private lateinit var presenter: Presenter<CallScreenState>

    private lateinit var audioManager: AudioManager

    private var requestPermissionCallback: RequestPermissionCallback? = null

    private var audiofocusRequest: AudioFocusRequest? = null
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    private val requestPermissionsLauncher = registerPermissionResultLauncher()

    private var isDarkMode = false

    private val webViewTarget = mutableStateOf<CallType?>(null)

    private var eventSink: ((CallScreenEvents) -> Unit)? = null

    private var isAudioCall: Boolean? = null
    private var isIncomingCall: Boolean = false
    
    // Store active call information
    private var activeCallId: Long? = null
    private var activeUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isAudioCall = intent?.extras?.get(DefaultElementCallEntryPoint.IS_AUDIO_CALL) as? Boolean
        isIncomingCall = intent?.extras?.get(DefaultElementCallEntryPoint.IS_INCOMING_CALL) as? Boolean ?: false
        Timber.tag("CallActivity").d("Audio call: $isAudioCall, Incoming: $isIncomingCall")

        applicationContext.bindings<CallBindings>().inject(this)

        // Register for broadcast messages
        registerForBroadcastMessages()

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setCallType(intent)
        // If presenter is not created at this point, it means we have no call to display, the Activity is finishing, so return early
        if (!::presenter.isInitialized) {
            return
        }

        if (savedInstanceState == null) {
            updateUiMode(resources.configuration)
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        setContent {
            // State to track the current room name for fake UI
            val currentRoomName = remember { mutableStateOf("Conference") }
            
            ElementThemeApp(
                appPreferencesStore = appPreferencesStore,
                enterpriseService = enterpriseService,
            ) {
                val state = presenter.present()
                eventSink = state.eventSink
                LaunchedEffect(state.isCallActive, state.isInWidgetMode) {
                    // Note when not in WidgetMode, isCallActive will never be true, so consider the call is active
                    if (state.isCallActive || !state.isInWidgetMode) {
                        setCallIsActive()
                    }
                }

                LaunchedEffect(state.urlState) {
                    if (state.urlState is AsyncData.Success) {
                        val url = state.urlState.data
                        val (roomId, displayName, userId) = extractRoomIdAndDisplayName(url)
                        println("RoomName URL ==>> $roomId $displayName $userId")
                        
                        // Update the room name for fake UI - use displayName instead of roomId
                        currentRoomName.value = displayName?.takeIf { it.isNotBlank() } ?: formatRoomName(roomId)
                        
                        activeUserId = userId

                        // AUTH CHECK: Block call join if not authenticated
                        val isAuthenticated = kotlinx.coroutines.runBlocking {
                            val sessionId = authenticationService.getLatestSessionId()
                            sessionId != null
                        }
                        if (!isAuthenticated) {
                            Toast.makeText(this@ElementCallActivity, "You must be logged in to join a call.", Toast.LENGTH_LONG).show()
                            finish()
                            return@LaunchedEffect
                        }

                        // ✅ PARALLEL APPROACH: Show fake UI immediately, launch Jitsi in parallel
                        if (roomId?.isNotBlank() == true) {
                            val audioOnly = isAudioCall == true
                            
                            // Launch Jitsi in background with a small delay to let fake UI render first
                            launch {
                                delay(100) // Just enough time for fake UI to appear
                                joinJitsiMeetingInstant(this@ElementCallActivity, roomId, displayName ?: "Anonymous", audioOnly)
                            }
                        }

                        // Background API calls - non-blocking
                        this@ElementCallActivity.lifecycleScope.launch(Dispatchers.IO) {
                            if (isAudioCall != null) {
                                println("Creating call for primary user==>")
                                try {
                                    val response = callApiService.createCall(
                                        userId = userId,
                                        body = CallRequestBody(room_id = roomId, call_type = if (isAudioCall == true) "audio" else "video")
                                    )
                                    if (response.isSuccessful) {
                                        val data = response.body()
                                        Timber.tag("response ==>>>").d(data.toString())
                                        println("Call Created==>: $data")
                                        activeCallId = data?.call_id
                                        Timber.tag("Active Call ID").d("Stored call_id: $activeCallId")
                                    } else {
                                        println("Create call failed==>: ${response.errorBody()}")
                                    }
                                } catch (e: Exception) {
                                    println("Exception in createCall==>: ${e.localizedMessage}")
                                }
                            } else {
                                println("get call details==>")
                                try {
                                    val response = callApiService.getCallDetails(
                                        userId = userId,
                                        roomId = roomId
                                    )
                                    if (response.isSuccessful) {
                                        val firstCall = response.body()?.calls?.first()
                                        Timber.tag("response ==>>>").d(firstCall.toString())
                                        activeCallId = firstCall?.call_id
                                        Timber.tag("Active Call ID").d("Retrieved call_id: $activeCallId")
                                    } else {
                                        println("Create call failed==>: ${response.errorBody()}")
                                    }
                                } catch (e: Exception) {
                                    println("Exception in createCall==>: ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                }

                // ✅ FAKE UI - COMMENTED OUT for instant performance testing
                // FakeConferenceUI(
                //     isAudioCall = isAudioCall == true,
                //     participantName = currentRoomName.value,
                //     roomName = intent?.getStringExtra("ROOM_NAME") ?: currentRoomName.value
                // )
            }
        }
    }

    fun extractRoomIdAndDisplayName(callUrl: String): Triple<String?, String?, String?> {

        val uri = Uri.parse(callUrl)
        val fragment = uri.fragment // Extract the fragment part after '#?'

        val params = fragment?.split("&")?.associate {
            val (key, value) = it.split("=")
            key to Uri.decode(value) // Decode the URL-encoded value
        }

        Timber.tag("callUrl ==>>>").d(callUrl)
        Timber.tag("params ==>>>").d(params.toString())
        val roomId = params?.get("roomId")
        val displayName = params?.get("displayName")
        val userId = params?.get("?userId")

        return Triple(roomId, displayName, userId)
    }

    /**
     * Formats a room ID to a more user-friendly display name
     */
    private fun formatRoomName(roomId: String?): String {
        return when {
            roomId.isNullOrBlank() -> "Conference"
            roomId.length <= 20 -> roomId // Show short room names as-is
            else -> {
                // For longer room IDs, try to make them more readable
                roomId.replace("-", " ")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word -> 
                        word.lowercase().replaceFirstChar { it.uppercase() }
                    }
                    .take(25) + if (roomId.length > 25) "..." else ""
            }
        }
    }

    // ✅ WhatsApp-style INSTANT Jitsi Meeting Launch - 0ms delay
    private fun joinJitsiMeetingInstant(context: Context, roomName: String, displayName: String, isAudioCall: Boolean) {
        try {
            JitsiConfigurationBuilder.logConfiguration(roomName, isAudioCall, isIncomingCall)
            
            val options = JitsiConfigurationBuilder.createInstantCallOptions(
                roomName = roomName,
                displayName = displayName,
                isAudioCall = isAudioCall,
                isIncomingCall = isIncomingCall
            )

            // ✅ Launch Jitsi with extended fake UI coverage for mobile
            launchJitsiWithExtendedFakeUI(context, options, roomName)
            
        } catch (e: Exception) {
            // Silent error handling to avoid disrupting user experience
            Timber.tag(loggerTag.value).e(e, "Jitsi launch error")
            finish()
        }
    }
    
    // ✅ MOBILE OPTIMIZED: Launch Jitsi with extended fake UI duration for React Native
    private fun launchJitsiWithExtendedFakeUI(context: Context, options: JitsiMeetConferenceOptions, roomName: String) {
        try {
            // ✅ Launch Jitsi in background while fake UI remains visible
            lifecycleScope.launch(Dispatchers.Main) {
                // Launch immediately to reduce first-call latency
                
                // Launch Jitsi Meet Activity with NO animation
                val jitsiIntent = Intent(this@ElementCallActivity, JitsiMeetActivity::class.java).apply {
                    action = "org.jitsi.meet.CONFERENCE"
                    putExtra("JitsiMeetConferenceOptions", options)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(jitsiIntent)
                this@ElementCallActivity.overridePendingTransition(0, 0)
                
                Timber.tag(loggerTag.value).d("✅ Jitsi launched with extended fake UI coverage")
                
                // Delay before finishing to allow smooth transition
                delay(8000) // Show fake UI for 8 seconds on mobile to cover all loading
                finish()
            }
            
        } catch (e: Exception) {
            // Silent error handling to avoid disrupting user experience
            Timber.tag(loggerTag.value).e(e, "Extended Jitsi launch error")
            // Fallback: use minimal configuration
            try {
                val fallbackOptions = JitsiConfigurationBuilder.createMinimalOptions(
                    roomName = roomName
                )
                // Launch Jitsi Meet Activity (fallback) with NO animation
                val jitsiIntent = Intent(this@ElementCallActivity, JitsiMeetActivity::class.java).apply {
                    action = "org.jitsi.meet.CONFERENCE"
                    putExtra("JitsiMeetConferenceOptions", fallbackOptions)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(jitsiIntent)
                this@ElementCallActivity.overridePendingTransition(0, 0)
            } catch (fallbackError: Exception) {
                Timber.tag(loggerTag.value).e(fallbackError, "Fallback Jitsi launch failed")
            }
            finish()
        }
    }

    override fun onBackPressed() {
        // Default back press behavior
        super.onBackPressed()
    }

    private fun setCallIsActive() {
        requestAudioFocus()
        CallForegroundService.start(this)
    }

    @Composable
    private fun ListenToAndroidEvents(pipState: PictureInPictureState) {
        val pipEventSink by rememberUpdatedState(pipState.eventSink)
        DisposableEffect(Unit) {
            val listener = Runnable {
                if (requestPermissionCallback != null) {
                    Timber.tag(loggerTag.value).w("Ignoring onUserLeaveHint event because user is asked to grant permissions")
                } else {
                    pipEventSink(PictureInPictureEvents.EnterPictureInPicture)
                }
            }
            addOnUserLeaveHintListener(listener)
            onDispose {
                removeOnUserLeaveHintListener(listener)
            }
        }
        DisposableEffect(Unit) {
            val onPictureInPictureModeChangedListener = Consumer { _: PictureInPictureModeChangedInfo ->
                pipEventSink(PictureInPictureEvents.OnPictureInPictureModeChanged(isInPictureInPictureMode))
                if (!isInPictureInPictureMode && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    Timber.tag(loggerTag.value).d("Exiting PiP mode: Hangup the call")
                    eventSink?.invoke(CallScreenEvents.Hangup)
                }
            }
            addOnPictureInPictureModeChangedListener(onPictureInPictureModeChangedListener)
            onDispose {
                removeOnPictureInPictureModeChangedListener(onPictureInPictureModeChangedListener)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateUiMode(newConfig)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setCallType(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAudioFocus()
        CallForegroundService.stop(this)
        pictureInPicturePresenter.setPipView(null)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        
        // End the call when activity is destroyed if we have an active call ID
        endActiveCall()
    }
    
    /**
     * End the active call by calling the PUT API with the stored call_id
     */
    private fun endActiveCall() {
        if (activeCallId != null && activeUserId != null) {
            Timber.tag("End Call").d("Ending call with ID: $activeCallId for user: $activeUserId")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = callApiService.endCall(
                        userId = activeUserId!!,
                        requestBody = EndCallRequest(call_id = activeCallId!!)
                    )
                    
                    if (response.isSuccessful) {
                        val callData = response.body()
                        Timber.tag("End Call").d("Call ended successfully: $callData")
                    } else {
                        Timber.tag("End Call").e("Failed to end call: ${response.code()} ${response.message()}")
                    }
                } catch (e: Exception) {
                    Timber.tag("End Call").e(e, "Exception when ending call")
                }
            }
        }
    }

    override fun finish() {
        // Also remove the task from recents
        finishAndRemoveTask()
        // Disable transition animation when finishing and returning from Jitsi
        overridePendingTransition(0, 0)
    }

    override fun close() {
        finish()
    }

    private fun setCallType(intent: Intent?) {
        val callType = intent?.let {
            IntentCompat.getParcelableExtra(intent, DefaultElementCallEntryPoint.EXTRA_CALL_TYPE, CallType::class.java)
                ?: intent.dataString?.let(::parseUrl)?.let(::ExternalUrl)
        }

//        val isAudioCall = if (intent?.hasExtra(DefaultElementCallEntryPoint.IS_AUDIO_CALL) == true) {
//            intent.getBooleanExtra(DefaultElementCallEntryPoint.IS_AUDIO_CALL, false)
//        } else {
//            null // explicitly treat as null when not present
//        }

//        val extras = intent?.extras
//        if (extras != null) {
//            for (key in extras.keySet()) {
//                val value = extras.get(key)
//                Timber.tag("AUDIO_CALL INTENT_EXTRA").d("$key: $value")
//            }
//        } else {
//            Timber.tag("AUDIO_CALL INTENT_EXTRA").d("No extras found in intent")
//        }

        val currentCallType = webViewTarget.value

        if (currentCallType == null) {
            if (callType == null) {
                Timber.tag(loggerTag.value).d("Re-opened the activity but we have no url to load or a cached one, finish the activity")
                finish()
            } else {
                Timber.tag(loggerTag.value).d("Set the call type and create the presenter")
                webViewTarget.value = callType
                presenter = presenterFactory.create(
                    callType,
                    false,
                    this
                )
            }
        } else {
            if (callType == null) {
                Timber.tag(loggerTag.value).d("Coming back from notification, do nothing")
            } else if (callType != currentCallType) {
                Timber.tag(loggerTag.value).d("User starts another call, restart the Activity")
                setIntent(intent)
                recreate()
            } else {
                // Starting the same call again, should not happen, the UI is preventing this. But maybe when using external links.
                Timber.tag(loggerTag.value).d("Starting the same call again, do nothing")
            }
        }
    }

    private fun parseUrl(url: String?): String? = callIntentDataParser.parse(url)

    private fun registerPermissionResultLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val callback = requestPermissionCallback ?: return@registerForActivityResult
            val permissionsToGrant = mutableListOf<String>()
            permissions.forEach { (permission, granted) ->
                if (granted) {
                    val webKitPermission = when (permission) {
                        Manifest.permission.CAMERA -> PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        Manifest.permission.RECORD_AUDIO -> PermissionRequest.RESOURCE_AUDIO_CAPTURE
                        else -> return@forEach
                    }
                    permissionsToGrant.add(webKitPermission)
                }
            }
            callback(permissionsToGrant.toTypedArray())
            requestPermissionCallback = null
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        Timber.tag("requestAudioFocus==>").d("Request Audio")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .build()
            audioManager.requestAudioFocus(request)
            audiofocusRequest = request
        } else {
            val listener = AudioManager.OnAudioFocusChangeListener { }
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            )
            audioFocusChangeListener = listener
        }
    }

    @Suppress("DEPRECATION")
    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audiofocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioFocusChangeListener?.let { audioManager.abandonAudioFocus(it) }
        }
    }

    private fun updateUiMode(configuration: Configuration) {
        val prevDarkMode = isDarkMode
        val currentNightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_YES
        isDarkMode = currentNightMode != 0
        if (prevDarkMode != isDarkMode) {
            if (isDarkMode) {
                window.setBackgroundDrawableResource(android.R.drawable.screen_background_dark)
            } else {
                window.setBackgroundDrawableResource(android.R.drawable.screen_background_light)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun setPipParams() {
        setPictureInPictureParams(getPictureInPictureParams())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun enterPipMode(): Boolean {
        return if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            enterPictureInPictureMode(getPictureInPictureParams())
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPictureInPictureParams(): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            // Portrait for calls seems more appropriate
            .setAspectRatio(Rational(3, 5))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }
            .build()
    }

    override fun hangUp() {
        eventSink?.invoke(CallScreenEvents.Hangup)
        val hangupBroadcastIntent = org.jitsi.meet.sdk.BroadcastIntentHelper.buildHangUpIntent()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(hangupBroadcastIntent)
    }

    private fun registerForBroadcastMessages() {
        val intentFilter = android.content.IntentFilter().apply {
            addAction(org.jitsi.meet.sdk.BroadcastEvent.Type.CONFERENCE_JOINED.action)
            addAction(org.jitsi.meet.sdk.BroadcastEvent.Type.CONFERENCE_TERMINATED.action)
            addAction(org.jitsi.meet.sdk.BroadcastEvent.Type.PARTICIPANT_JOINED.action)
            addAction(org.jitsi.meet.sdk.BroadcastEvent.Type.READY_TO_CLOSE.action)
        }

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun onBroadcastReceived(intent: Intent) {
        intent?.let {
            val event = org.jitsi.meet.sdk.BroadcastEvent(intent)
            when (event.type) {
                org.jitsi.meet.sdk.BroadcastEvent.Type.CONFERENCE_JOINED -> {
                    Timber.tag(loggerTag.value).d("Conference Joined: ${event.data}")
                }
                org.jitsi.meet.sdk.BroadcastEvent.Type.CONFERENCE_TERMINATED -> {
                    Timber.tag(loggerTag.value).d("Conference Terminated: ${event.data}")
                    // End the call when conference is terminated
                    endActiveCall()
                    finish()
                }
                org.jitsi.meet.sdk.BroadcastEvent.Type.PARTICIPANT_JOINED -> {
                    Timber.tag(loggerTag.value).d("Participant joined: ${event.data["name"]}")
                }
                org.jitsi.meet.sdk.BroadcastEvent.Type.READY_TO_CLOSE -> {
                    Timber.tag(loggerTag.value).d("Ready to close: ${event.data}")
                    // End the call when app is ready to close
                    endActiveCall()
                }
                else -> {}
            }
        }
    }
}

internal fun mapWebkitPermissions(permissions: Array<String>): List<String> {
    Timber.tag("mapWebkitPermissions==>>>").d(permissions.toString())
    return permissions.mapNotNull { permission ->
        when (permission) {
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
            else -> null
        }
    }
}
