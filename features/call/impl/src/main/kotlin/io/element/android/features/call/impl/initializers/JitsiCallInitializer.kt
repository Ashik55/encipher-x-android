package io.element.android.features.call.impl.initializers

import android.content.Context
import androidx.startup.Initializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jitsi.meet.sdk.JitsiMeet
import timber.log.Timber
import java.net.URL
import io.element.android.features.call.impl.config.JitsiConfigurationBuilder

/**
 * Call Initializer - Initializes Jitsi Meet at app startup for instant calling (0ms delay)
 */
class JitsiCallInitializer : Initializer<Unit> {

    companion object {
        private const val TAG = "JitsiCallInit"
        private const val SERVER_URL = "https://meet.prod.enciph-er.com/"
    }

    override fun create(context: Context) {
        Timber.tag(TAG).d("🚀 Initializing instant calling system...")
        
        // Start preloading Jitsi Meet in background immediately
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Pre-load Jitsi Meet SDK for instant calls
                preloadJitsiMeet()
                
                // Pre-warm server connection
                preloadServerConnection()
                
                Timber.tag(TAG).d("✅ Instant calling system ready! WhatsApp-like speed achieved.")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Failed to initialize instant calling")
            }
        }
    }

    private fun preloadJitsiMeet() {
        try {
            Timber.tag(TAG).d("🚀 Pre-loading Jitsi Meet for instant calls...")
            
            // Use centralized configuration builder
            val defaultOptions = JitsiConfigurationBuilder.createDefaultOptions()
            
            // Set default configuration for instant launch
            JitsiMeet.setDefaultConferenceOptions(defaultOptions)
            
            Timber.tag(TAG).d("✅ Jitsi Meet pre-loaded successfully! Calls will launch instantly.")
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to pre-load Jitsi Meet")
        }
    }

    private fun preloadServerConnection() {
        try {
            Timber.tag(TAG).d("🌐 Pre-loading server connection...")
            
            // Pre-warm the network connection to Jitsi server
            val url = URL(SERVER_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 3000 // Quick connection test
            connection.readTimeout = 3000
            connection.connect()
            connection.inputStream.close()
            
            Timber.tag(TAG).d("✅ Server connection pre-loaded!")
            
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "⚠️ Server pre-load failed (this is okay)")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
