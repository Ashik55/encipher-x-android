/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files i    // Display passphrase validation UI when in change mode and validation needed
    if (state.isChangeRecoveryKeyUserStory && state.needsPasskeyValidation) {
        PassphraseValidationView(
            state = state,
            onBackClick = handleBackClick,
            modifier = modifier
        )
    } else {
        // Regular recovery key setup/change flow
        NewFlowStepPage(
            modifier = modifier,
            onBackClick = handleBackClick,tory root for full details.
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
import androidx.activity.compose.BackHandler

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
    
    // Handle back navigation - check if passphrase is required
    val handleBackClick = {
        val recoveryKeyGenerated = formattedRecoveryKey != null
        val passphraseEntered = state.recoveryKeyViewState.passphrase.isNotBlank()
        val isInValidationMode = state.isChangeRecoveryKeyUserStory && state.needsPasskeyValidation
        
        Timber.d("Back click - recoveryKeyGenerated: $recoveryKeyGenerated, passphraseEntered: $passphraseEntered, isInValidationMode: $isInValidationMode")
        
        // Show dialog if recovery key is generated but passphrase not set (and not in validation mode)
        if (recoveryKeyGenerated && !passphraseEntered && !isInValidationMode) {
            Timber.d("Showing passphrase required dialog")
            state.eventSink.invoke(SecureBackupSetupEvents.ShowPassphraseRequiredDialog)
        } else {
            Timber.d("Proceeding with back navigation")
            onBackClick()
        }
    }
    
    // Track if bottom sheet should be shown (when recovery key is available)
    var showBottomSheet by remember { mutableStateOf(false) }
    
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
    
    // If in change mode and validation needed, don't auto-generate key
    val shouldAutoGenerateKey = !(state.isChangeRecoveryKeyUserStory && state.needsPasskeyValidation)
    
    // Auto-generate recovery key if not already generated or in progress
    LaunchedEffect(state.recoveryKeyViewState.formattedRecoveryKey, state.recoveryKeyViewState.inProgress) {
        if (shouldAutoGenerateKey && !state.recoveryKeyViewState.inProgress && state.recoveryKeyViewState.formattedRecoveryKey == null) {
            state.eventSink.invoke(SecureBackupSetupEvents.CreateRecoveryKey)
        }
    }
    
    // Force show bottom sheet when recovery key is available - BUT only if validation is not needed OR already completed
    LaunchedEffect(formattedRecoveryKey, state.needsPasskeyValidation) {
        if (formattedRecoveryKey != null) {
            Timber.d("Recovery key available, checking if we should show bottom sheet")
            // Only show bottom sheet if we don't need validation or validation is done
            val shouldShowSheet = !state.isChangeRecoveryKeyUserStory || !state.needsPasskeyValidation
            Timber.d("Should show bottom sheet: $shouldShowSheet (isChange=${state.isChangeRecoveryKeyUserStory}, needsValidation=${state.needsPasskeyValidation})")
            showBottomSheet = shouldShowSheet
        }
    }
    
    // Automatically hide bottom sheet and proceed when save is successful
    LaunchedEffect(state.vaultSaveAction) {
        if (state.vaultSaveAction is AsyncAction.Success) {
            // Give a small delay to let the user see the success state
            kotlinx.coroutines.delay(1500)
            showBottomSheet = false
            // Automatically proceed to next step after successful vault save
            onSuccess()
        }
    }
    
    // Display passphrase validation UI when in change mode and validation needed
    if (state.isChangeRecoveryKeyUserStory && state.needsPasskeyValidation) {
        PassphraseValidationView(
            state = state,
            onBackClick = handleBackClick,
            modifier = modifier
        )
    } else {
        // Add BackHandler to intercept system back gesture without showing back icon
        BackHandler {
            handleBackClick()
        }
        
        // Regular recovery key setup/change flow
        NewFlowStepPage(
            modifier = modifier,
            onBackClick = null, // Hide back icon but handle system back gesture above
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
                            Timber.d("Button clicked to show bottom sheet")
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
                        color = Color(0xFF0A8741),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
    
    // Show bottom sheet when recovery key is generated and validation is not needed or is completed
    if (formattedRecoveryKey != null && (!state.isChangeRecoveryKeyUserStory || !state.needsPasskeyValidation)) {
        Timber.d("Attempting to show bottom sheet, showBottomSheet: $showBottomSheet")
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    Timber.d("Bottom sheet dismissed")
                    showBottomSheet = false 
                },
                sheetState = bottomSheetState,
            ) {
                BottomSheetContent(
                    state = state,
                    onSave = {
                        // This will be handled automatically by LaunchedEffect when vault save succeeds
                        // Just mark the key as saved if manually triggered
                        if (state.vaultSaveAction is AsyncAction.Success) {
                            state.eventSink.invoke(SecureBackupSetupEvents.RecoveryKeyHasBeenSaved)
                            showBottomSheet = false
                            onSuccess()
                        }
                    }
                )
            }
        }
    }

    // Display AsyncActionView for vault save action
    AsyncActionView(
        async = state.vaultSaveAction,
        onSuccess = {
            // Mark recovery key as saved in the state machine
            state.eventSink.invoke(SecureBackupSetupEvents.RecoveryKeyHasBeenSaved)
            // Note: Automatic progression to next step is handled by LaunchedEffect above
        },
        onErrorDismiss = {}
    )
    
    // Display AsyncActionView for passkey validation action
    AsyncActionView(
        async = state.validatePasskeyAction,
        onSuccess = {
            // No action needed here as the validation state is already updated
        },
        onErrorDismiss = {
            state.eventSink(SecureBackupSetupEvents.DismissValidationError)
        }
    )
    
    // Show passphrase required dialog when user tries to leave without setting passphrase
    if (state.showPassphraseRequiredDialog) {
        ConfirmationDialog(
            title = "Must setup passphrase",
            content = "You must set up a passphrase before leaving this screen to secure your recovery key.",
            submitText = "OK",
            onSubmitClick = {
                state.eventSink.invoke(SecureBackupSetupEvents.DismissPassphraseRequiredDialog)
            },
            onDismiss = {
                state.eventSink.invoke(SecureBackupSetupEvents.DismissPassphraseRequiredDialog)
            }
        )
    }
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
            color = if (saveSuccessful) Color(0xFF0A8741) else ElementTheme.colors.textPrimary,
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
            
            // Swipable save button - now disabled until passphrase is entered
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
                tint = Color(0xFF0A8741),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp)
            )
            
