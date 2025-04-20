/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultCallApiService @Inject constructor() : CallApiService {
    /**
     * Get call details for a user.
     * Currently returns mock data as this is just a UI demonstration.
     */
    override suspend fun getCallDetails(userId: String, roomId: String?): ApiResponse<List<Call>> {
        // In a real implementation, this would make an API call to fetch call history
        // For now, we just return sample data
        val callsList = listOf(
            Call(
                call_id = 396,
                caller_user_id = "@ben5:dev.enciph-er.com",
                room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                call_type = "audio",
                created_ts = "2025-04-15T09:56:12.505007",
                ended_ts = null
            ),
            Call(
                call_id = 395,
                caller_user_id = "@ben5:dev.enciph-er.com",
                room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                call_type = "video",
                created_ts = "2025-04-15T09:51:27.231343",
                ended_ts = null
            ),
            // Add more mock data as needed
        )

        // Filter by roomId if provided
        val filteredCalls = roomId?.let { id ->
            callsList.filter { it.room_id == id }
        } ?: callsList

        return ApiResponse(data = filteredCalls)
    }
}