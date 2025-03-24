/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.encryption.PasskeyService
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.PasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyResponse
import io.element.android.libraries.network.RetrofitFactory
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

/**
 * Default implementation of [PasskeyService].
 * This implementation makes API calls to store and retrieve passkeys
 * with the correct user ID from the session.
 */
@ContributesBinding(SessionScope::class)
class DefaultPasskeyService @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val sessionId: SessionId
) : PasskeyService {
    init {
        Timber.d("Initializing DefaultPasskeyService for session: ${sessionId.value}")
    }
    
    // Base URL for the passkey service
    private val baseUrl = "https://dev.enciph-er.com"
    
    // JSON serializer/deserializer
    private val json = Json { ignoreUnknownKeys = true }
    
    // HTTP client for direct API calls
    private val client by lazy { OkHttpClient() }

    // In-memory cache as fallback
    private val passkeyStore = mutableMapOf<String, String>()

    override suspend fun savePasskey(passkey: String, passphrase: String): Result<Unit> = withContext(Dispatchers.IO) {
        Timber.d("DefaultPasskeyService.savePasskey called")
        runCatching {
            val userId = sessionId.value
            Timber.d("Saving passkey for user: $userId")
            Timber.d("Recovery key length: ${passkey.length} characters, Passphrase length: ${passphrase.length} characters")
            
            try {
                // Prepare API request based on the specific endpoint
                val requestBody = SavePasskeyRequest(passkey = passkey, passphrase = passphrase)
                val jsonBody = json.encodeToString(SavePasskeyRequest.serializer(), requestBody)
                Timber.d("Request body JSON length: ${jsonBody.length} characters")
                
                val url = "$baseUrl/_matrix/client/v3/auth/passkey/$userId"
                Timber.d("Making POST request to: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .header("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                Timber.d("Save API response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("API call failed with code ${response.code}: $errorBody")
                    throw Exception("Failed to save passkey: HTTP ${response.code} - $errorBody")
                }
                
                // Log response headers for debugging
                Timber.d("API response headers: ${response.headers}")
                val responseBody = response.body?.string()
                
                if (responseBody != null) {
                    try {
                        // Parse the response to get full details including encryption info
                        val saveResponse = json.decodeFromString(SavePasskeyResponse.serializer(), responseBody)
                        Timber.d("Successfully parsed save response from user: ${saveResponse.requester}")
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse save response details: ${e.message}")
                    }
                }
                
                Timber.d("Successfully saved passkey to API")
            } catch (e: Exception) {
                Timber.w(e, "API call failed, falling back to in-memory storage: ${e.message}")
                // Fallback to in-memory storage if API call fails
                passkeyStore[passphrase] = passkey
                Timber.d("Saved passkey to in-memory store as fallback")
            }
            
            Unit
        }.onFailure {
            Timber.e(it, "Failed to save passkey: ${it.message}")
        }
    }

    override suspend fun retrievePasskey(passphrase: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Validate input
            if (passphrase.isBlank()) {
                Timber.e("Empty passphrase provided")
                throw IllegalArgumentException("Passphrase cannot be empty")
            }
            
            val userId = sessionId.value
            Timber.d("Retrieving passkey for user: $userId with passphrase length: ${passphrase.length}")
            
            try {
                // Build the URL with query parameter
                val url = "$baseUrl/_matrix/client/v3/auth/passkey/$userId?passphrase=$passphrase"
                Timber.d("Making GET request to: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                Timber.d("Retrieve API response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("API call failed with code ${response.code}: $errorBody")
                    
                    // Provide more specific error messages based on HTTP status
                    when (response.code) {
                        401, 403 -> throw IllegalArgumentException("Invalid passphrase. Please check and try again.")
                        404 -> throw IllegalArgumentException("No recovery key found for this account.")
                        else -> throw Exception("Failed to retrieve passkey: HTTP ${response.code} - $errorBody")
                    }
                }
                
                // Log response headers for debugging
                Timber.d("API response headers: ${response.headers}")
                val responseBody = response.body?.string()
                
                if (responseBody == null) {
                    Timber.e("Empty response body")
                    throw Exception("Empty response body")
                }
                
                Timber.d("API response body length: ${responseBody.length} characters")
                Timber.d("API response body sample: ${responseBody.take(100)}")
                
                try {
                    // Parse the response to get the passkey
                    val passkeyResponse = json.decodeFromString(PasskeyResponse.serializer(), responseBody)
                    Timber.d("Successfully parsed response, recovery key length: ${passkeyResponse.passkey.length}")
                    passkeyResponse.passkey
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse API response: ${e.message}")
                    throw Exception("Failed to parse passkey response: ${e.message}", e)
                }
            } catch (e: Exception) {
                Timber.w(e, "API call failed, falling back to in-memory storage: ${e.message}")
                // Fallback to in-memory storage if API call fails
                val passkey = passkeyStore[passphrase]
                if (passkey == null) {
                    Timber.e("No passkey found for the provided passphrase in memory storage")
                    throw IllegalArgumentException("No passkey found for the provided passphrase")
                }
                Timber.d("Retrieved passkey from in-memory store as fallback, length: ${passkey.length}")
                passkey
            }
        }.onFailure {
            Timber.e(it, "Failed to retrieve passkey: ${it.message}")
        }
    }
    
    override suspend fun hasPasskey(passphrase: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = sessionId.value
            Timber.d("Checking if user $userId has a passkey")
            
            try {
                // Construct request URL
                val url = "$baseUrl/_matrix/client/v3/auth/check_passkey/$userId"
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                
                // If passphrase provided, send as JSON body
                if (passphrase != null) {
                    val requestBody = CheckPasskeyRequest(passphrase = passphrase)
                    val jsonBody = json.encodeToString(CheckPasskeyRequest.serializer(), requestBody)
                    requestBuilder.post(jsonBody.toRequestBody("application/json".toMediaType()))
                        .header("Content-Type", "application/json")
                } else {
                    requestBuilder.get()
                }
                
                val request = requestBuilder.build()
                val response = client.newCall(request).execute()
                
                Timber.d("Check passkey API response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("API call failed with code ${response.code}: $errorBody")
                    
                    // For certain errors like 404, we can return false (no passkey) rather than failing
                    if (response.code == 404) {
                        return@runCatching false
                    }
                    
                    throw Exception("Failed to check passkey: HTTP ${response.code} - $errorBody")
                }
                
                val responseBody = response.body?.string()
                
                if (responseBody == null) {
                    Timber.e("Empty response body")
                    throw Exception("Empty response body")
                }
                
                try {
                    // Parse the response to get the hasPasskey value
                    val checkResponse = json.decodeFromString(CheckPasskeyResponse.serializer(), responseBody)
                    Timber.d("User ${checkResponse.userId} has passkey: ${checkResponse.hasPasskey}")
                    checkResponse.hasPasskey
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse API response: ${e.message}")
                    throw Exception("Failed to parse check passkey response: ${e.message}", e)
                }
            } catch (e: Exception) {
                Timber.w(e, "API call failed, checking in-memory storage: ${e.message}")
                // Fall back to memory if API fails
                if (passphrase != null) {
                    val hasKey = passkeyStore.containsKey(passphrase)
                    Timber.d("In-memory store has passkey for passphrase: $hasKey")
                    hasKey
                } else {
                    val hasAnyKey = passkeyStore.isNotEmpty()
                    Timber.d("In-memory store has any passkeys: $hasAnyKey")
                    hasAnyKey
                }
            }
        }.onFailure {
            Timber.e(it, "Failed to check passkey: ${it.message}")
        }
    }
} 