//            Text(
//                text = stringResource(id = R.string.screen_recovery_key_vault_success),
//                textAlign = TextAlign.Center,
//                style = ElementTheme.typography.fontBodyMdMedium,
//                color = Color(0xFF0A8741),
//            )
//
            Spacer(modifier = Modifier.height(24.dp))
            
            io.element.android.libraries.designsystem.theme.components.Button(
                text = stringResource(id = CommonStrings.action_done),
                onClick = {
                    // Trigger immediate progression when user clicks Done after successful vault save
                    onSave()
                },
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
    
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val dragThreshold = maxWidthPx - 48f // Some padding
    
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
    
    // Attractive colors
    val primaryColor = Color(0xFF0A8741) // Vibrant blue
    val successColor = Color(0xFF0A8741) // Success green
    val disabledColor = Color(0xFFEAEAEA) // Light gray for disabled state
    val backgroundColor = Color(0xFFFCFCFC) // Pure white background for better contrast
    
    // Button background container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .background(if (!enabled) disabledColor else backgroundColor)
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
        // Left-to-right progress fill with alignment to start
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(height)
                .background(if (completed || isSuccess) successColor else primaryColor)
                .align(Alignment.CenterStart)
        )
        
        // Button content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main button text
            Text(
                text = if (isSuccess) 
                    stringResource(id = R.string.screen_recovery_key_vault_success)
                else if (!enabled)
                    stringResource(id = R.string.screen_recovery_key_vault_passphrase_required)
                else 
                    stringResource(id = R.string.screen_recovery_key_vault_swipe_hint),
                style = ElementTheme.typography.fontBodyLgMedium,
                textAlign = TextAlign.Center,
                color = if (progress > 0.5 || completed || isSuccess) 
                    Color(0xFFFFFFFF) // White text on colored background
                else if (!enabled)
                    Color(0xFF8C8C8C) // Gray for disabled state
                else 
                    Color(0xFF3D3D3D), // Dark text on light background
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            // Hint text when enabled and not in success state
//            if (enabled && !isSuccess && !completed) {
//                Text(
//                    text = stringResource(id = R.string.screen_recovery_key_vault_swipe_hint),
//                    style = ElementTheme.typography.fontBodyXsRegular,
//                    textAlign = TextAlign.Center,
//                    color = if (progress > 0.5)
//                        Color(0xDDFFFFFF)
//                    else
//                        Color(0xFF8C8C8C),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(top = 2.dp)
//                )
//            }
        }
        
        // Visual indicator for arrow that moves with the swipe
        if (enabled && !completed && !isSuccess && !isLoading) {

            // Add left-most indicating arrow to show starting point
            Icon(
                imageVector = CompoundIcons.ArrowRight(),
                contentDescription = null,
                tint = if (progress > 0.1) Color(0x99FFFFFF) else Color(0xFF0A8741),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(16.dp)
            )
        }
        
        // Visual indicators for button states
        when {
            isLoading -> {
                // Loading spinner
                CircularProgressIndicator(
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }
            completed || isSuccess -> {
                // Success check mark
                Icon(
                    imageVector = CompoundIcons.Check(),
                    contentDescription = null,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(20.dp)
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

@Composable
private fun PassphraseValidationView(
    state: SecureBackupSetupState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NewFlowStepPage(
        modifier = modifier,
        onBackClick = onBackClick,
        title = stringResource(id = R.string.screen_recovery_key_change_title),
        subTitle = "Please verify your passphrase to change your recovery key",
        iconStyle = RecoveryKeyIcon.Style.Default(CompoundIcons.KeySolid()),
        buttons = {
            Button(
                text = "Continue",
                enabled = state.oldPassphrase.isNotBlank() && !state.validatePasskeyAction.isLoading(),
                onClick = {
                    state.eventSink(SecureBackupSetupEvents.ValidatePasskey)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Create a simplified version of input field
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Current Passphrase",
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = ElementTheme.colors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // A simple password field with proper parameters
                io.element.android.libraries.designsystem.theme.components.TextField(
                    value = state.oldPassphrase,
                    onValueChange = { value -> state.eventSink(SecureBackupSetupEvents.OldPassphraseChanged(value)) },
                    placeholder = "Enter your current passphrase",
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                    )
                )
            }
            
            // Show loading state
            if (state.validatePasskeyAction.isLoading()) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = Color(0xFF0A8741),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Validating passphrase...",
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = ElementTheme.colors.textSecondary
                )
            }
        }
    }
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
