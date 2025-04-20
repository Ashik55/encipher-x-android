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
import io.element.android.libraries.designsystem.components.navbar.BottomNavRoute
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.core.RoomId

@ContributesNode(SessionScope::class)
class CallsHistoryNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: CallsHistoryPresenter
) : Node(buildContext, plugins = plugins) {
    
    interface Callback : Plugin {
        fun onRoomDetailsClick(roomId: RoomId)
        fun onHomeClick()
        fun onSettingsClick()
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
            BottomNavRoute.Home -> callbacks.forEach { it.onHomeClick() }
            BottomNavRoute.Settings -> callbacks.forEach { it.onSettingsClick() }
            BottomNavRoute.Calls -> { /* Already on calls screen, do nothing */ }
        }
    }
    
    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        
        CallsHistoryView(
            state = state,
            onRoomDetailsClick = ::onRoomDetailsClick,
            currentRoute = BottomNavRoute.Calls,
            onRouteSelect = ::onBottomNavigationRouteSelected,
            modifier = modifier
        )
    }
}
