/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

/**
 * Service interface for making API calls related to call history.
 */
interface CallApiService {
    /**
     * Get call details for a specific user.
     *
     * @param userId The ID of the user to get call details for
     * @param roomId Optional room ID to filter calls
     * @param page Optional page number for pagination
     * @return API response containing call details
     */
    suspend fun getCallDetails(userId: String, roomId: String? = null, page: Int? = null): ApiResponse<List<Call>>
}

/**
 * Simple API response wrapper to handle success and error cases.
 */
data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val isSuccessful: Boolean = error == null,
    val nextPage: Int? = null
)