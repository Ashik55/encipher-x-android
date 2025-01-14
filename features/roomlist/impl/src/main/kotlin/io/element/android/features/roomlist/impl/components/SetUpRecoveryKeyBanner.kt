/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.roomlist.impl.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.element.android.features.roomlist.impl.R
import io.element.android.libraries.designsystem.atomic.molecules.DialogLikeBannerMolecule
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight

@Composable
internal fun SetUpRecoveryKeyBanner(
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DialogLikeBannerMolecule(
        modifier = modifier,
        title = stringResource(R.string.banner_set_up_recovery_title),
        titleColor = Color(0xFFEB3E18),
        content = stringResource(R.string.banner_set_up_recovery_content),
        actionText = stringResource(R.string.banner_set_up_recovery_submit),
        onSubmitClick = onContinueClick,
        onDismissClick = onDismissClick,
    )
}

@PreviewsDayNight
@Composable
internal fun SetUpRecoveryKeyBannerPreview() = ElementPreview {
    SetUpRecoveryKeyBanner(
        onContinueClick = {},
        onDismissClick = {},
    )
}
