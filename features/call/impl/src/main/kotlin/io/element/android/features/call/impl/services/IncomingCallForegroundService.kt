/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import io.element.android.features.call.api.CallType
import io.element.android.features.call.impl.R
import io.element.android.features.call.impl.di.CallBindings
import io.element.android.features.call.impl.notifications.CallNotificationData
import io.element.android.features.call.impl.receivers.DeclineCallBroadcastReceiver
import io.element.android.features.call.impl.ui.IncomingCallActivity
import io.element.android.features.call.impl.utils.ActiveCallManager
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.ui.media.ImageLoaderHolder
import io.element.android.libraries.push.api.notifications.ForegroundServiceType
import io.element.android.libraries.push.api.notifications.NotificationBitmapLoader
import io.element.android.libraries.push.api.notifications.NotificationIdProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * A foreground service that shows a persistent notification for incoming calls.
 * This service ensures the notification remains visible until the call is answered or declined.
 */
class IncomingCallForegroundService : Service() {
    
    companion object {
        private const val TAG = "IncomingCallService"
        private const val EXTRA_NOTIFICATION_DATA = "EXTRA_NOTIFICATION_DATA"
        private const val EXTRA_ACTION = "EXTRA_ACTION"
        private const val ACTION_START_INCOMING_CALL = "START_INCOMING_CALL"
        private const val ACTION_STOP_INCOMING_CALL = "STOP_INCOMING_CALL"
        private const val CHANNEL_ID = "INCOMING_CALL_FOREGROUND_SERVICE"
        private const val CHANNEL_NAME = "Incoming Call Service"
        
        // Track if we've already created a ringing call recently to prevent double ringing
        private var lastRingTime: Long = 0
        private const val RING_DEBOUNCE_TIME_MS = 2000 // 2 seconds
        
        fun startService(context: Context, notificationData: CallNotificationData) {
            try {
                val intent = Intent(context, IncomingCallForegroundService::class.java).apply {
                    putExtra(EXTRA_ACTION, ACTION_START_INCOMING_CALL)
                    putExtra(EXTRA_NOTIFICATION_DATA, notificationData)
                }
                ContextCompat.startForegroundService(context, intent)
                Timber.tag(TAG).d("Started incoming call foreground service")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to start incoming call foreground service")
            }
        }
        
        fun stopService(context: Context) {
            try {
                val intent = Intent(context, IncomingCallForegroundService::class.java).apply {
                    putExtra(EXTRA_ACTION, ACTION_STOP_INCOMING_CALL)
                }
                context.startService(intent)
                Timber.tag(TAG).d("Stopping incoming call foreground service")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to stop incoming call foreground service")
            }
        }
    }
    
    @Inject
    lateinit var activeCallManager: ActiveCallManager
    
    @Inject
    lateinit var matrixClientProvider: MatrixClientProvider
    
    @Inject
    lateinit var imageLoaderHolder: ImageLoaderHolder
    
    @Inject
    lateinit var notificationBitmapLoader: NotificationBitmapLoader
    
    private lateinit var notificationManager: NotificationManagerCompat
    private var currentNotificationData: CallNotificationData? = null
    private var ringtone: Ringtone? = null
    private var audioManager: AudioManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        applicationContext.bindings<CallBindings>().inject(this)
        
