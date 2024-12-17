/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.R
import io.element.android.libraries.designsystem.modifiers.blurCompat
import io.element.android.libraries.designsystem.modifiers.blurredShapeShadow
import io.element.android.libraries.designsystem.modifiers.canUseBlurMaskFilter
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.ui.strings.CommonStrings

/**
 * Compound component that display a big icon centered in a rounded square.
 * Figma: https://www.figma.com/design/G1xy0HDZKJf5TCRFmKb5d5/Compound-Android-Components?node-id=1960-553&node-type=frame&m=dev
 */
object ProfileIcon {
    /**
     * The style of the [ProfileIcon].
     */
    @Immutable
    sealed interface Style {
        /**
         * The default style.
         *
         * @param vectorIcon the [ImageVector] to display
         * @param contentDescription the content description of the icon, if any. It defaults to `null`
         */
        data class Default(val vectorIcon: ImageVector, val contentDescription: String? = null) : Style

        /**
         * An alert style with a transparent background.
         */
        data object Alert : Style

        /**
         * An alert style with a tinted background.
         */
        data object AlertSolid : Style

        /**
         * A success style with a transparent background.
         */
        data object Success : Style

        /**
         * A success style with a tinted background.
         */
        data object SuccessSolid : Style
    }

    /**
     * Display a [ProfileIcon].
     *
     * @param style the style of the icon
     * @param modifier the modifier to apply to this layout
     */
    @Composable
    operator fun invoke(
        style: Style,
        size: NewElementLogoAtomSize,
        modifier: Modifier = Modifier,
        useBlurredShadow: Boolean = canUseBlurMaskFilter(),
        darkTheme: Boolean = isSystemInDarkTheme(),
    ) {
        val backgroundColor = when (style) {
            is Style.Default -> ElementTheme.colors.bgSubtleSecondary
            Style.Alert, Style.Success -> Color.Transparent
            Style.AlertSolid -> ElementTheme.colors.bgCriticalSubtle
            Style.SuccessSolid -> ElementTheme.colors.bgSuccessSubtle
        }
        val icon = when (style) {
            is Style.Default -> painterResource(id = R.drawable.ic_single_profile)
            Style.Alert, Style.AlertSolid -> painterResource(id = R.drawable.ic_error)
            Style.Success, Style.SuccessSolid -> painterResource(id = R.drawable.ic_check_circle)
        }
        val contentDescription = when (style) {
            is Style.Default -> style.contentDescription
            Style.Alert, Style.AlertSolid -> stringResource(CommonStrings.common_error)
            Style.Success, Style.SuccessSolid -> stringResource(CommonStrings.common_success)
        }
        val iconTint = when (style) {
            is Style.Default -> ElementTheme.colors.iconSecondary
            Style.Alert, Style.AlertSolid -> ElementTheme.colors.iconCriticalPrimary
            Style.Success, Style.SuccessSolid -> ElementTheme.colors.iconSuccessPrimary
        }

        val blur = if (darkTheme) 160.dp else 24.dp
        val borderColor = if (darkTheme) Color.White.copy(alpha = 0.89f) else Color.White

        val shadowColor = Color(0xFF0A8741).copy(alpha = 0.5f)

        Box(
            modifier = modifier
                .size(size.outerSize)
                .shadow(
                    elevation = size.shadowRadius,
                    shape = RoundedCornerShape(size.cornerRadius),
                    ambientColor = shadowColor
                )
                .background(backgroundColor, shape = RoundedCornerShape(size.cornerRadius))
                .border(
                    width = size.borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(size.cornerRadius)
                ),
//                .blurCompat(blur),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier
                    .size(size.logoSize),
                painter = icon,
                contentDescription = contentDescription
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ProfileIconPreview() {
    ElementPreview {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(10.dp)) {
            val provider = ProfileIconStyleProvider()
            for (style in provider.values) {
                ProfileIcon(style = style, size = NewElementLogoAtomSize.Medium)
            }
        }
    }
}

internal class ProfileIconStyleProvider : PreviewParameterProvider<ProfileIcon.Style> {
    override val values: Sequence<ProfileIcon.Style>
        get() = sequenceOf(
            ProfileIcon.Style.Default(Icons.Filled.CatchingPokemon),
            ProfileIcon.Style.Alert,
            ProfileIcon.Style.AlertSolid,
            ProfileIcon.Style.Success,
            ProfileIcon.Style.SuccessSolid
        )
}
