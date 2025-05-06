/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.root

import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.RecoveryState

data class SecureBackupRootState(
    val enableAction: AsyncAction<Unit>,
    val backupState: BackupState,
    val doesBackupExistOnServer: AsyncData<Boolean>,
    val recoveryState: RecoveryState,
    val appName: String,
    val displayKeyStorageDisabledError: Boolean,
    val snackbarMessage: SnackbarMessage?,
    val eventSink: (SecureBackupRootEvents) -> Unit,
) {
    // Always return true to keep key storage enabled
    val isKeyStorageEnabled: Boolean
        get() = true
}
