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
import kotlin.random.Random

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultCallApiService @Inject constructor() : CallApiService {
    /**
     * Get call details for a user with pagination support.
     * Currently returns mock data as this is just a UI demonstration.
     */
    override suspend fun getCallDetails(userId: String, roomId: String?, page: Int?): ApiResponse<List<Call>> {
        val allCalls = generateSampleCallsList()
        val pageSize = 10
        val currentPage = page ?: 1
        
        // Filter by roomId if provided
        val filteredCalls = roomId?.let { id ->
            allCalls.filter { it.room_id == id }
        } ?: allCalls

        // Paginate the results
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredCalls.size)
        
        // Check if there are more pages
        val hasNextPage = filteredCalls.size > endIndex
        val nextPageNumber = if (hasNextPage) currentPage + 1 else null
        
        // Get the calls for the current page
        val callsForPage = if (startIndex < filteredCalls.size) {
            filteredCalls.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return ApiResponse(
            data = callsForPage,
            nextPage = nextPageNumber
        )
    }
    
    /**
     * Generate a large sample list of calls for testing pagination.
     */
    private fun generateSampleCallsList(): List<Call> {
        val baseList = mutableListOf(
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
            )
        )
        
        // Generate more sample calls for pagination testing
        val callTypes = listOf("audio", "video")
        val userIds = listOf("@ben5:dev.enciph-er.com", "@alice:dev.enciph-er.com", "@bob:dev.enciph-er.com")
        val roomIds = listOf("!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com", "!RoomTwo:dev.enciph-er.com")
        
        // Add more calls to demonstrate pagination
        for (i in 1..40) {
            val day = Random.nextInt(1, 30)
            val hour = Random.nextInt(0, 24)
            val minute = Random.nextInt(0, 60)
            val callType = callTypes[Random.nextInt(callTypes.size)]
            val callerId = userIds[Random.nextInt(userIds.size)]
            val roomId = roomIds[Random.nextInt(roomIds.size)]
            
            baseList.add(
                Call(
                    call_id = 400L + i,
                    caller_user_id = callerId,
                    room_id = roomId,
                    call_type = callType,
                    created_ts = "2025-04-${day}T${hour}:${minute}:12.505007",
                    ended_ts = if (Random.nextBoolean()) "2025-04-${day}T${hour}:${minute + Random.nextInt(1, 30)}:27.231343" else null
                )
            )
        }
        
        return baseList
    }
}