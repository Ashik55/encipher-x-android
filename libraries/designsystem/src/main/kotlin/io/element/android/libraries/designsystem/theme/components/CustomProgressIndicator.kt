/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import io.element.android.libraries.designsystem.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme.isLightTheme
import io.element.android.libraries.designsystem.preview.ElementThemedPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup
import kotlinx.coroutines.launch

@Composable
fun CustomProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    trackColor: Color = ProgressIndicatorDefaults.circularDeterminateTrackColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        progress = progress,
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
    )
}

@Composable
fun CustomProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val scaleAnimation = remember { Animatable(1f) }

    // Static preview for design-time rendering
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = if (isLightTheme) {
                    R.drawable.ic_lock_light
                } else {
                    R.drawable.ic_lock_dark
                }),
                contentDescription = "Loading",
                modifier = Modifier
                    .size(size)
                    .scale(1.1f)
            )
        }
    } else {
        // Animated version for runtime
        LaunchedEffect(Unit) {
            launch {
                scaleAnimation.animateTo(
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = if (isLightTheme) {
                R.drawable.ic_lock_light
            } else {
                R.drawable.ic_lock_dark
            }),
                contentDescription = "Loading",
                modifier = Modifier
                    .size(size)
                    .scale(scaleAnimation.value)
            )
        }
    }
}

@Preview(group = PreviewGroup.Progress)
@Composable
internal fun CustomProgressIndicatorPreview() = ElementThemedPreview(vertical = false) {
    Column(
        modifier = Modifier.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indeterminate progress
        Text("Indeterminate")
        CustomProgressIndicator()
        // Fixed progress
        Text("Fixed progress")
        CustomProgressIndicator(
            progress = { 0.50F }
        )
    }
}
