/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.ui

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import io.element.android.features.call.api.CallType
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.call.impl.di.CallBindings
import io.element.android.features.call.impl.notifications.CallNotificationData
import io.element.android.features.call.impl.utils.ActiveCallManager
import io.element.android.features.call.impl.utils.CallState
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.designsystem.theme.ElementThemeApp
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity that's displayed as a full screen intent when an incoming call is received.
 */
class IncomingCallActivity : AppCompatActivity() {
    companion object {
        /**
         * Extra key for the notification data.
         */
        const val EXTRA_NOTIFICATION_DATA = "EXTRA_NOTIFICATION_DATA"
        private const val TAG = "IncomingCallActivity"
    }

    @Inject
    lateinit var elementCallEntryPoint: ElementCallEntryPoint

    @Inject
    lateinit var activeCallManager: ActiveCallManager

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    @Inject
    lateinit var enterpriseService: EnterpriseService

    private var ringtone: Ringtone? = null
    private var audioManager: AudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationContext.bindings<CallBindings>().inject(this)

        // Initialize audio manager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Set flags so it can be displayed in the lock screen
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val notificationData = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_NOTIFICATION_DATA, CallNotificationData::class.java) }
        if (notificationData != null) {
            // Start ringtone when activity is created
            startRingtone()
            
            setContent {
                ElementThemeApp(
                    appPreferencesStore = appPreferencesStore,
                    enterpriseService = enterpriseService,
                ) {
                    IncomingCallScreen(
                        notificationData = notificationData,
                        onAnswer = ::onAnswer,
                        onCancel = ::onCancel,
                    )
                }
            }
        } else {
            // No data, finish the activity
            finish()
            return
        }

        activeCallManager.activeCall
            .filter { it?.callState !is CallState.Ringing }
            .onEach { 
                stopRingtone()
                finish() 
            }
            .launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }

    override fun onPause() {
        super.onPause()
        // Don't stop ringtone on pause - keep it playing even if user navigates away
        Timber.tag(TAG).d("Activity paused, but keeping ringtone active")
    }

    override fun onResume() {
        super.onResume()
        // Ensure ringtone is still playing when returning to activity
        if (ringtone?.isPlaying != true) {
            Timber.tag(TAG).d("Resuming activity - restarting ringtone if needed")
            startRingtone()
        }
    }

    private fun startRingtone() {
        try {
            stopRingtone() // Stop any existing ringtone first
            
            val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            if (ringtoneUri != null) {
                ringtone = RingtoneManager.getRingtone(this, ringtoneUri).apply {
                    // Set audio attributes for call ringtone
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    
                    // Start playing the ringtone
                    play()
                    Timber.tag(TAG).d("Started ringtone playback")
                }
            } else {
                Timber.tag(TAG).w("No ringtone URI available")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start ringtone")
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.let { ringtone ->
                if (ringtone.isPlaying) {
                    ringtone.stop()
                    Timber.tag(TAG).d("Stopped ringtone playback")
                }
            }
            ringtone = null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to stop ringtone")
        }
    }

    private fun onAnswer(notificationData: CallNotificationData) {
        stopRingtone()
        elementCallEntryPoint.startCall(CallType.RoomCall(notificationData.sessionId, notificationData.roomId))
    }

    private fun onCancel() {
        stopRingtone()
        val activeCall = activeCallManager.activeCall.value ?: return
        activeCallManager.hungUpCall(callType = activeCall.callType)
    }
}
