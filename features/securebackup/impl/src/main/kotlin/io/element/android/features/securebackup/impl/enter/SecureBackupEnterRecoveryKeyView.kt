/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.enter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.securebackup.impl.R
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyView
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.atomic.pages.NewFlowStepPage
import io.element.android.libraries.designsystem.components.RecoveryKeyIcon
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.IconSource
import timber.log.Timber

@Composable
fun SecureBackupEnterRecoveryKeyView(
    state: SecureBackupEnterRecoveryKeyState,
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Handle recovery completion
    AsyncActionView(
        async = state.submitAction,
        onSuccess = { onSuccess() },
        progressDialog = { },
        errorTitle = { stringResource(id = R.string.screen_recovery_key_vault_recovery_failed_title) },
        errorMessage = { stringResource(id = R.string.screen_recovery_key_vault_recovery_failed_content) },
        onErrorDismiss = { state.eventSink(SecureBackupEnterRecoveryKeyEvents.ClearDialog) },
    )
    
    // Handle vault passkey retrieval
    when (val vaultAction = state.retrieveVaultAction) {
        AsyncAction.Uninitialized -> {}
        is AsyncAction.Loading -> {}
        is AsyncAction.Success -> {
            Timber.d("Passkey retrieved successfully from vault")
        }
        is AsyncAction.Failure -> {
            val isNoOpError = vaultAction.error.message?.contains("Vault feature is not available") == true
            val errorTitle = if (isNoOpError) {
                stringResource(id = R.string.screen_recovery_key_vault_unavailable_title)
            } else {
                stringResource(id = R.string.screen_recovery_key_vault_retrieve_error_title)
            }
            val errorMessage = vaultAction.error.message ?: stringResource(id = R.string.screen_recovery_key_vault_retrieve_error)
            
            Timber.e("Displaying error message for passkey retrieval: ${vaultAction.error.message}")
            
            io.element.android.libraries.designsystem.components.dialogs.ErrorDialog(
                title = errorTitle,
                content = errorMessage,
                onSubmit = {
                    Timber.d("Error dialog dismissed, clearing action state")
                    state.eventSink(SecureBackupEnterRecoveryKeyEvents.ClearDialog)
                }
            )
        }
        is AsyncAction.Confirming -> {}
    }

    NewFlowStepPage(
        modifier = modifier,
        isScrollable = true,
        onBackClick = onBackClick,
        iconStyle = RecoveryKeyIcon.Style.Default(CompoundIcons.KeySolid()),
        title = stringResource(id = R.string.screen_recovery_key_vault_retrieve_button),
        subTitle = stringResource(id = R.string.screen_recovery_key_vault_hint),
        buttons = { Buttons(state = state) },
    ) {
        Content(state = state)
    }
}

@Composable
private fun Content(
    state: SecureBackupEnterRecoveryKeyState,
) {
    Column(
        modifier = Modifier.padding(top = 32.dp, bottom = 32.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Only show the passphrase input field, not the recovery key text area
        RecoveryKeyView(
            modifier = Modifier.fillMaxWidth(),
            state = state.recoveryKeyViewState,
            onClick = null,
            onChange = {
                state.eventSink.invoke(SecureBackupEnterRecoveryKeyEvents.OnRecoveryKeyChange(it))
            },
            onSubmit = {
                state.eventSink.invoke(SecureBackupEnterRecoveryKeyEvents.Submit)
            },
            showInputOnly = true
        )
    }
}

@Composable
private fun ColumnScope.Buttons(
    state: SecureBackupEnterRecoveryKeyState,
) {
    Button(
        text = stringResource(id = R.string.screen_recovery_key_vault_retrieve_button),
        leadingIcon = IconSource.Vector(CompoundIcons.Key()),
        enabled = state.isSubmitEnabled,
        showProgress = state.submitAction.isLoading() || state.retrieveVaultAction.isLoading(),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            Timber.d("Retrieve from vault button clicked with passphrase length: ${state.recoveryKeyViewState.formattedRecoveryKey?.length ?: 0}")
            state.eventSink.invoke(SecureBackupEnterRecoveryKeyEvents.Submit)
        }
    )
}

@PreviewsDayNight
@Composable
internal fun SecureBackupEnterRecoveryKeyViewPreview(
    @PreviewParameter(SecureBackupEnterRecoveryKeyStateProvider::class) state: SecureBackupEnterRecoveryKeyState
) = ElementPreview {
    SecureBackupEnterRecoveryKeyView(
        state = state,
        onSuccess = {},
        onBackClick = {},
    )
}
