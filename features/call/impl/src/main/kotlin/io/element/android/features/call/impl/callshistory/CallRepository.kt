/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.isDm
import io.element.android.libraries.matrix.impl.call.model.Call as MatrixCall
import io.element.android.libraries.matrix.impl.call.model.CallDetailsResponse
import io.element.android.libraries.matrix.impl.call.services.CallApiService
import javax.inject.Inject
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn
import retrofit2.Response
import timber.log.Timber

/**
 * Result of a paginated call details request, including the next page info if available.
 */
data class PaginatedCallResult(
    val calls: List<Call>,
    val nextPage: String? = null,
    val prevPage: String? = null
)

interface CallRepository {
    /**
     * Get call details for a specific user with pagination support.
     *
     * @param sessionId The session ID to use
     * @param userId The user ID to get call history for
     * @param roomId Optional room ID to filter calls
     * @param page Optional page token for pagination
     * @return API response containing call details with pagination info
     */
    suspend fun getCallDetails(
        sessionId: SessionId, 
        userId: String, 
        roomId: String? = null,
        page: String? = null
    ): Result<PaginatedCallResult>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultCallRepository @Inject constructor(
    private val matrixCallApiService: CallApiService,
    private val matrixClientProvider: MatrixClientProvider,
) : CallRepository {
    override suspend fun getCallDetails(
        sessionId: SessionId, 
        userId: String, 
        roomId: String?,
        page: String?
    ): Result<PaginatedCallResult> = runCatching {
        Timber.d("Fetching calls for user: $userId, room: $roomId, page: $page")
        
        val response = matrixCallApiService.getCallDetails(userId, roomId, limit = 15, offset = page)
        
        if (response.isSuccessful) {
            val callsResponse = response.body()
            val calls = callsResponse?.calls?: emptyList()
            
            // Get MatrixClient to determine isDm for each call
            val matrixClient = matrixClientProvider.getOrRestore(sessionId).getOrNull()
            
            PaginatedCallResult(
                calls = calls.map { it.toCall(matrixClient) },
                nextPage = callsResponse?.next_page_offset,
                prevPage = callsResponse?.prev_page_offset
            )
        } else {
            Timber.e("Failed to fetch calls: ${response.code()} ${response.message()}")
            throw Exception("Failed to fetch calls: ${response.code()} ${response.message()}")
        }
    }
    
    private suspend fun MatrixCall.toCall(matrixClient: MatrixClient?) = Call(
        call_id = call_id?.toLong(),
        call_type = call_type,
        caller_user_id = caller_user_id,
        room_id = room_id,
        created_ts = created_ts,
        ended_ts = ended_ts,
        caller_display_name = caller_display_name,
        room_name = room_name,
        room_avatar = room_avatar,
        caller_avatar = caller_avatar,
        is_caller = is_caller,
        receiver_user_ids = receiver_user_ids,
        receiver_display_names = receiver_display_names,
        receiver_avatars = receiver_avatars,
        isDm = determineDmStatus(matrixClient)
    )
    
    private suspend fun MatrixCall.determineDmStatus(matrixClient: MatrixClient?): Boolean {
        return try {
            val roomIdString = room_id
            if (matrixClient == null || roomIdString.isNullOrEmpty()) {
                // Fallback: use old logic as backup
                room_name == null
            } else {
                val roomId = RoomId(roomIdString)
                val room = matrixClient.getRoom(roomId)
                room?.isDm ?: false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error determining isDm for room: $room_id, falling back to old logic")
            // Fallback: use old logic if MatrixClient fails
            room_name == null
        }
    }
}
