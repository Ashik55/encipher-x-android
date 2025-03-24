/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.securebackup.impl.setup

import androidx.compose.runtime.Composable
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
        val vaultSaveAction = remember { mutableStateOf<AsyncAction<Unit>>(AsyncAction.Uninitialized) }

        fun handleEvents(event: SecureBackupSetupEvents) {
            when (event) {
                SecureBackupSetupEvents.CreateRecoveryKey -> {
                    coroutineScope.createOrChangeRecoveryKey(stateAndDispatch)
                }
                SecureBackupSetupEvents.RecoveryKeyHasBeenSaved ->
                    stateAndDispatch.dispatchAction(SecureBackupSetupStateMachine.Event.UserSavedKey)
                SecureBackupSetupEvents.DismissDialog -> {
                    showSaveConfirmationDialog = false
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
            }
        }

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
}
