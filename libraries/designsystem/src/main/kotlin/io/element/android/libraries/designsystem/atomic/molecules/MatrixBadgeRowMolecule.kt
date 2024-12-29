/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.designsystem.atomic.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.atomic.atoms.CustomMatrixBadgeAtom
import io.element.android.libraries.designsystem.atomic.atoms.MatrixBadgeAtom
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MatrixBadgeRowMolecule(
    data: ImmutableList<MatrixBadgeAtom.MatrixBadgeData>,//    data: ImmutableList<MatrixBadgeAtom.MatrixBadgeData>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier // Apply background color
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (badge in data) {
//            CustomMatrixBadgeAtom.View(badge)
            MatrixBadgeAtom.View(badge)
        }
    }
}

@Composable
fun CustomMatrixBadgeRowMolecule(
    data: ImmutableList<CustomMatrixBadgeAtom.CustomMatrixBadgeData>,
//    data: ImmutableList<MatrixBadgeAtom.MatrixBadgeData>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier // Apply background color
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (badge in data) {
            CustomMatrixBadgeAtom.View(badge)
//            MatrixBadgeAtom.View(badge)
        }
    }
}
