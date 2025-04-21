/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.call.model

data class Call(
    val call_id: Long? = null,
    val call_type: String? = null,
    val caller_user_id: String? = null,
    val room_id: String? = null,
    val created_ts: String? = null,
    val ended_ts: String? = null,
    val caller_display_name: String? = null,
    val room_name: String? = null,
    val room_avatar: String? = null,
    val caller_avatar: String? = null,
    val is_caller: Boolean? = null,
    val receiver_user_ids: List<String>? = null,
    val receiver_display_names: Map<String, String>? = null,
    val receiver_avatars: Map<String, String>? = null
)