        notificationManager = NotificationManagerCompat.from(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Create notification channel
        createNotificationChannel()
        
        Timber.tag(TAG).d("Incoming call foreground service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(EXTRA_ACTION)
        
        when (action) {
            ACTION_START_INCOMING_CALL -> {
                val notificationData = intent?.let { 
                    IntentCompat.getParcelableExtra(it, EXTRA_NOTIFICATION_DATA, CallNotificationData::class.java)
                }
                if (notificationData != null) {
                    startIncomingCallService(notificationData)
                } else {
                    Timber.tag(TAG).e("No notification data provided, stopping service")
                    stopSelf()
                }
            }
            ACTION_STOP_INCOMING_CALL -> {
                stopIncomingCallService()
            }
            else -> {
                Timber.tag(TAG).w("Unknown action: $action")
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        currentNotificationData = null
        serviceScope.cancel()
        Timber.tag(TAG).d("Incoming call foreground service destroyed")
    }
    
    private fun createNotificationChannel() {
        // Get ringtone URI for the channel
        val ringtoneUri = runCatching { 
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE) 
        }.getOrNull()
        
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_MAX
        )
            .setName(CHANNEL_NAME)
            .setDescription("Persistent notifications for incoming calls")
            .setVibrationEnabled(true)
            .setLightsEnabled(true)
            .setLightColor(0xFF00FF00.toInt()) // Green light
            .apply {
                if (ringtoneUri != null) {
                    setSound(
                        ringtoneUri,
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_RING)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                    )
                }
            }
            .build()
        
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun startIncomingCallService(notificationData: CallNotificationData) {
        currentNotificationData = notificationData
        
        // Don't start manual ringtone - let the notification channel handle sound
        // startRingtone()
        
        val notificationId = NotificationIdProvider.getForegroundServiceNotificationId(
            ForegroundServiceType.INCOMING_CALL
        )
        
        // Create a basic notification immediately to start foreground service
        val basicNotification = createBasicNotification(notificationData)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        
        try {
            ServiceCompat.startForeground(this, notificationId, basicNotification, serviceType)
            Timber.tag(TAG).d("Started foreground service with basic notification")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start foreground service")
            stopRingtone()
            stopSelf()
            return
        }
        
        // Then load avatar and update notification asynchronously
        serviceScope.launch {
            try {
                val enhancedNotification = createPersistentNotification(notificationData)
                notificationManager.notify(notificationId, enhancedNotification)
                Timber.tag(TAG).d("Updated notification with avatar")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to update notification with avatar, keeping basic notification")
            }
        }
    }
    
    private fun stopIncomingCallService() {
        Timber.tag(TAG).d("Stopping incoming call foreground service")
        stopRingtone()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createBasicNotification(notificationData: CallNotificationData): Notification {
        val roomName = notificationData.roomName ?: "Unknown Room"
        val senderName = notificationData.senderName ?: notificationData.senderId.value
        
        // Create caller person object without avatar (for immediate display)
        // For group calls, use room name; for direct calls, use sender name
        val displayName = if (roomName != "Unknown Room" && roomName != senderName) {
            roomName // Group call - show room name
        } else {
            senderName // Direct call - show sender name
        }
        
        val callerPerson = Person.Builder()
            .setName(displayName)
            .setImportant(true)
            .build()
        
        return createNotificationBuilder(notificationData, callerPerson, roomName, senderName).build()
    }
    
    private suspend fun createPersistentNotification(notificationData: CallNotificationData): Notification {
        val roomName = notificationData.roomName ?: "Unknown Room"
        val senderName = notificationData.senderName ?: notificationData.senderId.value
        
        // Load caller avatar
        val matrixClient = matrixClientProvider.getOrRestore(notificationData.sessionId).getOrNull()
        val largeIcon = if (matrixClient != null && notificationData.avatarUrl != null) {
            try {
                val imageLoader = imageLoaderHolder.get(matrixClient)
                notificationBitmapLoader.getUserIcon(notificationData.avatarUrl, imageLoader)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to load avatar")
                null
            }
        } else {
            null
        }
        
        // Create caller person object with avatar
        // For group calls, use room name; for direct calls, use sender name
        val displayName = if (roomName != "Unknown Room" && roomName != senderName) {
            roomName // Group call - show room name
        } else {
            senderName // Direct call - show sender name
        }
        
        val callerPerson = Person.Builder()
            .setName(displayName)
            .setImportant(true)
            .apply {
                if (largeIcon != null) {
                    setIcon(largeIcon)
                }
            }
            .build()
        
        return createNotificationBuilder(notificationData, callerPerson, roomName, senderName).build()
    }
    
    private fun createNotificationBuilder(
        notificationData: CallNotificationData, 
        callerPerson: Person, 
        roomName: String, 
        senderName: String
    ): NotificationCompat.Builder {
        // Create intent to open the incoming call activity
        val callIntent = PendingIntentCompat.getActivity(
            this,
            0,
            Intent(this, IncomingCallActivity::class.java).apply {
                putExtra(IncomingCallActivity.EXTRA_NOTIFICATION_DATA, notificationData)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
        
        // Create decline intent
        val declineIntent = PendingIntentCompat.getBroadcast(
            this,
            1,
            Intent(this, DeclineCallBroadcastReceiver::class.java).apply {
                putExtra(DeclineCallBroadcastReceiver.EXTRA_NOTIFICATION_DATA, notificationData)
            },
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
        
        // Create answer intent 
        val answerIntent = PendingIntentCompat.getActivity(
            this,
            2,
            Intent(this, IncomingCallActivity::class.java).apply {
                putExtra(IncomingCallActivity.EXTRA_NOTIFICATION_DATA, notificationData)
                putExtra("AUTO_ANSWER", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )
        
        // Get ringtone URI
        val ringtoneUri = runCatching { 
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE) 
        }.getOrNull()
        
        // Create the main content text
        val contentText = if (roomName != "Unknown Room" && roomName != senderName) {
            "Calling from $roomName"
        } else {
            "Incoming call"
        }
        
        // Create big text style content
        val bigText = "📞 Incoming call from $senderName" + 
            if (roomName != "Unknown Room" && roomName != senderName) " in $roomName" else ""
        
        // For notification title, show who's calling for both group and direct calls
        val notificationTitle = if (roomName != "Unknown Room" && roomName != senderName) {
            "📞 $senderName in $roomName" // Group call - show "Sender in Room"
        } else {
            "📞 $senderName" // Direct call - show just sender
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(CommonDrawables.ic_notification_small)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setSubText("Tap to answer")
            .setContentIntent(callIntent!!)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true) // Makes the notification persistent
            .setAutoCancel(false) // Prevent dismissing by swiping
            .setShowWhen(true)
            .setWhen(notificationData.timestamp)
            .setUsesChronometer(false)
            .setColor(0xFF4CAF50.toInt()) // Green color for call theme
            .addPerson(callerPerson)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson,
                    declineIntent!!,
                    answerIntent!!
                ).setIsVideo(false)
                    .setAnswerButtonColorHint(0xFF4CAF50.toInt())
                    .setDeclineButtonColorHint(0xFFFF4444.toInt())
            )
            .apply {
                // Don't set sound on individual notification - let the channel handle it
                // Setting sound here AND on channel can cause double sound
                // if (ringtoneUri != null) {
                //     setSound(ringtoneUri, AudioManager.STREAM_RING)
                // }
                
                // Custom vibration pattern (short pause, long vibration, short pause, long vibration)
                setVibrate(longArrayOf(0, 1000, 500, 1000))
                
                // LED notification
                setLights(0xFF00FF00.toInt(), 300, 1000)
                
                // Set as full screen intent for better visibility
                setFullScreenIntent(callIntent!!, true)
                
                // Make it high priority and heads-up
                priority = NotificationCompat.PRIORITY_MAX
            }
    }
    
    /**
     * Starts playing the ringtone for the incoming call.
     * Uses debouncing to prevent multiple ringtones from playing simultaneously.
     */
    private fun startRingtone() {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Check if we recently started a ringtone to prevent double ringing
            if (currentTime - lastRingTime < RING_DEBOUNCE_TIME_MS) {
                Timber.tag(TAG).d("Skipping ringtone - too soon after last ring (${currentTime - lastRingTime}ms)")
                return
            }
            
            stopRingtone() // Stop any existing ringtone first
            
            val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            if (ringtoneUri != null) {
                ringtone = RingtoneManager.getRingtone(this, ringtoneUri).apply {
                    // Set audio attributes for call ringtone
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_RING)
                            .build()
                    }
                    
                    // Start playing the ringtone
                    if (!isPlaying) {
                        play()
                        lastRingTime = currentTime
                        Timber.tag(TAG).d("Started ringtone playback")
                    }
                }
            } else {
                Timber.tag(TAG).w("No ringtone URI available")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start ringtone")
        }
    }
    
    /**
     * Stops playing the ringtone.
     */
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
}
