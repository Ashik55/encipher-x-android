/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.atomic.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.LockIcon
import io.element.android.libraries.designsystem.components.NewElementLogoAtomSize
import io.element.android.libraries.designsystem.components.RecoveryKeyIcon
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text

/**
 * NewIconTitleSubtitleMolecule is a molecule which displays an icon, a title and a subtitle.
 *
 * @param title the title to display
 * @param subTitle the subtitle to display
 * @param iconStyle the style of the [LockIcon] to display
 * @param modifier the modifier to apply to this layout
 */
@Composable
fun NewIconTitleSubtitleMolecule(
    title: String,
    subTitle: String?,
    iconStyle: LockIcon.Style,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = ElementTheme.typography.fontHeadingMdBold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (subTitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subTitle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        LockIcon(
            modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally),
            style = iconStyle, size = NewElementLogoAtomSize.Medium
        )
    }
}

@PreviewsDayNight
@Composable
internal fun NewIconTitleSubtitleMoleculePreview() = ElementPreview {
    NewIconTitleSubtitleMolecule(
        iconStyle = LockIcon.Style.Default(CompoundIcons.Chat()),
        title = "Title",
        subTitle = "Subtitle",
    )
}