package io.element.android.libraries.designsystem.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementThemedPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomHomeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.mediumTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        modifier = modifier
            .background(
                color = Color(0xFF002F25),  // Set the background color
                shape = RoundedCornerShape(48.dp)  // Apply rounded corners at the bottom
            ),
        navigationIcon = navigationIcon,
        actions = {
            CompositionLocalProvider(LocalContentColor provides ElementTheme.colors.textActionPrimary) {
                actions()
            }
        },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
        title = {
            // Use Row to align title horizontally between navigationIcon and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                title()  // This will allow your title composable to be injected
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(group = PreviewGroup.AppBars)
@Composable
internal fun CustomHomeTopAppBarPreview() = ElementThemedPreview {
    TopAppBar(
        title = {
            // Here, we include an image inside the title composable
            Image(
                painter = painterResource(id = io.element.android.libraries.designsystem.R.drawable.ic_encipher_text_logo),
                contentDescription = "Logo"
            )
        },
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
        modifier = Modifier.background(Color(0xFF002F25)) // Optional background color
    )
}
