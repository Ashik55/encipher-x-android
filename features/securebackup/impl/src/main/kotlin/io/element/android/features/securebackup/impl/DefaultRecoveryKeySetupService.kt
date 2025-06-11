/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.features.securebackup.api.RecoveryKeySetupService
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.MatrixClient
import timber.log.Timber
import javax.inject.Inject

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultRecoveryKeySetupService @Inject constructor(
    private val matrixClient: MatrixClient,
) : RecoveryKeySetupService {
    
    private var setupHandledInSession = false
    
    override suspend fun shouldTriggerRecoveryKeySetup(): Boolean {
        if (setupHandledInSession) {
            Timber.d("Recovery key setup already handled in this session")
            return false
        }
        
        try {
            // Always check hasPasskey() API after login as per requirements
            val hasPasskeys = hasExistingPasskeys()
            Timber.d("User has existing passkeys: $hasPasskeys")
            
            // If hasPasskey = false, show recovery key setup page
            return !hasPasskeys
        } catch (e: Exception) {
            Timber.e(e, "Error checking recovery key setup requirement: ${e.message}")
            // Default to not triggering on error to avoid annoying users
            return false
        }
    }
    
    override suspend fun hasExistingPasskeys(): Boolean {
        return try {
            // Check if the user has any passkeys via the passkey service
            val passkeyService = matrixClient.passkeyService()
            val userId = matrixClient.sessionId.value
            
            // Use userId as the identifier to check for passkey existence
            val hasPasskeyResult = passkeyService.hasPasskey(userId)
            
            val hasPasskeys = hasPasskeyResult.getOrDefault(false)
            Timber.d("hasPasskey result for user $userId: $hasPasskeys")
            
            hasPasskeys
        } catch (e: Exception) {
            Timber.w(e, "Error checking for existing passkeys: ${e.message}")
            // Default to assuming they have existing setup (conservative approach)
            true
        }
    }
    
    override suspend fun markRecoveryKeySetupHandled() {
        setupHandledInSession = true
        Timber.d("Marked recovery key setup as handled for this session")
    }
    
    override suspend fun isRecoveryKeySetupHandled(): Boolean {
        return setupHandledInSession
    }
}
