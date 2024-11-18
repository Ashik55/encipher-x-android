package io.element.android.libraries.designsystem.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.mediumTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF002F25),
//                color = colorResource(id = R.color.primary_color),
                shape = RoundedCornerShape(48.dp) // Rounded background
            )
            .padding(end = 16.dp) // Ensure padding inside rounded corners
            .padding(start = 8.dp) // Adjust top padding if necessary
            .fillMaxWidth() // Ensure the box takes up the full width
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart) // Align navigation icon to the start
        ) {
            navigationIcon()
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center) // Align logo at the center
        ) {
            // Center: Logo (replace with your logo resource)
            Image(
                painter = painterResource(id = io.element.android.libraries.designsystem.R.drawable.ic_encipher_text_logo), // Replace with your logo resource
                contentDescription = "Logo",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd) // Align actions to the end
        ) {
            CompositionLocalProvider(LocalContentColor provides ElementTheme.colors.textActionPrimary) {
                actions()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(group = PreviewGroup.AppBars)
@Composable
internal fun CustomHomeTopAppBarPreview() = ElementThemedPreview {
    CustomHomeTopAppBar(
        navigationIcon = { BackButton(onClick = {}) },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = CompoundIcons.ShareAndroid(),
                    contentDescription = null,
                )
            }
        }
    )
}
