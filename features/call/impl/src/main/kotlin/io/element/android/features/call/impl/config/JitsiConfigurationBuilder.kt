/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.config

import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import timber.log.Timber
import java.net.URL

/**
 * Centralized Jitsi Meet configuration builder to eliminate code duplication
 * across ElementCallActivity and JitsiCallInitializer.
 */
object JitsiConfigurationBuilder {
    
    private const val TAG = "JitsiConfig"
    private const val SERVER_URL = "https://meet.prod.enciph-er.com/"
    
    /**
     * Creates optimized Jitsi configuration for instant call joining.
     * Removes all loading screens and connection messages for WhatsApp-like experience.
     */
    fun createInstantCallOptions(
        roomName: String,
        displayName: String,
        isAudioCall: Boolean,
        isIncomingCall: Boolean = false
    ): JitsiMeetConferenceOptions {
        
        val userInfo = JitsiMeetUserInfo().apply {
            this.displayName = displayName
        }
        
        return JitsiMeetConferenceOptions.Builder()
            .setServerURL(URL(SERVER_URL))
            .setRoom(roomName)
            .setAudioOnly(isAudioCall)
            .setUserInfo(userInfo)
            .apply {
                // Apply common instant UI configuration
                applyInstantUIConfig()
                // Apply call direction specific config
                applyCallDirectionConfig(isIncomingCall)
                // Apply audio/video specific config
                applyMediaConfig(isAudioCall)
            }
            .build()
    }
    
    /**
     * Creates default configuration for app initialization and preloading.
     */
    fun createDefaultOptions(): JitsiMeetConferenceOptions {
        return JitsiMeetConferenceOptions.Builder()
            .setServerURL(URL(SERVER_URL))
            .setRoom("preload_dummy_room")
            .apply {
                // Apply common instant UI configuration only
                applyInstantUIConfig()
                setConfigOverride("requireDisplayName", false)
                setConfigOverride("enableWelcomePage", false)
                setConfigOverride("enablePreJoinPage", false)
            }
            .build()
    }
    
    /**
     * Applies common instant UI configuration to remove all loading screens.
     */
    private fun JitsiMeetConferenceOptions.Builder.applyInstantUIConfig() {
        // Remove welcome and prejoin pages
        setFeatureFlag("welcomepage.enabled", false)
        setFeatureFlag("prejoinpage.enabled", false)
        
        // Remove UI elements for cleaner interface
        setFeatureFlag("filmstrip.enabled", false)
        setFeatureFlag("reactions.enabled", false)
        setFeatureFlag("chat.enabled", false)
        setFeatureFlag("lobby.enabled", false)
        setFeatureFlag("security.enabled", false)
        setFeatureFlag("invite.enabled", false)
        setFeatureFlag("notifications.enabled", false)
        
        // Remove statistics and recording features
        setFeatureFlag("callstats.enabled", false)
        setFeatureFlag("recording.enabled", false)
        setFeatureFlag("streaming.enabled", false)
        
        // Disable connection indicators and status messages
        setConfigOverride("connectionIndicators.enabled", false)
        setConfigOverride("disableInviteFunctions", true)
        setConfigOverride("disableModeratorIndicator", true)
        setConfigOverride("disableReactions", true)
        setConfigOverride("disableShowMore", true)
    }
    
    /**
     * Applies call direction specific configuration.
     */
    private fun JitsiMeetConferenceOptions.Builder.applyCallDirectionConfig(isIncomingCall: Boolean) {
        if (isIncomingCall) {
            setConfigOverride("callDirection", "incoming")
            setConfigOverride("isOutgoingCall", false)
        } else {
            setConfigOverride("callDirection", "outgoing")
            setConfigOverride("isOutgoingCall", true)
        }
    }
    
    /**
     * Applies media-specific configuration for audio/video calls.
     */
    private fun JitsiMeetConferenceOptions.Builder.applyMediaConfig(isAudioCall: Boolean) {
        setConfigOverride("startWithAudioMuted", false)
        setConfigOverride("startWithVideoMuted", isAudioCall)
        
        // Mobile optimizations
        setConfigOverride("resolution", 360)
        setConfigOverride("constraints.video.height", 360)
        setConfigOverride("disableDeepLinking", true)
        setConfigOverride("p2p.enabled", false) // Force server routing for faster connect
    }
    
    /**
     * Creates conference options with minimal configuration for testing.
     */
    fun createMinimalOptions(roomName: String): JitsiMeetConferenceOptions {
        return JitsiMeetConferenceOptions.Builder()
            .setServerURL(URL(SERVER_URL))
            .setRoom(roomName)
            .setFeatureFlag("welcomepage.enabled", false)
            .setFeatureFlag("prejoinpage.enabled", false)
            .build()
    }
    
    /**
     * Logs the configuration being used for debugging purposes.
     */
    fun logConfiguration(roomName: String, isAudioCall: Boolean, isIncomingCall: Boolean) {
        Timber.tag(TAG).d(
            "Creating Jitsi config - Room: $roomName, Audio: $isAudioCall, Incoming: $isIncomingCall"
        )
    }
}
