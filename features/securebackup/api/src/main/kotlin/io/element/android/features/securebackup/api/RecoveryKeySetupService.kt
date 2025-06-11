/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.api

/**
 * Service to determine when recovery key setup should be triggered for users.
 * This service handles the logic for prompting users to set up recovery keys
 * after login completion, particularly for new users and after identity reset.
 */
interface RecoveryKeySetupService {
    
    /**
     * Determines if recovery key setup should be triggered for the current user.
     * 
     * This method checks:
     * - Whether the user has any existing passkeys via hasPasskey() API
     * - If hasPasskey = false, then recovery key setup should be triggered
     * - Also triggers after successful identity reset
     * 
     * @return true if recovery key setup should be shown, false otherwise
     */
    suspend fun shouldTriggerRecoveryKeySetup(): Boolean
    
    /**
     * Checks if the user has existing passkeys stored.
     * Uses the passkey service to determine if any passkeys are available.
     * 
     * @return true if user has existing passkeys, false otherwise
     */
    suspend fun hasExistingPasskeys(): Boolean
    
    /**
     * Marks that recovery key setup has been handled in the current session.
     * This prevents duplicate prompts within the same session.
     */
    suspend fun markRecoveryKeySetupHandled()
    
    /**
     * Checks if recovery key setup has been handled in the current session.
     * 
     * @return true if setup has been handled, false otherwise
     */
    suspend fun isRecoveryKeySetupHandled(): Boolean
}