/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.test.encryption

import io.element.android.libraries.matrix.api.encryption.PasskeyService

class FakePasskeyService : PasskeyService {
    private val savePasskeyResponses = mutableMapOf<Pair<String, String>, Result<Unit>>()
    private val retrievePasskeyResponses = mutableMapOf<String, Result<String>>()
    
    private var defaultSaveResponse: Result<Unit> = Result.success(Unit)
    private var defaultRetrieveResponse: Result<String> = Result.success("fake-passkey")
    
    fun givenSavePasskeyResponse(passkey: String, passphrase: String, response: Result<Unit>) {
        savePasskeyResponses[Pair(passkey, passphrase)] = response
    }
    
    fun givenRetrievePasskeyResponse(passphrase: String, response: Result<String>) {
        retrievePasskeyResponses[passphrase] = response
    }
    
    fun givenDefaultSaveResponse(response: Result<Unit>) {
        defaultSaveResponse = response
    }
    
    fun givenDefaultRetrieveResponse(response: Result<String>) {
        defaultRetrieveResponse = response
    }
    
    override suspend fun savePasskey(passkey: String, passphrase: String): Result<Unit> {
        return savePasskeyResponses[Pair(passkey, passphrase)] ?: defaultSaveResponse
    }
    
    override suspend fun retrievePasskey(passphrase: String): Result<String> {
        return retrievePasskeyResponses[passphrase] ?: defaultRetrieveResponse
    }
} 