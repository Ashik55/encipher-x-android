/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.architecture.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import com.bumble.appyx.core.navigation.transition.ModifierTransitionHandler
import com.bumble.appyx.core.navigation.transition.TransitionDescriptor
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.operation.Push

/**
 * Custom safe transition handler for screen transitions that handles null operations gracefully.
 * This implementation avoids using BackStackSlider which causes the "Unexpected operation: null" crashes.
 */
@Composable
fun <NavTarget> rememberDefaultTransitionHandler(): ModifierTransitionHandler<NavTarget, BackStack.State> {
    return SafeTransitionHandler(spring(stiffness = Spring.StiffnessMediumLow))
}

/**
 * A completely custom implementation of a transition handler that doesn't rely on BackStackSlider.
 * This ensures we handle all edge cases safely without risking crashes.
 */
private class SafeTransitionHandler<T>(
    private val animSpec: FiniteAnimationSpec<Float>
) : ModifierTransitionHandler<T, BackStack.State>() {
    
    override fun createModifier(
        modifier: Modifier,
        transition: Transition<BackStack.State>,
        descriptor: TransitionDescriptor<T, BackStack.State>
    ): Modifier {
        return try {
            // We'll use a simplified animation that's resilient to edge cases
            modifier.composed {
                val element = descriptor.element
                val operation = descriptor.operation
                val progress by transition.animateFloat(
                    transitionSpec = { animSpec },
                    label = "SafeTransitionAnimation"
                ) { state ->
                    when (state) {
                        BackStack.State.CREATED -> 0f
                        BackStack.State.STASHED -> 0f
                        BackStack.State.ACTIVE -> 1f
                        else -> 1f  // Fallback to fully visible for any other state
                    }
                }

                // Apply simple, safe animations based on the operation type
                when {
                    operation is Push -> 
                        Modifier
                            .alpha(progress)
                            .scale(0.9f + 0.1f * progress)
                    else -> 
                        Modifier.alpha(progress)
                }
            }
        } catch (e: Exception) {
            // Safety fallback - if anything goes wrong, just return the unmodified modifier
            modifier
        }
    }
}
