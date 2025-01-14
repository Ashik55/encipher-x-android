/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

/**
 * Compound component that displays a big icon, a title, an optional subtitle and an optional call to action component.
 *
 * @param title the title to display
 * @param iconStyle the style of the [WelcomeIcon] to display
 * @param modifier the modifier to apply to this layout
 * @param subtitle the optional subtitle to display. It defaults to `null`
 * @param callToAction the optional call to action component to display. It defaults to `null`
 */
@Composable
fun WelcomePageTitle(
    title: AnnotatedString,
    iconStyle: WelcomeIcon.Style,
    modifier: Modifier = Modifier,
    subtitle: AnnotatedString? = null,
    callToAction: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = ElementTheme.typography.fontHeadingMdBold,
                color = ElementTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            subtitle?.let {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = it,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        WelcomeIcon(
            modifier = Modifier.padding(top = 32.dp),
            style = iconStyle, size = NewElementLogoAtomSize.Medium
        )

        callToAction?.invoke()
    }
}

/**
 * Compound component that displays a big icon, a title, an optional subtitle and an optional call to action component.
 *
 * @param title the title to display
 * @param iconStyle the style of the [WelcomeIcon] to display
 * @param modifier the modifier to apply to this layout
 * @param subtitle the optional subtitle to display. It defaults to `null`
 * @param callToAction the optional call to action component to display. It defaults to `null`
 */
@Composable
fun WelcomePageTitle(
    title: String,
    iconStyle: WelcomeIcon.Style,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    callToAction: @Composable (() -> Unit)? = null,
) = WelcomePageTitle(
    title = AnnotatedString(title),
    iconStyle = iconStyle,
    modifier = modifier,
    subtitle = subtitle?.let { AnnotatedString(it) },
    callToAction = callToAction
)


@PreviewsDayNight
@Composable
internal fun WelcomePageTitleWithIconFullPreview(@PreviewParameter(WelcomeIconStyleProvider::class) style: WelcomeIcon.Style) {
    ElementPreview {
        WelcomePageTitle(
            modifier = Modifier.padding(top = 24.dp),
            title = AnnotatedString("Headline"),
            subtitle = AnnotatedString("Description goes here"),
            iconStyle = style,
            callToAction = {
                TextButton(text = "Learn more", onClick = {})
            }
        )
    }
}

@PreviewsDayNight
@Composable
internal fun WelcomePageTitleWithIconMinimalPreview() {
    ElementPreview {
        WelcomePageTitle(
            modifier = Modifier.padding(top = 24.dp),
            title = "Headline",
            iconStyle = WelcomeIcon.Style.Default(CompoundIcons.CheckCircleSolid()),
        )
    }
}
