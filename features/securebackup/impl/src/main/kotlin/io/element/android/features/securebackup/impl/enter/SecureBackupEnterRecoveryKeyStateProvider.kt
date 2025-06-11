/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.enter

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyUserStory
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyViewState
import io.element.android.libraries.architecture.AsyncAction

class SecureBackupEnterRecoveryKeyStateProvider : PreviewParameterProvider<SecureBackupEnterRecoveryKeyState> {
    override val values: Sequence<SecureBackupEnterRecoveryKeyState>
        get() = sequenceOf(
            aSecureBackupEnterRecoveryKeyState(),
            aSecureBackupEnterRecoveryKeyState(
                recoveryKeyViewState = RecoveryKeyViewState(
                    recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
                    formattedRecoveryKey = "1234 5678 90ab cdef ghij klmn opqr stuv wxyz ABCD EFGH IJKL",
                    inProgress = false,
                )
            ),
            aSecureBackupEnterRecoveryKeyState(
                submitAction = AsyncAction.Loading,
                recoveryKeyViewState = RecoveryKeyViewState(
                    recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
                    formattedRecoveryKey = "1234 5678 90ab cdef ghij klmn opqr stuv wxyz ABCD EFGH IJKL",
                    inProgress = true,
                ),
                isSubmitEnabled = false,
            ),
            // Vault mode states
            aSecureBackupEnterRecoveryKeyState(
                isVaultMode = true,
                recoveryKeyViewState = RecoveryKeyViewState(
                    recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
                    formattedRecoveryKey = "my-passphrase",
                    inProgress = false,
                    isVaultMode = true,
                ),
            ),
            aSecureBackupEnterRecoveryKeyState(
                isVaultMode = true,
                retrieveVaultAction = AsyncAction.Loading,
                recoveryKeyViewState = RecoveryKeyViewState(
                    recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
                    formattedRecoveryKey = "my-passphrase",
                    inProgress = true,
                    isVaultMode = true,
                ),
                isSubmitEnabled = false,
            ),
        )
}

fun aSecureBackupEnterRecoveryKeyState(
    recoveryKeyViewState: RecoveryKeyViewState = RecoveryKeyViewState(
        recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
        formattedRecoveryKey = "",
        inProgress = false,
    ),
    submitAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
    retrieveVaultAction: AsyncAction<String> = AsyncAction.Uninitialized,
    isVaultMode: Boolean = false,
    isSubmitEnabled: Boolean = true,
    eventSink: (SecureBackupEnterRecoveryKeyEvents) -> Unit = {},
): SecureBackupEnterRecoveryKeyState {
    return SecureBackupEnterRecoveryKeyState(
        recoveryKeyViewState = recoveryKeyViewState,
        submitAction = submitAction,
        retrieveVaultAction = retrieveVaultAction,
        isVaultMode = isVaultMode,
        isSubmitEnabled = isSubmitEnabled,
        eventSink = eventSink
    )
}
