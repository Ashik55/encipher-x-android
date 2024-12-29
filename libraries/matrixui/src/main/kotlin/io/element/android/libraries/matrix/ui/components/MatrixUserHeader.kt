/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatarBloom
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toDp
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.getBestName

@Composable
fun MatrixUserHeader(
    matrixUser: MatrixUser?,
    modifier: Modifier = Modifier,
    // TODO handle click on this item, to let the user be able to update their profile.
    // onClick: () -> Unit,
) {
    if (matrixUser == null) {
        MatrixUserHeaderPlaceholder(modifier = modifier)
    } else {
        MatrixUserHeaderContent(
            matrixUser = matrixUser,
            modifier = modifier,
            // onClick = onClick
        )
    }
}

@Composable
private fun MatrixUserHeaderContent(
    matrixUser: MatrixUser,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Box to add green border with a drop shadow effect
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color = Color.White)
                .border(
                    width = 4.dp,
                    color = Color(0xFF0A8741),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Avatar(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                avatarData = matrixUser.getAvatarData(size = AvatarSize.UserPreference),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Name
            Text(
                modifier = Modifier.clipToBounds(),
                text = matrixUser.getBestName(),
                maxLines = 1,
                style = ElementTheme.typography.fontHeadingSmMedium,
                overflow = TextOverflow.Ellipsis,
                color = ElementTheme.materialColors.primary,
            )
            // ID
            if (matrixUser.displayName.isNullOrEmpty().not()) {
                Text(
                    text = matrixUser.userId.value,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.materialColors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


    @PreviewsDayNight
@Composable
internal fun MatrixUserHeaderPreview(@PreviewParameter(MatrixUserProvider::class) matrixUser: MatrixUser) = ElementPreview {
    MatrixUserHeader(matrixUser)
}
