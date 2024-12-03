/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.textcomposer.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.textcomposer.R

@Composable
internal fun ComposerOptionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier
            .size(48.dp),
        onClick = onClick
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_plus_composer),
            contentDescription = stringResource(R.string.rich_text_editor_a11y_add_attachment),
            modifier = Modifier.size(30.dp)
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ComposerOptionsButtonPreview() = ElementPreview {
    ComposerOptionsButton(onClick = {})
}
