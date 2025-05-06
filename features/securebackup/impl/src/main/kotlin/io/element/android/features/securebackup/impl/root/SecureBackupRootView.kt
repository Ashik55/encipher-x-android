/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.securebackup.impl.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.dialogs.ErrorDialog
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.buildAnnotatedStringWithStyledPart
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun SecureBackupRootView(
    state: SecureBackupRootState,
    onBackClick: () -> Unit,
    onSetupClick: () -> Unit,
    onChangeClick: () -> Unit,
    onDisableClick: () -> Unit,
    onConfirmRecoveryKeyClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)

    PreferencePage(
        modifier = modifier,
        onBackClick = onBackClick,
        title = stringResource(id = CommonStrings.common_encryption),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.screen_chat_backup_key_backup_title),
                )
            },
            supportingContent = {
                Text(
                    text = buildAnnotatedStringWithStyledPart(
                        fullTextRes = R.string.screen_chat_backup_key_backup_description,
                        coloredTextRes = CommonStrings.action_learn_more,
                        color = ElementTheme.colors.textPrimary,
                        underline = false,
                        bold = true,
                    ),
                )
            },
//            onClick = onLearnMoreClick,
        )

        // Key storage toggle has been hidden from UI but functionality remains enabled in the background
        // This section silently ensures key storage is always enabled
        when (state.backupState) {
            BackupState.UNKNOWN -> {
                when (state.doesBackupExistOnServer) {
                    is AsyncData.Success -> {
                        if (!state.doesBackupExistOnServer.data) {
                            // Silently enable key storage if it's not enabled
                            state.eventSink.invoke(SecureBackupRootEvents.EnableKeyStorage)
                        }
                    }
                    else -> { /* Do nothing, wait for data */ }
                }
            }
            BackupState.WAITING_FOR_SYNC -> { /* No action needed */ }
            BackupState.CREATING -> { /* No action needed, already being created */ }
            BackupState.ENABLING -> { /* No action needed, already being enabled */ }
            BackupState.RESUMING -> { /* No action needed, already resuming */ }
            BackupState.ENABLED -> { /* No action needed, already enabled */ }
            BackupState.DOWNLOADING -> { /* No action needed */ }
            BackupState.DISABLING -> {
                // If it's being disabled, attempt to re-enable it silently
                state.eventSink.invoke(SecureBackupRootEvents.EnableKeyStorage)
            }
        }

        HorizontalDivider()
        
        // Setup recovery - modernized UI
        when (state.recoveryState) {
            RecoveryState.UNKNOWN,
            RecoveryState.WAITING_FOR_SYNC -> Unit
            RecoveryState.DISABLED -> {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.screen_chat_backup_recovery_action_setup),
                                style = ElementTheme.typography.fontBodyLgMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.screen_chat_backup_recovery_action_setup_description, state.appName),
                                style = ElementTheme.typography.fontBodyMdRegular,
                            )
                        },
                        leadingContent = ListItemContent.Icon(
                            IconSource.Vector(CompoundIcons.Key())
                        ),
                        trailingContent = ListItemContent.Icon(
                            IconSource.Vector(CompoundIcons.ChevronRight())
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElementTheme.colors.bgSubtleSecondary)
                            .clickable(enabled = state.isKeyStorageEnabled) {
                                if (state.isKeyStorageEnabled) {
                                    onSetupClick()
                                } else {
                                    state.eventSink.invoke(SecureBackupRootEvents.DisplayKeyStorageDisabledError)
                                }
                            }
                            .padding(vertical = 4.dp),
                        enabled = state.isKeyStorageEnabled,
                        alwaysClickable = true,
                    )
                }
            }
            RecoveryState.ENABLED -> {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.screen_chat_backup_recovery_action_change),
                                style = ElementTheme.typography.fontBodyLgMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.screen_chat_backup_recovery_action_change_description),
                                style = ElementTheme.typography.fontBodyMdRegular,
                            )
                        },
                        leadingContent = ListItemContent.Icon(
                            IconSource.Vector(CompoundIcons.Edit())
                        ),
                        trailingContent = ListItemContent.Icon(
                            IconSource.Vector(CompoundIcons.ChevronRight())
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElementTheme.colors.bgSubtleSecondary)
                            .clickable(enabled = state.isKeyStorageEnabled) {
                                if (state.isKeyStorageEnabled) {
                                    onChangeClick()
                                } else {
                                    state.eventSink.invoke(SecureBackupRootEvents.DisplayKeyStorageDisabledError)
                                }
                            }
                            .padding(vertical = 4.dp),
                        enabled = state.isKeyStorageEnabled,
                        alwaysClickable = true,
                    )
                }
            }
            RecoveryState.INCOMPLETE ->
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.screen_chat_backup_recovery_action_confirm),
                                style = ElementTheme.typography.fontBodyLgMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.screen_chat_backup_recovery_action_confirm_description),
                                style = ElementTheme.typography.fontBodyMdRegular,
                            )
                        },
                        leadingContent = ListItemContent.Icon(
                            IconSource.Vector(CompoundIcons.CheckCircle())
                        ),
                        trailingContent = ListItemContent.Icon(
                            IconSource.Vector(CompoundIcons.ChevronRight())
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElementTheme.colors.bgSubtleSecondary)
                            .clickable(enabled = state.isKeyStorageEnabled) {
                                if (state.isKeyStorageEnabled) {
                                    onConfirmRecoveryKeyClick()
                                } else {
                                    state.eventSink.invoke(SecureBackupRootEvents.DisplayKeyStorageDisabledError)
                                }
                            }
                            .padding(vertical = 4.dp),
                        enabled = state.isKeyStorageEnabled,
                        alwaysClickable = true,
                    )
                }
        }
    }

    AsyncActionView(
        async = state.enableAction,
        progressDialog = { },
        onSuccess = { },
        onErrorDismiss = { state.eventSink.invoke(SecureBackupRootEvents.DismissDialog) }
    )
    if (state.displayKeyStorageDisabledError) {
        ErrorDialog(
            title = null,
            content = stringResource(id = R.string.screen_chat_backup_key_storage_disabled_error),
            onSubmit = { state.eventSink.invoke(SecureBackupRootEvents.DismissDialog) },
        )
    }
}

@Composable
private fun LoadingView() {
    CircularProgressIndicator(
        modifier = Modifier
            .progressSemantics()
            .size(24.dp),
        strokeWidth = 2.dp
    )
}

@PreviewsDayNight
@Composable
internal fun SecureBackupRootViewPreview(
    @PreviewParameter(SecureBackupRootStateProvider::class) state: SecureBackupRootState
) = ElementPreview {
    SecureBackupRootView(
        state = state,
        onBackClick = {},
        onSetupClick = {},
        onChangeClick = {},
        onDisableClick = {},
        onConfirmRecoveryKeyClick = {},
        onLearnMoreClick = {},
    )
}
