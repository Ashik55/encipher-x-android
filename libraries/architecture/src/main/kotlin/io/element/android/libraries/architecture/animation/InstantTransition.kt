/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.architecture.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import com.bumble.appyx.core.navigation.transition.ModifierTransitionHandler
import com.bumble.appyx.core.navigation.transition.TransitionDescriptor
import com.bumble.appyx.navmodel.backstack.BackStack

/**
 * Custom safe instant transition handler that handles null operations gracefully.
 * This implementation avoids using BackStackSlider which causes the "Unexpected operation: null" crashes.
 */
@Composable
fun <NavTarget> rememberInstantTransitionHandler(): ModifierTransitionHandler<NavTarget, BackStack.State> {
    return SafeInstantTransitionHandler()
}

/**
 * A completely custom implementation of an instant transition handler that doesn't rely on BackStackSlider.
 * This ensures we handle all edge cases safely without risking crashes.
 */
private class SafeInstantTransitionHandler<T> : ModifierTransitionHandler<T, BackStack.State>() {
    
    // Create a zero-duration tween animation for instant transitions
    private val instantAnimSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 0)
    
    override fun createModifier(
        modifier: Modifier,
        transition: Transition<BackStack.State>,
        descriptor: TransitionDescriptor<T, BackStack.State>
    ): Modifier {
        return try {
            // We'll use a simplified instant transition that's resilient to edge cases
            modifier.composed {
                val progress by transition.animateFloat(
                    transitionSpec = { instantAnimSpec }, // Zero duration for instant effect
                    label = "SafeInstantTransition"
                ) { state ->
                    when (state) {
                        BackStack.State.CREATED -> 0f
                        BackStack.State.STASHED -> 0f
                        BackStack.State.ACTIVE -> 1f
                        else -> 1f  // Fallback to fully visible for any other state
                    }
                }

                // Simple alpha modifier to handle visibility
                Modifier.alpha(progress)
            }
        } catch (e: Exception) {
            // Safety fallback - if anything goes wrong, just return the unmodified modifier
            modifier
        }
    }
}
