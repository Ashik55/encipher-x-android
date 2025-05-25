/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.setup

sealed interface SecureBackupSetupEvents {
    data object CreateRecoveryKey : SecureBackupSetupEvents
    data object RecoveryKeyHasBeenSaved : SecureBackupSetupEvents
    data object Done : SecureBackupSetupEvents
    data object DismissDialog : SecureBackupSetupEvents
    
    // New events for vault functionality
    data class PassphraseChanged(val passphrase: String) : SecureBackupSetupEvents
    data object SaveToVault : SecureBackupSetupEvents
    data object ToggleVaultMode : SecureBackupSetupEvents
    
    // New events for passkey validation
    data class OldPassphraseChanged(val passphrase: String) : SecureBackupSetupEvents
    data object ValidatePasskey : SecureBackupSetupEvents
    data object DismissValidationError : SecureBackupSetupEvents
    
    // New events for handling back navigation
    data object ShowPassphraseRequiredDialog : SecureBackupSetupEvents
    data object DismissPassphraseRequiredDialog : SecureBackupSetupEvents
}
