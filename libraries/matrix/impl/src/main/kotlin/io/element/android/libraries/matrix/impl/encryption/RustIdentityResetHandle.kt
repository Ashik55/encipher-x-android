/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.encryption.IdentityOidcResetHandle
import io.element.android.libraries.matrix.api.encryption.IdentityPasswordResetHandle
import io.element.android.libraries.matrix.api.encryption.IdentityResetHandle
import io.element.android.libraries.matrix.impl.encryption.services.PasskeyApiService
import org.matrix.rustcomponents.sdk.AuthData
import org.matrix.rustcomponents.sdk.AuthDataPasswordDetails
import org.matrix.rustcomponents.sdk.CrossSigningResetAuthType
import timber.log.Timber

object RustIdentityResetHandleFactory {
    fun create(
        userId: UserId,
        identityResetHandle: org.matrix.rustcomponents.sdk.IdentityResetHandle?,
        passkeyApiService: PasskeyApiService
    ): Result<IdentityResetHandle?> {
        return runCatching {
            identityResetHandle?.let {
                when (val authType = identityResetHandle.authType()) {
                    is CrossSigningResetAuthType.Oidc -> RustOidcIdentityResetHandle(identityResetHandle, authType.info.approvalUrl)
                    // User interactive authentication (user + password)
                    CrossSigningResetAuthType.Uiaa -> RustPasswordIdentityResetHandle(userId, identityResetHandle, passkeyApiService)
                }
            }
        }
    }
}

class RustPasswordIdentityResetHandle(
    private val userId: UserId,
    private val identityResetHandle: org.matrix.rustcomponents.sdk.IdentityResetHandle,
    private val passkeyApiService: PasskeyApiService
) : IdentityPasswordResetHandle {
    override suspend fun resetPassword(password: String): Result<Unit> {
        return runCatching { 
            // First call the reset passkey API
            val resetResponse = passkeyApiService.resetPasskey(userId.value)
            if (!resetResponse.isSuccessful) {
                Timber.e("Failed to reset passkey: ${resetResponse.code()}")
                throw Exception("Failed to reset passkey: ${resetResponse.code()}")
            }
            
            // Then perform the identity reset
            identityResetHandle.reset(AuthData.Password(AuthDataPasswordDetails(userId.value, password)))
        }
    }

    override suspend fun cancel() {
        identityResetHandle.cancelAndDestroy()
    }
}

class RustOidcIdentityResetHandle(
    private val identityResetHandle: org.matrix.rustcomponents.sdk.IdentityResetHandle,
    override val url: String,
) : IdentityOidcResetHandle {
    override suspend fun resetOidc(): Result<Unit> {
        return runCatching { identityResetHandle.reset(null) }
    }

    override suspend fun cancel() {
        identityResetHandle.cancelAndDestroy()
    }
}

private suspend fun org.matrix.rustcomponents.sdk.IdentityResetHandle.cancelAndDestroy() {
    cancel()
    destroy()
}
