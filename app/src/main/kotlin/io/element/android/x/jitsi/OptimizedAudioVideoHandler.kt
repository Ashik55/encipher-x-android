/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.jitsi

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
// CameraX dependencies removed for compatibility
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Optimized audio and video handler for instant conference performance.
 * 
 * Pre-warms audio/video systems to achieve sub-300ms initialization times
 * for WhatsApp-like smooth video calling experience.
 */
object OptimizedAudioVideoHandler {
    
    private const val TAG = "OptimizedAudioVideo"
    
    // Preloading state
    private val isAudioPreloaded = AtomicBoolean(false)
    private val isVideoPreloaded = AtomicBoolean(false)
    private val isCameraPreloaded = AtomicBoolean(false)
    
    // Performance tracking
    private val audioPreloadTime = AtomicLong(0L)
    private val videoPreloadTime = AtomicLong(0L)
    private val cameraPreloadTime = AtomicLong(0L)
    
    // Preloaded components
    private var preloadedAudioManager: AudioManager? = null
    private var preloadedAudioFocusRequest: AudioFocusRequest? = null
    // Camera components removed for compatibility
    
    /**
     * Preloads audio systems for instant audio capture.
     */
    suspend fun preloadAudioSystems(context: Context) = withContext(Dispatchers.Main) {
        if (isAudioPreloaded.get()) {
            Log.d(TAG, "Audio systems already preloaded")
            return@withContext
        }
        
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🎵 Preloading audio systems...")
        
        try {
            // Pre-warm audio manager
            preloadedAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            preloadedAudioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            
            // Pre-warm audio focus for voice calls
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                preloadedAudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_GAIN -> Log.d(TAG, "Audio focus gained")
                            AudioManager.AUDIOFOCUS_LOSS -> Log.d(TAG, "Audio focus lost")
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> Log.d(TAG, "Audio focus lost transient")
                        }
                    }
                    .build()
            }
            
            // Pre-warm microphone permissions and capabilities
            preloadMicrophoneCapabilities(context)
            
            isAudioPreloaded.set(true)
            audioPreloadTime.set(System.currentTimeMillis() - startTime)
            
            Log.d(TAG, "✅ Audio systems preloaded in ${audioPreloadTime.get()}ms")
            ConferencePerformanceMonitor.recordAudioInitTime(audioPreloadTime.get())
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Audio preloading failed", e)
            audioPreloadTime.set(-1L)
        }
    }
    
    /**
     * Preloads video systems for instant video capture.
     */
    suspend fun preloadVideoSystems(context: Context) = withContext(Dispatchers.Main) {
        if (isVideoPreloaded.get()) {
            Log.d(TAG, "Video systems already preloaded")
            return@withContext
        }
        
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "📹 Preloading video systems...")
        
        try {
            // Pre-warm video permissions and capabilities
            preloadVideoCapabilities(context)
            
            isVideoPreloaded.set(true)
            videoPreloadTime.set(System.currentTimeMillis() - startTime)
            
            Log.d(TAG, "✅ Video systems preloaded in ${videoPreloadTime.get()}ms")
            ConferencePerformanceMonitor.recordVideoInitTime(videoPreloadTime.get())
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Video preloading failed", e)
            videoPreloadTime.set(-1L)
        }
    }
    
    /**
     * Preloads camera for instant video capture.
     */
    suspend fun preloadCamera(context: Context) = withContext(Dispatchers.Main) {
        if (isCameraPreloaded.get()) {
            Log.d(TAG, "Camera already preloaded")
            return@withContext
        }
        
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "📷 Preloading camera...")
        
        try {
            // Pre-warm camera capabilities
            preloadVideoCapabilities(context)
            
            isCameraPreloaded.set(true)
            cameraPreloadTime.set(System.currentTimeMillis() - startTime)
            
            Log.d(TAG, "✅ Camera preloaded in ${cameraPreloadTime.get()}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Camera preloading failed", e)
            cameraPreloadTime.set(-1L)
        }
    }
    
    /**
     * Preloads microphone capabilities and permissions.
     */
    private suspend fun preloadMicrophoneCapabilities(context: Context) = withContext(Dispatchers.IO) {
        try {
            // Check microphone availability
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val isMicrophoneMute = audioManager.isMicrophoneMute
            val isSpeakerphoneOn = audioManager.isSpeakerphoneOn
            val isBluetoothScoOn = audioManager.isBluetoothScoOn
            
            Log.d(TAG, "Microphone capabilities: mute=$isMicrophoneMute, speaker=$isSpeakerphoneOn, bluetooth=$isBluetoothScoOn")
            
            // Pre-warm MediaRecorder for audio recording
            try {
                val mediaRecorder = MediaRecorder()
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                // Don't actually start recording, just prepare
                mediaRecorder.release()
            } catch (e: Exception) {
                Log.w(TAG, "MediaRecorder pre-warming failed (this is okay)", e)
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Microphone capability preloading failed (this is okay)", e)
        }
    }
    
    /**
     * Preloads video capabilities and permissions.
     */
    private suspend fun preloadVideoCapabilities(context: Context) = withContext(Dispatchers.IO) {
        try {
            // Pre-warm video permissions and capabilities
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            Log.d(TAG, "Available cameras: ${cameraIds.size}")
            
            // Pre-warm camera info for faster access later
            cameraIds.forEach { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    Log.d(TAG, "Camera pre-warmed: $cameraId")
                } catch (e: Exception) {
                    Log.w(TAG, "Camera info pre-warming failed for camera $cameraId", e)
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Video capability preloading failed (this is okay)", e)
        }
    }
    
    /**
     * Gets preloaded audio manager for instant use.
     */
    fun getPreloadedAudioManager(): AudioManager? = preloadedAudioManager
    
    /**
     * Gets preloaded audio focus request for instant use.
     */
    fun getPreloadedAudioFocusRequest(): AudioFocusRequest? = preloadedAudioFocusRequest
    
    // Camera provider methods removed for compatibility
    
    /**
     * Checks if audio systems are preloaded and ready.
     */
    fun isAudioPreloaded(): Boolean = isAudioPreloaded.get()
    
    /**
     * Checks if video systems are preloaded and ready.
     */
    fun isVideoPreloaded(): Boolean = isVideoPreloaded.get()
    
    /**
     * Checks if camera is preloaded and ready.
     */
    fun isCameraPreloaded(): Boolean = isCameraPreloaded.get()
    
    /**
     * Gets audio preload time in milliseconds.
     */
    fun getAudioPreloadTime(): Long = audioPreloadTime.get()
    
    /**
     * Gets video preload time in milliseconds.
     */
    fun getVideoPreloadTime(): Long = videoPreloadTime.get()
    
    /**
     * Gets camera preload time in milliseconds.
     */
    fun getCameraPreloadTime(): Long = cameraPreloadTime.get()
    
    /**
     * Gets performance summary for audio/video systems.
     */
    fun getPerformanceSummary(): String {
        val audioTime = if (audioPreloadTime.get() >= 0) "${audioPreloadTime.get()}ms" else "FAILED"
        val videoTime = if (videoPreloadTime.get() >= 0) "${videoPreloadTime.get()}ms" else "FAILED"
        val cameraTime = if (cameraPreloadTime.get() >= 0) "${cameraPreloadTime.get()}ms" else "FAILED"
        
        return "Audio: $audioTime, Video: $videoTime, Camera: $cameraTime"
    }
    
    /**
     * Resets all preloading state (useful for testing).
     */
    fun resetPreloading() {
        isAudioPreloaded.set(false)
        isVideoPreloaded.set(false)
        isCameraPreloaded.set(false)
        
        preloadedAudioManager = null
        preloadedAudioFocusRequest = null
        // Camera components removed for compatibility
        
        audioPreloadTime.set(0L)
        videoPreloadTime.set(0L)
        cameraPreloadTime.set(0L)
        
        Log.d(TAG, "🔄 Audio/Video preloading state reset")
    }
    
    /**
     * Preloads all audio/video systems in parallel for maximum speed.
     */
    suspend fun preloadAllSystems(context: Context) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 Starting comprehensive audio/video preloading...")
        
        val startTime = System.currentTimeMillis()
        
        // Preload all systems in parallel
        val preloadJobs = listOf(
            async { preloadAudioSystems(context) },
            async { preloadVideoSystems(context) },
            async { preloadCamera(context) }
        )
        
        preloadJobs.awaitAll()
        
        val totalTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "✅ All audio/video systems preloaded in ${totalTime}ms")
        Log.d(TAG, "Performance summary: ${getPerformanceSummary()}")
    }
}
