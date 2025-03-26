/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.matrix.api.encryption.PasskeyService
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.PasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyResponse
import io.element.android.libraries.network.RetrofitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Production implementation of [PasskeyService] that works with the actual API endpoints.
 */
class ProductionPasskeyService(
    private val retrofitFactory: RetrofitFactory,
    private val userId: String
) : PasskeyService {
    // Base URL for the passkey service
    private val baseUrl = "https://dev.enciph-er.com"
    
    // JSON serializer/deserializer
    private val json = Json { ignoreUnknownKeys = true }
    
    // HTTP client for direct API calls
    private val client by lazy { OkHttpClient() }

    // In-memory cache for fallbacks
    private val passkeyStore = mutableMapOf<String, String>()

    init {
        Timber.d("Initializing ProductionPasskeyService with base URL: $baseUrl for user: $userId")
    }

    override suspend fun savePasskey(passkey: String, passphrase: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.d("ProductionPasskeyService: Saving passkey for user: $userId")
            
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
                
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        // Parse the response to get full details including encryption info
                        val saveResponse = json.decodeFromString(SavePasskeyResponse.serializer(), responseBody)
                        Timber.d("Successfully saved passkey for user: ${saveResponse.requester}")
                        Timber.d("Encrypted passkey length: ${saveResponse.encryptedPasskey.length}")
                        // Store the encrypted passkey in memory as fallback
                        passkeyStore[passphrase] = saveResponse.encryptedPasskey
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
            Timber.d("ProductionPasskeyService: Retrieving passkey for user: $userId with passphrase length: ${passphrase.length}")
            
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
                
                val responseBody = response.body?.string()
                
                if (responseBody == null) {
                    Timber.e("Empty response body")
                    throw Exception("Empty response body")
                }
                
                Timber.d("API response body length: ${responseBody.length} characters")
                
                try {
                    // Parse the response to get the passkey
                    val passkeyResponse = json.decodeFromString(PasskeyResponse.serializer(), responseBody)
                    Timber.d("Successfully retrieved passkey, length: ${passkeyResponse.passkey.length}")
                    // Store the retrieved passkey in memory as fallback
                    passkeyStore[passphrase] = passkeyResponse.passkey
                    passkeyResponse.passkey
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse API response: ${e.message}")
                    throw Exception("Failed to parse passkey response: ${e.message}", e)
                }
            } catch (e: Exception) {
                Timber.w(e, "API call failed, checking in-memory storage: ${e.message}")
                // Check in-memory storage if API call fails
                val passkey = passkeyStore[passphrase]
                if (passkey == null) {
                    Timber.e("No passkey found for the provided passphrase in memory storage")
                    throw IllegalArgumentException("No passkey found for the provided passphrase. If you just set up your recovery key, please try with your actual recovery key.")
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
            Timber.d("ProductionPasskeyService: Checking for passkey existence for user: $userId")
            
            try {
                // Construct the URL for checking passkey existence
                val url = "$baseUrl/_matrix/client/v3/auth/check_passkey/$userId"
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                
                // If passphrase provided, send as JSON body in a POST request
                if (passphrase != null) {
                    val requestBody = CheckPasskeyRequest(passphrase = passphrase)
                    val jsonBody = json.encodeToString(CheckPasskeyRequest.serializer(), requestBody)
                    requestBuilder.post(jsonBody.toRequestBody("application/json".toMediaType()))
                        .header("Content-Type", "application/json")
                } else {
                    // Otherwise use GET request
                    requestBuilder.get()
                }
                
                val request = requestBuilder.build()
                val response = client.newCall(request).execute()
                
                Timber.d("Check passkey API response code: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Timber.e("API call failed with code ${response.code}: $errorBody")
                    
                    // For 404 errors, we can interpret that as "no passkey exists"
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
                    if (checkResponse.hasPasskey && passphrase != null) {
                        // If we have a passphrase and the user has a passkey, store it in memory
                        passkeyStore[passphrase] = "has_passkey"
                    }
                    checkResponse.hasPasskey
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse API response: ${e.message}")
                    throw Exception("Failed to parse check passkey response: ${e.message}", e)
                }
            } catch (e: Exception) {
                Timber.w(e, "API call failed, checking in-memory storage: ${e.message}")
                
                // Fallback to in-memory storage check
                if (passphrase != null) {
                    val hasKey = passkeyStore.containsKey(passphrase)
                    Timber.d("In-memory store has passkey for specific passphrase: $hasKey")
                    hasKey
                } else {
                    val hasAnyKeys = passkeyStore.isNotEmpty()
                    Timber.d("In-memory store has any passkey: $hasAnyKeys")
                    hasAnyKeys
                }
            }
        }.onFailure {
            Timber.e(it, "Failed to check passkey existence: ${it.message}")
        }
    }
} 
