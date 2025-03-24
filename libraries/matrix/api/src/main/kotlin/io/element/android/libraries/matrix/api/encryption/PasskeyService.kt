/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.encryption

/**
 * Service to handle passkey operations for secure backup recovery keys.
 */
interface PasskeyService {
    /**
     * Saves a passkey (recovery key) with the associated passphrase to the server.
     * @param passkey The recovery key to save
     * @param passphrase The user-provided passphrase to associate with the key
     * @return Result of the operation
     */
    suspend fun savePasskey(passkey: String, passphrase: String): Result<Unit>

    /**
     * Retrieves a passkey (recovery key) using the provided passphrase.
     * @param passphrase The user-provided passphrase to retrieve the associated key
     * @return Result containing the passkey if successful
     */
    suspend fun retrievePasskey(passphrase: String): Result<String>
    
    /**
     * Checks if the user has a passkey stored in the vault.
     * @param passphrase Optional passphrase to check for a specific entry
     * @return Result containing true if the user has a passkey, false otherwise
     */
    suspend fun hasPasskey(passphrase: String? = null): Result<Boolean>
} 