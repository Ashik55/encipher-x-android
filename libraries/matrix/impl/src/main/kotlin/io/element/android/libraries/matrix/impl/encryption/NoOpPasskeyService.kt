/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.matrix.api.encryption.PasskeyService
import timber.log.Timber
import javax.inject.Inject

/**
 * No-op implementation of [PasskeyService] used at app scope.
 * This implementation provides a fallback when no session is available.
 */
class NoOpPasskeyService @Inject constructor() : PasskeyService {
    init {
        Timber.d("Initializing NoOpPasskeyService")
    }
    
    override suspend fun savePasskey(passkey: String, passphrase: String): Result<Unit> {
        Timber.d("NoOp: Saving passkey (app scope implementation)")
        return Result.success(Unit)
    }

    override suspend fun retrievePasskey(passphrase: String): Result<String> {
        Timber.d("NoOp: Retrieving passkey (app scope implementation)")
        return Result.failure(UnsupportedOperationException("NoOp implementation cannot retrieve passkeys"))
    }
    
    override suspend fun hasPasskey(passphrase: String): Result<Boolean> {
        Timber.d("NoOp: Checking for passkey existence (app scope implementation)")
        return Result.success(false) // Always return false for NoOp implementation
    }
} 