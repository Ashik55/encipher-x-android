/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption.services

import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.PasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for passkey operations
 */
interface PasskeyApiService {
    @POST("_matrix/client/v3/auth/passkey/{userId}")
    suspend fun savePasskey(
        @Path("userId") userId: String,
        @Body request: SavePasskeyRequest
    ): Response<SavePasskeyResponse>
    
    @GET("_matrix/client/v3/auth/passkey/{userId}")
    suspend fun retrievePasskey(
        @Path("userId") userId: String,
        @Query("passphrase") passphrase: String
    ): Response<PasskeyResponse>
    
    @GET("_matrix/client/v3/auth/check_passkey/{userId}")
    suspend fun checkPasskey(
        @Path("userId") userId: String,
        @Query("passphrase") passphrase: String
    ): Response<CheckPasskeyResponse>

    @GET("_matrix/client/v3/auth/reset_passkey/{userId}")
    suspend fun resetPasskey(
        @Path("userId") userId: String
    ): Response<Unit>
}
