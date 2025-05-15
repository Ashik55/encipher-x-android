/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.call.impl.callshistory.details.CallDetailsInput
import io.element.android.features.call.impl.callshistory.details.CallDetailsScreen
import io.element.android.libraries.designsystem.components.navbar.BottomNavRoute
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId

@ContributesNode(SessionScope::class)
class CallsHistoryNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: CallsHistoryPresenter,
    private val elementCallEntryPoint: ElementCallEntryPoint,
    private val callDetailsScreenFactory: CallDetailsScreen.Factory,
) : Node(buildContext, plugins = plugins) {
    
    interface Callback : Plugin {
        fun onRoomDetailsClick(roomId: RoomId)
        fun onHomeClick()
        fun onSettingsClick()
        fun navigateToCallDetails(call: Call)
    }
    
    private fun onRoomDetailsClick(roomId: String) {
        val callbacks = plugins<Callback>()
        callbacks.forEach { callback -> 
            val matrixRoomId = RoomId(roomId)
            callback.onRoomDetailsClick(matrixRoomId) 
        }
    }
    
    private fun onBottomNavigationRouteSelected(route: BottomNavRoute) {
        val callbacks = plugins<Callback>()
        when (route) {
            BottomNavRoute.Chats -> callbacks.forEach { it.onHomeClick() }
            BottomNavRoute.Settings -> callbacks.forEach { it.onSettingsClick() }
            BottomNavRoute.Calls -> { /* Already on calls screen, do nothing */ }
        }
    }
    
    private fun navigateToCallDetails(call: Call) {
        // Delegate to the parent flow to handle the navigation
        plugins<Callback>().forEach { it.navigateToCallDetails(call) }
    }
    
    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        
        CallsHistoryView(
            state = state,
            onRoomDetailsClick = ::onRoomDetailsClick,
            currentRoute = BottomNavRoute.Calls,
            onRouteSelect = ::onBottomNavigationRouteSelected,
            onStartCall = { roomId, isAudioCall ->
                val sessionId = presenter.getActiveSessionId()
                // Only initiate the call if we have a valid session ID
                if (sessionId != null) {
                    val callType = io.element.android.features.call.api.CallType.RoomCall(
                        sessionId = sessionId,
                        roomId = RoomId(roomId)
                    )
                    elementCallEntryPoint.startCall(callType, isAudioCall)
                }
            },
            onCallDetailsClick = ::navigateToCallDetails,
            modifier = modifier
        )
    }
}
