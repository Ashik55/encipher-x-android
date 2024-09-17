/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.api.pinned

import androidx.compose.runtime.Composable

fun interface IsPinnedMessagesFeatureEnabled {
    @Composable
    operator fun invoke(): Boolean
}