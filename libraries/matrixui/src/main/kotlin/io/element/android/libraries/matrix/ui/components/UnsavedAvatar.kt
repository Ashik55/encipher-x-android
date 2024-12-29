/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.debugPlaceholderBackground
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.temporaryColorBgSpecial
import io.element.android.libraries.matrix.ui.R

/**
 * An avatar that the user has selected, but which has not yet been uploaded to Matrix.
 *
 * The image is loaded from a local resource instead of from a MXC URI.
 */
@Composable
fun UnsavedAvatar(
    avatarUri: Uri?,
    modifier: Modifier = Modifier,
) {
    val commonModifier = modifier
        .size(60.dp)
        .clip(CircleShape)
        .border(
            width = 1.dp,
            color = ElementTheme.materialColors.outline,
            shape = CircleShape
        )

    if (avatarUri != null) {
        val context = LocalContext.current
        val model = ImageRequest.Builder(context)
            .data(avatarUri)
            .build()
        AsyncImage(
            modifier = commonModifier,
            model = model,
            placeholder = debugPlaceholderBackground(ColorPainter(MaterialTheme.colorScheme.surfaceVariant)),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    } else {
        Box(modifier = commonModifier
            .background(MaterialTheme.colorScheme.surface)
//            .border(width = 1.dp, color = ElementTheme.materialColors.outline)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id =  R.drawable.ic_camera),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun UnsavedAvatarPreview() = ElementPreview {
    Row {
        UnsavedAvatar(null)
        UnsavedAvatar(Uri.EMPTY)
    }
}
