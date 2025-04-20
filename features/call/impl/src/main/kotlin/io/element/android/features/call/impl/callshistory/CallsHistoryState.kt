/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import io.element.android.libraries.architecture.AsyncData
import kotlinx.coroutines.flow.StateFlow

interface CallsHistoryState {
    val callsList: StateFlow<AsyncData<List<Call>>>
    val favorites: StateFlow<List<Call>>
    val currentUserId: StateFlow<String?>
}