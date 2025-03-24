/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.setup

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.securebackup.impl.R
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyView
import io.element.android.libraries.androidutils.system.copyToClipboard
import io.element.android.libraries.androidutils.system.startSharePlainTextIntent
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.atomic.pages.FlowStepPage
import io.element.android.libraries.designsystem.atomic.pages.NewFlowStepPage
import io.element.android.libraries.designsystem.components.RecoveryKeyIcon
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import timber.log.Timber

@Composable
fun SecureBackupSetupView(
    state: SecureBackupSetupState,
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NewFlowStepPage(
        modifier = modifier,
        onBackClick = onBackClick.takeIf { state.canGoBack() },
        title = title(state),
        subTitle = subtitle(state),
        iconStyle = RecoveryKeyIcon.Style.Default(CompoundIcons.KeySolid()),
        buttons = { Buttons(state, onFinish = onSuccess) },
    ) {
        Content(state = state)
    }

    // Display AsyncActionView for vault save action
    val context = LocalContext.current
    AsyncActionView(
        async = state.vaultSaveAction,
        onSuccess = {
            showSnackbar(context, R.string.screen_recovery_key_vault_success)
            state.eventSink.invoke(SecureBackupSetupEvents.RecoveryKeyHasBeenSaved)
            // Auto-finish after successful vault save
            onSuccess()
        },
        onErrorDismiss = {
            showSnackbar(context, R.string.screen_recovery_key_vault_error)
        }
    )
}

private fun SecureBackupSetupState.canGoBack(): Boolean {
    return recoveryKeyViewState.formattedRecoveryKey == null
}

@Composable
private fun title(state: SecureBackupSetupState): String {
    return when (state.setupState) {
        SetupState.Init,
        SetupState.Creating -> if (state.isChangeRecoveryKeyUserStory) {
            stringResource(id = R.string.screen_recovery_key_change_title)
        } else {
            stringResource(id = R.string.screen_recovery_key_setup_title)
        }
        is SetupState.Created,
        is SetupState.CreatedAndSaved ->
            stringResource(id = R.string.screen_recovery_key_vault_save_button)
    }
}

@Composable
private fun subtitle(state: SecureBackupSetupState): String {
    return when (state.setupState) {
        SetupState.Init,
        SetupState.Creating -> if (state.isChangeRecoveryKeyUserStory) {
            stringResource(id = R.string.screen_recovery_key_change_description)
        } else {
            stringResource(id = R.string.screen_recovery_key_setup_description)
        }
        is SetupState.Created,
        is SetupState.CreatedAndSaved ->
            stringResource(id = R.string.screen_recovery_key_vault_save_description)
    }
}

@Composable
private fun Content(
    state: SecureBackupSetupState,
) {
    val context = LocalContext.current
    val formattedRecoveryKey = state.recoveryKeyViewState.formattedRecoveryKey
    
    // Always show vault mode if not already in it
    if (!state.recoveryKeyViewState.isVaultMode) {
        LaunchedEffect(Unit) {
            state.eventSink.invoke(SecureBackupSetupEvents.ToggleVaultMode)
        }
    }
    
    // If we don't have a recovery key yet and we're not creating one, start creating it
    if (!state.recoveryKeyViewState.inProgress && formattedRecoveryKey == null) {
        LaunchedEffect(Unit) {
            state.eventSink.invoke(SecureBackupSetupEvents.CreateRecoveryKey)
        }
    }
    
    // Now the recovery key is hidden and only shown in the background
    // Only show the passphrase input UI
    RecoveryKeyView(
        modifier = Modifier.padding(top = 52.dp),
        state = state.recoveryKeyViewState,
        onClick = null,
        onChange = null,
        onSubmit = null,
        onPassphraseChange = { state.eventSink(SecureBackupSetupEvents.PassphraseChanged(it)) },
        showRecoveryKey = false, // Hide the actual recovery key
    )
}

@Composable
private fun ColumnScope.Buttons(
    state: SecureBackupSetupState,
    onFinish: () -> Unit,
) {
    // Only show Save to Vault button when we have a recovery key
    if (state.recoveryKeyViewState.formattedRecoveryKey != null) {
        // Add a note about the vault storage
        Text(
            text = stringResource(id = R.string.screen_recovery_key_vault_save_description),
            color = ElementTheme.colors.textSecondary,
            style = ElementTheme.typography.fontBodySmRegular,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Show vault save button with required passphrase
        Button(
            text = stringResource(id = R.string.screen_recovery_key_vault_save_button),
            leadingIcon = IconSource.Vector(CompoundIcons.Key()),
            modifier = Modifier.fillMaxWidth(),
            enabled = state.recoveryKeyViewState.passphrase.isNotBlank() && !state.isSavingToVault,
            showProgress = state.isSavingToVault,
            onClick = {
                state.eventSink.invoke(SecureBackupSetupEvents.SaveToVault)
            }
        )
        
        if (state.recoveryKeyViewState.passphrase.isBlank()) {
            // Add hint to enter passphrase
            Text(
                text = stringResource(id = R.string.screen_recovery_key_vault_passphrase_required),
                color = ElementTheme.colors.textCriticalPrimary,
                style = ElementTheme.typography.fontBodyXsRegular,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun showSnackbar(context: Context, messageResId: Int) {
    Toast.makeText(
        context,
        context.getString(messageResId),
        Toast.LENGTH_SHORT
    ).show()
}

@PreviewsDayNight
@Composable
internal fun SecureBackupSetupViewPreview(
    @PreviewParameter(SecureBackupSetupStateProvider::class) state: SecureBackupSetupState
) = ElementPreview {
    SecureBackupSetupView(
        state = state,
        onSuccess = {},
        onBackClick = {},
    )
}
