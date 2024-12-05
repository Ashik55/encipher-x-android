/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.R
import io.element.android.libraries.designsystem.components.drawWithLayer
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight

/**
 * Gradient background for FTUE (onboarding) screens.
 */
@Suppress("ModifierMissing")
@Composable
fun OnboardingBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        val isLightTheme = ElementTheme.isLightTheme
        val backgroundImage = if (isLightTheme) {
            R.drawable.onboarding_background
        } else {
            R.drawable.onboarding_background_dark
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = backgroundImage),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun OnboardingBackgroundPreview() {
    ElementPreview {
        OnboardingBackground()
    }
}
