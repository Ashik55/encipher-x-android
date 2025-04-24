/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.lifecycle.subscribe
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.activeElement
import com.bumble.appyx.navmodel.backstack.operation.push
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.features.call.api.ElementCallEntryPoint
import io.element.android.features.call.impl.callshistory.Call
import io.element.android.features.call.impl.callshistory.CallsHistoryNode
import io.element.android.features.call.impl.callshistory.details.CallDetailsInput
import io.element.android.features.call.impl.callshistory.details.CallDetailsScreen
import io.element.android.libraries.architecture.BackstackView
import io.element.android.libraries.architecture.BaseFlowNode
import io.element.android.libraries.architecture.createNode
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@ContributesNode(AppScope::class)
class CallsFlowNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val elementCallEntryPoint: ElementCallEntryPoint,
) : BaseFlowNode<CallsFlowNode.NavTarget>(
    backstack = BackStack(
        initialElement = NavTarget.CallHistory,
        savedStateMap = buildContext.savedStateMap,
    ),
    buildContext = buildContext,
    plugins = plugins,
) {
    sealed interface NavTarget : Parcelable {
        @Parcelize
        data object CallHistory : NavTarget

        @Parcelize
        data class CallDetails(val call: @RawValue Call) : NavTarget
    }

    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node {
        return when (navTarget) {
            is NavTarget.CallHistory -> {
                val callback = object : CallsHistoryNode.Callback {
                    override fun onRoomDetailsClick(roomId: RoomId) {
                        // Handle room details navigation
                    }

                    override fun onHomeClick() {
                        // Handle home navigation
                    }

                    override fun onSettingsClick() {
                        // Handle settings navigation
                    }

                    override fun navigateToCallDetails(call: Call) {
                        backstack.push(NavTarget.CallDetails(call))
                    }
                }
                createNode<CallsHistoryNode>(buildContext, plugins = listOf(callback))
            }
            is NavTarget.CallDetails -> {
                val callDetailsInput = CallDetailsInput(navTarget.call)
                val callback = object : CallDetailsScreen.Callback {
                    override fun onBackPressed() {
                        navigateUp()
                    }

                    override fun onRoomDetailsClick(roomId: RoomId) {
                        // Handle room details navigation
                    }
                }
                createNode<CallDetailsScreen>(buildContext, plugins = listOf(callback, callDetailsInput))
            }
        }
    }

    @Composable
    override fun View(modifier: Modifier) {
        BackstackView()
    }
}