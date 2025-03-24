/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.enter

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
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
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import timber.log.Timber

@Composable
fun SecureBackupEnterRecoveryKeyView(
    state: SecureBackupEnterRecoveryKeyState,
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    
    // Always enable vault mode if not already enabled
    if (!state.isVaultMode) {
        LaunchedEffect(Unit) {
            state.eventSink.invoke(SecureBackupEnterRecoveryKeyEvents.ToggleVaultMode)
        }
    }
    
    // Handle recovery completion
    AsyncActionView(
        async = state.submitAction,
        onSuccess = { onSuccess() },
        progressDialog = { },
        errorTitle = { stringResource(id = R.string.screen_recovery_key_confirm_error_title) },
        errorMessage = { stringResource(id = R.string.screen_recovery_key_confirm_error_content) },
        onErrorDismiss = { state.eventSink(SecureBackupEnterRecoveryKeyEvents.ClearDialog) },
    )
    
    // Handle vault passkey retrieval
    AsyncActionView(
        async = state.retrieveVaultAction,
        onSuccess = {
            Timber.d("Passkey retrieved successfully from vault")
            Toast.makeText(
                context,
                context.getString(R.string.screen_recovery_key_vault_retrieve_success),
                Toast.LENGTH_SHORT
            ).show()
        },
        errorTitle = { 
            val isNoOpError = (state.retrieveVaultAction as? AsyncAction.Failure)?.error?.message?.contains("Vault feature is not available") == true
            if (isNoOpError) {
                stringResource(id = R.string.screen_recovery_key_vault_unavailable_title)
            } else {
                stringResource(id = R.string.screen_recovery_key_vault_retrieve_error_title)
            }
        },
        errorMessage = { error -> 
            Timber.e("Displaying error message for passkey retrieval: ${error?.message}")
            error?.message ?: stringResource(id = R.string.screen_recovery_key_vault_retrieve_error)
        },
        onErrorDismiss = { 
            Timber.d("Error dialog dismissed, clearing action state")
            state.eventSink(SecureBackupEnterRecoveryKeyEvents.ClearDialog) 
        },
    )

    NewFlowStepPage(
        modifier = modifier,
        isScrollable = true,
        onBackClick = onBackClick,
        iconStyle = RecoveryKeyIcon.Style.Default(CompoundIcons.KeySolid()),
        title = stringResource(id = R.string.screen_recovery_key_vault_retrieve_button),
        subTitle = stringResource(id = R.string.screen_recovery_key_vault_or_direct_hint),
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
        modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        )
        
        // Show a hint about direct key entry
        if (state.retrieveVaultAction is AsyncAction.Failure || state.recoveryKeyViewState.formattedRecoveryKey?.contains(" ") == true) {
            Text(
                text = stringResource(id = R.string.screen_recovery_key_vault_need_direct_key),
                color = ElementTheme.colors.textSecondary,
                style = ElementTheme.typography.fontBodyXsRegular,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.Buttons(
    state: SecureBackupEnterRecoveryKeyState,
) {
    Button(
        text = stringResource(
            id = if (state.recoveryKeyViewState.formattedRecoveryKey?.contains(" ") == true) {
                R.string.screen_recovery_key_use_recovery_key_button
            } else {
                R.string.screen_recovery_key_vault_retrieve_button
            }
        ),
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
