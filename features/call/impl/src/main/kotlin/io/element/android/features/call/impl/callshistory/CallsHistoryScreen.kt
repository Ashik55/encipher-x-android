/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.runtime.Composable
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.designsystem.components.navbar.BottomNavRoute
import io.element.android.libraries.di.AppScope
import io.element.android.features.call.impl.callshistory.details.CallDetailsScreen

object CallsHistoryEntryPoint : NodeInputs

@Composable
fun CallsHistoryScreen(
    presenter: CallsHistoryPresenter,
    onRoomDetailsClick: (roomId: String) -> Unit,
    currentRoute: BottomNavRoute,
    onRouteSelect: (BottomNavRoute) -> Unit,
    onStartCall: (roomId: String, isAudioCall: Boolean) -> Unit,
    onCallDetailsClick: (call: Call) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    // Call the @Composable present() function directly from this @Composable context
    val state = presenter.present()

    CallsHistoryView(
        state = state,
        onRoomDetailsClick = onRoomDetailsClick,
        currentRoute = currentRoute,
        onRouteSelect = onRouteSelect,
        onStartCall = onStartCall,
        onCallDetailsClick = onCallDetailsClick,
        modifier = modifier
    )
}

@ContributesNode(AppScope::class)
class CallsHistoryScreen @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: CallsHistoryPresenter,
) : Node(buildContext, plugins = plugins) {

    interface Factory {
        fun create(
            buildContext: BuildContext,
            plugins: List<Plugin>,
        ): CallsHistoryScreen
    }
    
    private fun onBottomNavigationRouteSelected(route: BottomNavRoute) {
        when (route) {
            BottomNavRoute.Chats -> navigateTo(route)
            BottomNavRoute.Settings -> navigateTo(route)
            BottomNavRoute.Calls -> {} // Already on calls screen, do nothing
        }
    }
    
    private fun navigateTo(route: BottomNavRoute) {
        // We should use the existing navigation system here
        // For now, this is just a placeholder
    }

    @Composable
    override fun View(modifier: androidx.compose.ui.Modifier) {
        CallsHistoryScreen(
            presenter = presenter,
            onRoomDetailsClick = { roomId ->
                // Navigate to room details or handle click
            },
            currentRoute = BottomNavRoute.Calls,
            onRouteSelect = ::onBottomNavigationRouteSelected,
            onStartCall = { roomId, isAudioCall ->
                // Handle starting a call
            },
            onCallDetailsClick = { call ->
                // Navigate to call details screen
                navigateToCallDetails(call)
            },
            modifier = modifier
        )
    }

    private fun navigateToCallDetails(call: Call) {
        // Navigate to the CallDetailsView
        // This implementation depends on the navigation system you're using
        // For example, if using Appyx, you might use a navigator to navigate to CallDetailsNode
    }
}
