/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.architecture.animation

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.bumble.appyx.core.navigation.transition.ModifierTransitionHandler
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.transitionhandler.rememberBackstackSlider

@Composable
fun <NavTarget> rememberInstantTransitionHandler(): ModifierTransitionHandler<NavTarget, BackStack.State> {
    return rememberBackstackSlider(
        transitionSpec = { tween(0) }, // Set duration to 0 for instant transition
    )
} 
