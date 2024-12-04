/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementThemedPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
fun CustomMainActionButton(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = title,
) {
    val ripple = ripple(bounded = false)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier
            .background(color= Color(0xffEFEFEF),
                shape = RoundedCornerShape(15.dp) // Apply rounded corners
            )   .padding(horizontal = 15.dp, vertical = 8.dp) .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = onClick,
                indication = ripple
            )
            .widthIn(min = 76.dp, max = 96.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tintColor = if (enabled) LocalContentColor.current else MaterialTheme.colorScheme.secondary
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tintColor,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title,
            style = ElementTheme.typography.fontBodyMdMedium.copy(hyphens = Hyphens.Auto),
            color = tintColor,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(group = PreviewGroup.Buttons)
@Composable
internal fun CustomMainActionButtonPreview() {
    ElementThemedPreview {
        ContentsToPreview()
    }
}

@Composable
private fun ContentsToPreview() {
    Row(
        modifier = Modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CustomMainActionButton(
            title = "Share",
            imageVector = CompoundIcons.ShareAndroid(),
            onClick = { },
        )
        CustomMainActionButton(
            title = "Share with a long text",
            imageVector = CompoundIcons.ShareAndroid(),
            onClick = { },
        )
        CustomMainActionButton(
            title = "Share",
            imageVector = CompoundIcons.ShareAndroid(),
            onClick = { },
            enabled = false,
        )
    }
}
