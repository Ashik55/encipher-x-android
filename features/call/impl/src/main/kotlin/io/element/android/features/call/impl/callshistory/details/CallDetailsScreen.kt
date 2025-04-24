/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory.details

import androidx.compose.runtime.Composable
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.call.impl.callshistory.ActiveSessionIdHolder
import io.element.android.features.call.impl.callshistory.Call
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

/**
 * Input data for CallDetailsScreen.
 */
data class CallDetailsInput(
    val call: Call
) : NodeInputs

@ContributesNode(SessionScope::class)
class CallDetailsScreen @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val elementCallEntryPoint: ElementCallEntryPoint,
    private val activeSessionIdHolder: ActiveSessionIdHolder
) : Node(buildContext, plugins = plugins) {

    @AssistedFactory
    interface Factory {
        fun create(
            buildContext: BuildContext,
            plugins: List<Plugin>
        ): CallDetailsScreen
    }
    
    interface Callback : Plugin {
        fun onBackPressed()
        fun onRoomDetailsClick(roomId: RoomId)
    }
    
    private val call: Call? = plugins.filterIsInstance<CallDetailsInput>().firstOrNull()?.call
    
    private fun onBackClick() {
        plugins<Callback>().forEach { it.onBackPressed() }
    }
    
    private fun onMessageClick() {
        call?.room_id?.let { roomId ->
            plugins<Callback>().forEach { callback -> 
                val matrixRoomId = RoomId(roomId)
                callback.onRoomDetailsClick(matrixRoomId)
            }
        }
    }
    
    private fun onAudioCallClick() {
        call?.room_id?.let { roomId ->
            val sessionId = getActiveSessionId()
            if (sessionId != null) {
                val callType = io.element.android.features.call.api.CallType.RoomCall(
                    sessionId = sessionId,
                    roomId = RoomId(roomId)
                )
                elementCallEntryPoint.startCall(callType, true)
            }
        }
    }
    
    private fun onVideoCallClick() {
        call?.room_id?.let { roomId ->
            val sessionId = getActiveSessionId()
            if (sessionId != null) {
                val callType = io.element.android.features.call.api.CallType.RoomCall(
                    sessionId = sessionId,
                    roomId = RoomId(roomId)
                )
                elementCallEntryPoint.startCall(callType, false)
            }
        }
    }
    
    private fun getActiveSessionId(): SessionId? {
        return activeSessionIdHolder.getActiveSessionIdSync()
    }

    @Composable
    override fun View(modifier: androidx.compose.ui.Modifier) {
        CallDetailsView(
            state = Any(), // In a real implementation, you would have a proper state object
            onBackClick = ::onBackClick,
            onMessageClick = ::onMessageClick,
            onAudioCallClick = ::onAudioCallClick,
            onVideoCallClick = ::onVideoCallClick,
            modifier = modifier
        )
    }
}