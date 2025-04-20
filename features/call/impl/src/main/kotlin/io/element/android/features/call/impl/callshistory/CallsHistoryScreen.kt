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

object CallsHistoryEntryPoint : NodeInputs

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
        // This is a placeholder implementation 
        // In a real implementation, you would handle navigation between tabs
    }

    @Composable
    override fun View(modifier: androidx.compose.ui.Modifier) {
        val state = presenter.present()
        CallsHistoryView(
            state = state,
            onRoomDetailsClick = { roomId ->
                // Navigate to room details or handle click
                // navigator.navigateToRoomDetails(roomId)
            },
            currentRoute = BottomNavRoute.Calls,
            onRouteSelect = ::onBottomNavigationRouteSelected,
            modifier = modifier,
        )
    }
}