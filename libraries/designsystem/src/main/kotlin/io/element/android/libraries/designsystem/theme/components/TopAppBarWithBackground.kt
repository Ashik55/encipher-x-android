/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.R
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementThemedPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithBackground(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    backgroundImage: Int
) {
    Box(modifier = modifier
        .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = backgroundImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        androidx.compose.material3.TopAppBar(
            title = title,
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = navigationIcon,
            actions = {
                CompositionLocalProvider(LocalContentColor provides ElementTheme.colors.textActionPrimary) {
                    actions()
                }
            },
            windowInsets = windowInsets,
            colors = colors.copy(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            scrollBehavior = scrollBehavior,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(group = PreviewGroup.AppBars)
@Composable
internal fun TopAppBarWithBackgroundPreview() = ElementThemedPreview {
    TopAppBarWithBackground(
        title = { Text(text = "Title") },
        navigationIcon = { BackButton(onClick = {}) },
        actions = {
            TextButton(text = "Action", onClick = {})
            IconButton(onClick = {}) {
                Icon(
                    imageVector = CompoundIcons.ShareAndroid(),
                    contentDescription = null,
                )
            }
        },
        backgroundImage = R.drawable.home_top_bg
    )
}
