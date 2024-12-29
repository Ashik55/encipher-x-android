/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.atomic.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.Badge
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.badgeNegativeBackgroundColor
import io.element.android.libraries.designsystem.theme.badgeNegativeContentColor
import io.element.android.libraries.designsystem.theme.badgeNeutralBackgroundColor
import io.element.android.libraries.designsystem.theme.badgeNeutralContentColor
import io.element.android.libraries.designsystem.theme.badgePositiveBackgroundColor
import io.element.android.libraries.designsystem.theme.badgePositiveContentColor

object CustomMatrixBadgeAtom {
    data class CustomMatrixBadgeData(
        val text: String,
        val icon: ImageVector,
        val type: Type,
    )

    enum class Type {
        Positive,
        Neutral,
        Negative
    }

    @Composable
    fun View(
        data: CustomMatrixBadgeData,
    ) {
        val backgroundColor = when (data.type) {
            Type.Positive -> ElementTheme.colors.badgePositiveBackgroundColor
            Type.Neutral -> ElementTheme.colors.badgeNeutralBackgroundColor
            Type.Negative -> ElementTheme.colors.badgeNegativeBackgroundColor
        }
        val textColor = when (data.type) {
            Type.Positive -> ElementTheme.colors.badgePositiveContentColor
            Type.Neutral -> ElementTheme.colors.badgeNeutralContentColor
            Type.Negative -> ElementTheme.colors.badgeNegativeContentColor
        }
        val iconColor = when (data.type) {
            Type.Positive -> ElementTheme.colors.iconSuccessPrimary
            Type.Neutral -> ElementTheme.colors.iconSecondary
            Type.Negative -> ElementTheme.colors.iconCriticalPrimary
        }
        Badge(
            text = data.text,
            icon = data.icon,
            backgroundColor = Color(0xff0A8741),
            iconColor = Color.White,
//            iconColor = iconColor,
            textColor = Color.White,
            style =  TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W400)
//            textColor = textColor,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun CustomMatrixBadgeAtomPositivePreview() = ElementPreview {
    CustomMatrixBadgeAtom.View(
        CustomMatrixBadgeAtom.CustomMatrixBadgeData(
            text = "Trusted",
            icon = CompoundIcons.Verified(),
            type = CustomMatrixBadgeAtom.Type.Positive,
        )
    )
}

@PreviewsDayNight
@Composable
internal fun CustomMatrixBadgeAtomNeutralPreview() = ElementPreview {
    CustomMatrixBadgeAtom.View(
        CustomMatrixBadgeAtom.CustomMatrixBadgeData(
            text = "Public room",
            icon = CompoundIcons.Public(),
            type = CustomMatrixBadgeAtom.Type.Neutral,
        )
    )
}

@PreviewsDayNight
@Composable
internal fun CustomMatrixBadgeAtomNegativePreview() = ElementPreview {
    CustomMatrixBadgeAtom.View(
        CustomMatrixBadgeAtom.CustomMatrixBadgeData(
            text = "Not trusted",
            icon = CompoundIcons.Error(),
            type = CustomMatrixBadgeAtom.Type.Negative,
        )
    )
}
