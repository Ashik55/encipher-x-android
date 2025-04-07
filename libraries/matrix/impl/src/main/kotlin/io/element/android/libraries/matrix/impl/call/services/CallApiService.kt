/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.call.services

import io.element.android.libraries.matrix.impl.call.model.CallDetailsResponse
import io.element.android.libraries.matrix.impl.call.model.CallRequestBody
import io.element.android.libraries.matrix.impl.call.model.CreateCallResponse
import retrofit2.Response // Make sure this is from retrofit2, not okhttp3
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CallApiService {
    @POST("call/{userId}")
    suspend fun createCall(
        @Path("userId") userId: String?,
        @Body body: CallRequestBody
    ): Response<CreateCallResponse>


    @GET("call/{userId}")
    suspend fun getCallDetails(
        @Path("userId") userId: String?,
        @Query("room_id") roomId: String?
    ): Response<CallDetailsResponse>

}
