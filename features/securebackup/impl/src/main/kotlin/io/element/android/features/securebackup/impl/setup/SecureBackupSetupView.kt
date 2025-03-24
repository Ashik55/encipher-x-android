/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.setup

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
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
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import timber.log.Timber
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureBackupSetupView(
    state: SecureBackupSetupState,
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val formattedRecoveryKey = state.recoveryKeyViewState.formattedRecoveryKey
    
    // Track if bottom sheet should be shown (when recovery key is available)
    var showBottomSheet by remember(formattedRecoveryKey) { 
        mutableStateOf(formattedRecoveryKey != null) 
    }
    
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    // Create a new state for the setup phase
    val setupMessage = when {
        state.recoveryKeyViewState.inProgress -> stringResource(id = R.string.screen_recovery_key_generating_key)
        formattedRecoveryKey != null -> stringResource(id = R.string.screen_recovery_key_vault_save_description)
        else -> ""
    }
    
    // Always enable vault mode
    LaunchedEffect(Unit) {
        if (!state.recoveryKeyViewState.isVaultMode) {
            state.eventSink.invoke(SecureBackupSetupEvents.ToggleVaultMode)
        }
    }
    
    // Auto-generate recovery key if not already generated or in progress
    LaunchedEffect(state.recoveryKeyViewState.formattedRecoveryKey, state.recoveryKeyViewState.inProgress) {
        if (!state.recoveryKeyViewState.inProgress && state.recoveryKeyViewState.formattedRecoveryKey == null) {
            state.eventSink.invoke(SecureBackupSetupEvents.CreateRecoveryKey)
        }
    }
    
    // Automatically hide bottom sheet when save is successful
    LaunchedEffect(state.vaultSaveAction) {
        if (state.vaultSaveAction is AsyncAction.Success) {
            // Give a small delay to let the user see the success state
            kotlinx.coroutines.delay(1500)
            showBottomSheet = false
        }
    }
    
    NewFlowStepPage(
        modifier = modifier,
        onBackClick = onBackClick.takeIf { state.canGoBack() },
        title = title(state),
        subTitle = subtitle(state),
        iconStyle = RecoveryKeyIcon.Style.Default(CompoundIcons.KeySolid()),
        buttons = { 
            // Add button to reopen bottom sheet if it's closed and recovery key is available
            if (formattedRecoveryKey != null && !showBottomSheet) {
                Button(
                    text = stringResource(id = R.string.screen_recovery_key_vault_save_button),
                    leadingIcon = IconSource.Vector(CompoundIcons.Key()),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showBottomSheet = true
                    }
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = setupMessage,
                textAlign = TextAlign.Center,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            if (formattedRecoveryKey != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.screen_recovery_key_success),
                    textAlign = TextAlign.Center,
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = Color(0xFF0DBD8B),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
    
    // Show bottom sheet when recovery key is generated
    if (showBottomSheet && formattedRecoveryKey != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
        ) {
            BottomSheetContent(
                state = state,
                onSave = {
                    // We don't close immediately, we'll let the LaunchedEffect do it after success
                }
            )
        }
    }

    // Display AsyncActionView for vault save action
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

@Composable
private fun BottomSheetContent(
    state: SecureBackupSetupState,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track if the save operation has been completed successfully
    var saveSuccessful by remember { mutableStateOf(false) }
    
    // Mark as successful when vault save action is success
    LaunchedEffect(state.vaultSaveAction) {
        if (state.vaultSaveAction is AsyncAction.Success) {
            saveSuccessful = true
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (saveSuccessful) 
                stringResource(id = R.string.screen_recovery_key_vault_success)
            else
                stringResource(id = R.string.screen_recovery_key_vault_save_description),
            textAlign = TextAlign.Center,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = if (saveSuccessful) Color(0xFF0DBD8B) else ElementTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Only show input if not saved successfully yet
        if (!saveSuccessful) {
            // RecoveryKeyView for passphrase input only
            RecoveryKeyView(
                modifier = Modifier.fillMaxWidth(),
                state = state.recoveryKeyViewState,
                onClick = null,
                onChange = null,
                onSubmit = null,
                onPassphraseChange = { state.eventSink(SecureBackupSetupEvents.PassphraseChanged(it)) },
                showRecoveryKey = false // Hide the actual recovery key
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state.recoveryKeyViewState.passphrase.isBlank()) {
                // Add hint to enter passphrase
                Text(
                    text = stringResource(id = R.string.screen_recovery_key_vault_passphrase_required),
                    color = ElementTheme.colors.textCriticalPrimary,
                    style = ElementTheme.typography.fontBodyXsRegular,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Swipable save button
            SwipeToSaveButton(
                enabled = state.recoveryKeyViewState.passphrase.isNotBlank() && !state.isSavingToVault,
                isLoading = state.isSavingToVault,
                isSuccess = saveSuccessful,
                onSave = {
                    state.eventSink.invoke(SecureBackupSetupEvents.SaveToVault)
                }
            )
        } else {
            // Success state
            io.element.android.libraries.designsystem.theme.components.Icon(
                imageVector = CompoundIcons.Check(),
                contentDescription = null,
                tint = Color(0xFF0DBD8B),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )
            
            Text(
                text = stringResource(id = R.string.screen_recovery_key_vault_success),
                textAlign = TextAlign.Center,
                style = ElementTheme.typography.fontBodyMdMedium,
                color = Color(0xFF0DBD8B),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            io.element.android.libraries.designsystem.theme.components.Button(
                text = stringResource(id = CommonStrings.action_done),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SwipeToSaveButton(
    enabled: Boolean,
    isLoading: Boolean,
    isSuccess: Boolean = false,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxWidth = 300.dp
    val height = 56.dp
    val thumbSize = 48.dp
    
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val thumbWidth = with(density) { thumbSize.toPx() }
    val dragThreshold = maxWidthPx - thumbWidth - 16f // Some padding
    
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var completed by remember(isSuccess) { mutableStateOf(isSuccess) }
    
    val isDragged by remember(dragOffset) {
        derivedStateOf { dragOffset > 0f }
    }
    
    val progress by remember(dragOffset, maxWidthPx, isSuccess) {
        derivedStateOf { 
            if (isSuccess) 1f else (dragOffset / dragThreshold).coerceIn(0f, 1f) 
        }
    }
    
    // Create a gradient color that fills from left to right based on progress
    val gradientWidth = (progress * maxWidthPx).coerceIn(0f, maxWidthPx)
    
    val offsetAnimation by animateFloatAsState(
        targetValue = if (completed || isSuccess) dragThreshold else dragOffset,
        animationSpec = tween(300),
        label = "ThumbOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (enabled && !isLoading && !completed && !isSuccess) {
                    Modifier.draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            dragOffset = (dragOffset + delta).coerceIn(0f, dragThreshold)
                            if (dragOffset >= dragThreshold && !completed) {
                                completed = true
                                onSave()
                            }
                        },
                        onDragStopped = {
                            if (!completed) {
                                dragOffset = 0f
                            }
                        }
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    if (enabled || isSuccess) ElementTheme.colors.bgSubtleSecondary
                    else Color(0xFFE3E5E6)
                )
        )
        
        // Animated gradient fill
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(height)
                .background(
                    if (completed || isSuccess) 
                        Color(0xFF0DBD8B)  
                    else 
                        Color(0xFF0086EA)
                )
        )
        
        // Text label always centered
        Text(
            text = if (isSuccess) 
                stringResource(id = R.string.screen_recovery_key_vault_success)
            else 
                stringResource(id = R.string.screen_recovery_key_vault_save_button),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = if (progress > 0.5 || completed || isSuccess) 
                Color(0xFFFFFFFF)
            else 
                Color(0xFF0086EA),
            modifier = Modifier.padding(start = 12.dp)
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetAnimation.roundToInt(), 0) }
                .padding(4.dp)
                .size(thumbSize)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator(
                    color = ElementTheme.colors.iconPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                io.element.android.libraries.designsystem.theme.components.Icon(
                    imageVector = if (completed || isSuccess) CompoundIcons.Check() else CompoundIcons.ChevronRight(),
                    contentDescription = null,
                    tint = if (completed || isSuccess) Color(0xFF0DBD8B) else ElementTheme.colors.iconPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
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
