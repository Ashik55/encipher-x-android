/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.securebackup.impl.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.freeletics.flowredux.compose.StateAndDispatch
import com.freeletics.flowredux.compose.rememberStateAndDispatch
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.features.securebackup.impl.loggerTagSetup
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyUserStory
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyViewState
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.encryption.EnableRecoveryProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import timber.log.Timber

class SecureBackupSetupPresenter @AssistedInject constructor(
    @Assisted private val isChangeRecoveryKeyUserStory: Boolean,
    private val stateMachine: SecureBackupSetupStateMachine,
    private val encryptionService: EncryptionService,
    private val matrixClient: MatrixClient,
) : Presenter<SecureBackupSetupState> {
    @AssistedFactory
    fun interface Factory {
        fun create(isChangeRecoveryKeyUserStory: Boolean): SecureBackupSetupPresenter
    }

    @Composable
    override fun present(): SecureBackupSetupState {
        val coroutineScope = rememberCoroutineScope()
        val stateAndDispatch = stateMachine.rememberStateAndDispatch()
        val setupState by remember {
            derivedStateOf { stateAndDispatch.state.value.toSetupState() }
        }
        var showSaveConfirmationDialog by remember { mutableStateOf(false) }
        var isVaultMode by remember { mutableStateOf(false) }
        var passphrase by remember { mutableStateOf("") }
        var oldPassphrase by remember { mutableStateOf("") }
        var needsPasskeyValidation by remember { mutableStateOf(false) }
        var showPassphraseRequiredDialog by remember { mutableStateOf(false) }
        val vaultSaveAction = remember { mutableStateOf<AsyncAction<Unit>>(AsyncAction.Uninitialized) }
        val validatePasskeyAction = remember { mutableStateOf<AsyncAction<Unit>>(AsyncAction.Uninitialized) }
        
        // Check if we need to validate passkeys when initializing in change mode
        LaunchedEffect(Unit) {
            if (isChangeRecoveryKeyUserStory) {
                Timber.tag(loggerTagSetup.value).d("Checking if passkey validation is needed for recovery key change")
                
                try {
                    // Check if a passkey exists for this user
                    val passkeyService = matrixClient.passkeyService()
                    val userId = matrixClient.sessionId.value
                    Timber.tag(loggerTagSetup.value).d("Checking passkey for user: $userId")
                    
                    val hasPasskeyResponse = passkeyService.hasPasskey(userId)
                    Timber.tag(loggerTagSetup.value).d("hasPasskey response: $hasPasskeyResponse")
                    
                    val hasPasskey = hasPasskeyResponse.getOrDefault(false)
                    Timber.tag(loggerTagSetup.value).d("User has passkey: $hasPasskey")
                    
                    // Force set the validation flag for testing
                    needsPasskeyValidation = true
                    
                    // Uncomment this line when done testing
                    // needsPasskeyValidation = hasPasskey
                    
                    Timber.tag(loggerTagSetup.value).d("needsPasskeyValidation set to: $needsPasskeyValidation")
                } catch (e: Exception) {
                    Timber.tag(loggerTagSetup.value).e(e, "Failed to check if passkey exists")
                    // Default to not requiring validation on error
                    needsPasskeyValidation = false
                }
            }
        }

        fun handleEvents(event: SecureBackupSetupEvents) {
            when (event) {
                SecureBackupSetupEvents.CreateRecoveryKey -> {
                    // If we're in change mode and need validation first, don't proceed
                    if (isChangeRecoveryKeyUserStory && needsPasskeyValidation) {
                        Timber.tag(loggerTagSetup.value).d("Skipping key creation - validation required first")
                        // Don't proceed with key generation until validated
                        return
                    }
                    Timber.tag(loggerTagSetup.value).d("Creating recovery key")
                    coroutineScope.createOrChangeRecoveryKey(stateAndDispatch)
                }
                SecureBackupSetupEvents.RecoveryKeyHasBeenSaved ->
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.UserSavedKey)
                SecureBackupSetupEvents.DismissDialog -> {
                    showSaveConfirmationDialog = false
                    validatePasskeyAction.value = AsyncAction.Uninitialized
                }
                SecureBackupSetupEvents.Done -> {
                    showSaveConfirmationDialog = true
                }
                is SecureBackupSetupEvents.PassphraseChanged -> {
                    passphrase = event.passphrase
                }
                SecureBackupSetupEvents.SaveToVault -> {
                    val recoveryKey = setupState.recoveryKey()
                    if (recoveryKey != null && passphrase.isNotBlank()) {
                        coroutineScope.saveToVault(vaultSaveAction, recoveryKey, passphrase)
                    }
                }
                SecureBackupSetupEvents.ToggleVaultMode -> {
                    isVaultMode = !isVaultMode
                }
                is SecureBackupSetupEvents.OldPassphraseChanged -> {
                    oldPassphrase = event.passphrase
                    Timber.tag(loggerTagSetup.value).d("Old passphrase changed: ${oldPassphrase.isNotBlank()}")
                }
                SecureBackupSetupEvents.ValidatePasskey -> {
                    Timber.tag(loggerTagSetup.value).d("Validating passkey with passphrase, len=${oldPassphrase.length}")
                    if (oldPassphrase.isNotBlank()) {
                        coroutineScope.validatePasskey(validatePasskeyAction, oldPassphrase, needsPasskeyValidation) { validated ->
                            Timber.tag(loggerTagSetup.value).d("Passkey validation result: $validated")
                            needsPasskeyValidation = !validated
                            Timber.tag(loggerTagSetup.value).d("needsPasskeyValidation updated to: $needsPasskeyValidation")
                        }
                    }
                }
                SecureBackupSetupEvents.ShowPassphraseRequiredDialog -> {
                    Timber.tag(loggerTagSetup.value).d("Showing passphrase required dialog")
                    showPassphraseRequiredDialog = true
                }
                SecureBackupSetupEvents.DismissPassphraseRequiredDialog -> {
                    Timber.tag(loggerTagSetup.value).d("Dismissing passphrase required dialog")
                    showPassphraseRequiredDialog = false
                }
            }
        }

        // Add extra debug log to check state values
        Timber.tag(loggerTagSetup.value).d("Current state: isChangeRecoveryKeyUserStory=$isChangeRecoveryKeyUserStory, needsPasskeyValidation=$needsPasskeyValidation")
        
        val recoveryKeyViewState = RecoveryKeyViewState(
            recoveryKeyUserStory = if (isChangeRecoveryKeyUserStory) RecoveryKeyUserStory.Change else RecoveryKeyUserStory.Setup,
            formattedRecoveryKey = setupState.recoveryKey(),
            inProgress = setupState is SetupState.Creating,
            passphrase = passphrase,
            isVaultMode = isVaultMode,
        )

        return SecureBackupSetupState(
            isChangeRecoveryKeyUserStory = isChangeRecoveryKeyUserStory,
            recoveryKeyViewState = recoveryKeyViewState,
            setupState = setupState,
            showSaveConfirmationDialog = showSaveConfirmationDialog,
            isSavingToVault = vaultSaveAction.value.isLoading(),
            vaultSaveAction = vaultSaveAction.value,
            needsPasskeyValidation = needsPasskeyValidation,
            validatePasskeyAction = validatePasskeyAction.value,
            oldPassphrase = oldPassphrase,
            showPassphraseRequiredDialog = showPassphraseRequiredDialog,
            eventSink = ::handleEvents
        )
    }

    private fun SecureBackupSetupStateMachine.State?.toSetupState(): SetupState {
        return when (this) {
            null,
            SecureBackupSetupStateMachine.State.Initial -> SetupState.Init
            SecureBackupSetupStateMachine.State.CreatingKey -> SetupState.Creating
            is SecureBackupSetupStateMachine.State.KeyCreated -> SetupState.Created(formattedRecoveryKey = key)
            is SecureBackupSetupStateMachine.State.KeyCreatedAndSaved -> SetupState.CreatedAndSaved(formattedRecoveryKey = key)
        }
    }

    private fun CoroutineScope.createOrChangeRecoveryKey(
        stateAndDispatch: StateAndDispatch<SecureBackupSetupStateMachine.State, SecureBackupSetupStateMachine.Event>
    ) = launch {
        stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.UserCreatesKey)
        if (isChangeRecoveryKeyUserStory) {
            Timber.tag(loggerTagSetup.value).d("Calling encryptionService.resetRecoveryKey()")
            encryptionService.resetRecoveryKey().fold(
                onSuccess = {
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkHasCreatedKey(it))
                },
                onFailure = {
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkError(it))
                }
            )
        } else {
            observeEncryptionService(stateAndDispatch)
            Timber.tag(loggerTagSetup.value).d("Calling encryptionService.enableRecovery()")
            encryptionService.enableRecovery(waitForBackupsToUpload = false)
        }
    }

    private fun CoroutineScope.observeEncryptionService(
        stateAndDispatch: StateAndDispatch<SecureBackupSetupStateMachine.State, SecureBackupSetupStateMachine.Event>
    ) = launch {
        encryptionService.enableRecoveryProgressStateFlow.collect { enableRecoveryProgress ->
            Timber.tag(loggerTagSetup.value).d("New enableRecoveryProgress: ${enableRecoveryProgress.javaClass.simpleName}")
            when (enableRecoveryProgress) {
                is EnableRecoveryProgress.Starting,
                is EnableRecoveryProgress.CreatingBackup,
                is EnableRecoveryProgress.CreatingRecoveryKey,
                is EnableRecoveryProgress.BackingUp,
                is EnableRecoveryProgress.RoomKeyUploadError -> Unit
                is EnableRecoveryProgress.Done ->
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.SdkHasCreatedKey(enableRecoveryProgress.recoveryKey))
            }
        }
    }
    
    private fun CoroutineScope.saveToVault(
        vaultSaveAction: MutableState<AsyncAction<Unit>>,
        recoveryKey: String,
        passphrase: String
    ) = launch {
        vaultSaveAction.value = AsyncAction.Loading
        
        try {
            Timber.tag(loggerTagSetup.value).d("Attempting to save passkey to vault")
            val service = matrixClient.passkeyService()
            Timber.tag(loggerTagSetup.value).d("Using passkey service: ${service.javaClass.simpleName}")
            
            val result = service.savePasskey(
                passkey = recoveryKey,
                passphrase = passphrase
            )
            
            result.fold(
                onSuccess = {
                    Timber.tag(loggerTagSetup.value).d("Successfully saved passkey to vault")
                    vaultSaveAction.value = AsyncAction.Success(Unit)
                },
                onFailure = { error ->
                    Timber.tag(loggerTagSetup.value).e(error, "Failed to save passkey to vault: ${error.message}")
                    vaultSaveAction.value = AsyncAction.Failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag(loggerTagSetup.value).e(e, "Exception while saving passkey to vault: ${e.message}")
            vaultSaveAction.value = AsyncAction.Failure(e)
        }
    }
    
    private fun CoroutineScope.validatePasskey(
        validatePasskeyAction: MutableState<AsyncAction<Unit>>,
        oldPassphrase: String,
        currentValidationState: Boolean,
        onValidated: (Boolean) -> Unit
    ) = launch {
        validatePasskeyAction.value = AsyncAction.Loading
        
        try {
            Timber.tag(loggerTagSetup.value).d("Attempting to validate existing passkey")
            val service = matrixClient.passkeyService()
            
            // Attempt to retrieve the passkey with the provided passphrase
            val result = service.retrievePasskey(oldPassphrase)
            
            result.fold(
                onSuccess = { recoveryKey ->
                    Timber.tag(loggerTagSetup.value).d("Successfully validated passkey with passphrase")
                    // If we get here, the passphrase is valid
                    validatePasskeyAction.value = AsyncAction.Success(Unit)
                    // No longer need validation
                    onValidated(true)
                },
                onFailure = { error ->
                    Timber.tag(loggerTagSetup.value).e(error, "Failed to validate passkey: ${error.message}")
                    validatePasskeyAction.value = AsyncAction.Failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.tag(loggerTagSetup.value).e(e, "Exception while validating passkey: ${e.message}")
            validatePasskeyAction.value = AsyncAction.Failure(e)
        }
    }
}
