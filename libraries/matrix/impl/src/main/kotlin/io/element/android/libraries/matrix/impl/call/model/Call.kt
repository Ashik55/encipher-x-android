/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.call.model

data class Call(
    val call_id: Int?,
    val call_type: String?,
    val caller_user_id: String?,
    val created_ts: String?,
    val ended_ts: String?,
    val room_id: String?
)
