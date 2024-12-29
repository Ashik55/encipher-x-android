/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.element.android.libraries.designsystem.icons.CompoundDrawables
import io.element.android.libraries.matrix.ui.R
import io.element.android.libraries.ui.strings.CommonStrings

@Immutable
sealed class AvatarAction(
    @StringRes val titleResId: Int,
    @DrawableRes val iconResourceId: Int,
    val destructive: Boolean = false,
) {
    data object TakePhoto : AvatarAction(
        titleResId = CommonStrings.action_take_photo,
        iconResourceId = R.drawable.ic_camera,
    )

    data object ChoosePhoto : AvatarAction(
        titleResId = CommonStrings.action_choose_photo,
        iconResourceId = R.drawable.ic_image,
    )

    data object Remove : AvatarAction(
        titleResId = CommonStrings.action_remove,
        iconResourceId = io.element.android.libraries.designsystem.R.drawable.ic_remove,
        destructive = true
    )
}
