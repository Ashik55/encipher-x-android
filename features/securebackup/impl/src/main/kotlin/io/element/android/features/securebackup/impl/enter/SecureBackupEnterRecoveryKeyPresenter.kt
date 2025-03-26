/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.enter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyUserStory
import io.element.android.features.securebackup.impl.setup.views.RecoveryKeyViewState
import io.element.android.features.securebackup.impl.tools.RecoveryKeyTools
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.architecture.runCatchingUpdatingState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SecureBackupEnterRecoveryKeyPresenter @Inject constructor(
    private val encryptionService: EncryptionService,
    private val recoveryKeyTools: RecoveryKeyTools,
    private val matrixClient: MatrixClient,
) : Presenter<SecureBackupEnterRecoveryKeyState> {
    @Composable
    override fun present(): SecureBackupEnterRecoveryKeyState {
        val coroutineScope = rememberCoroutineScope()
        var recoveryKey by rememberSaveable { mutableStateOf("") }
        // Always use vault mode
        val isVaultMode = true
        val submitAction: MutableState<AsyncAction<Unit>> = remember { mutableStateOf(AsyncAction.Uninitialized) }
        val retrieveVaultAction: MutableState<AsyncAction<String>> = remember { mutableStateOf(AsyncAction.Uninitialized) }

        fun handleEvents(event: SecureBackupEnterRecoveryKeyEvents) {
            when (event) {
                is SecureBackupEnterRecoveryKeyEvents.OnRecoveryKeyChange -> {
                    recoveryKey = event.value
                }
                SecureBackupEnterRecoveryKeyEvents.Submit -> {
                    coroutineScope.retrievePasskeyAndRecover(recoveryKey, submitAction, retrieveVaultAction)
                }
                SecureBackupEnterRecoveryKeyEvents.ClearDialog -> {
                    submitAction.value = AsyncAction.Uninitialized
                    retrieveVaultAction.value = AsyncAction.Uninitialized
                }
                SecureBackupEnterRecoveryKeyEvents.RetrieveFromVault -> {
                    coroutineScope.retrievePasskeyAndRecover(recoveryKey, submitAction, retrieveVaultAction)
                }
            }
        }

        val recoveryKeyViewState = RecoveryKeyViewState(
            recoveryKeyUserStory = RecoveryKeyUserStory.Enter,
            formattedRecoveryKey = recoveryKey,
            inProgress = false,
            isVaultMode = isVaultMode,
        )

        // Enable submit button when passphrase is not empty
        val isSubmitEnabled = recoveryKey.isNotBlank()

        return SecureBackupEnterRecoveryKeyState(
            recoveryKeyViewState = recoveryKeyViewState,
            submitAction = submitAction.value,
            retrieveVaultAction = retrieveVaultAction.value,
            isVaultMode = isVaultMode,
            isSubmitEnabled = isSubmitEnabled,
            eventSink = ::handleEvents
        )
    }

    private fun CoroutineScope.recover(
        recoveryKey: String,
        submitAction: MutableState<AsyncAction<Unit>>,
    ) = launch {
        suspend {
            encryptionService.recover(recoveryKey).getOrThrow()
        }.runCatchingUpdatingState(submitAction)
    }
    
    private fun CoroutineScope.retrievePasskeyAndRecover(
        passphrase: String,
        submitAction: MutableState<AsyncAction<Unit>>,
        retrieveVaultAction: MutableState<AsyncAction<String>>,
    ) = launch {
        Timber.d("Retrieving passkey with passphrase length: ${passphrase.length}")
        
        // First, retrieve the passkey using the passphrase
        retrieveVaultAction.value = AsyncAction.Loading
        Timber.d("Setting retrieveVaultAction to Loading")
        
        try {
            Timber.d("Calling passkeyService().retrievePasskey() with passphrase")
            val result = matrixClient.passkeyService().retrievePasskey(passphrase)
            
            result.fold(
                onSuccess = { recoveryKey ->
                    Timber.d("Passkey retrieved successfully, key length: ${recoveryKey.length}")
                    retrieveVaultAction.value = AsyncAction.Success(recoveryKey)
                    Timber.d("Setting retrieveVaultAction to Success")
                    Timber.d("Beginning recovery process with the retrieved passkey")
                    recover(recoveryKey, submitAction)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to retrieve passkey with the provided passphrase: ${error.message}")
                    
                    // Special case for NoOpPasskeyService
                    if (error is UnsupportedOperationException && error.message?.contains("NoOp implementation") == true) {
                        Timber.d("NoOp implementation detected - vault feature not available")
                        val friendlyError = Exception(
                            "Vault feature is not available in this build. Please contact support.",
                            error
                        )
                        retrieveVaultAction.value = AsyncAction.Failure(friendlyError)
                    } else if (error.message?.contains("HTTP 404") == true || error.message?.contains("No recovery key found") == true) {
                        // Handle the case where no recovery key is found for this user/passphrase
                        val friendlyError = Exception(
                            "No recovery key found for this passphrase. Please check and try again.",
                            error
                        )
                        retrieveVaultAction.value = AsyncAction.Failure(friendlyError)
                    } else {
                        val friendlyError = Exception(
                            "Failed to retrieve recovery key. Please check your passphrase and try again.",
                            error
                        )
                        retrieveVaultAction.value = AsyncAction.Failure(friendlyError)
                    }
                    Timber.d("Setting retrieveVaultAction to Failure with error: ${error.message}")
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception occurred while retrieving passkey: ${e.message}")
            
            // Better handle connection-related errors
            val errorMessage = if (e.message?.contains("Failed to connect") == true || 
                                  e.message?.contains("timeout") == true ||
                                  e.message?.contains("UnknownHostException") == true) {
                "Could not connect to the server. Please check your internet connection and try again."
            } else {
                "An error occurred while connecting to the server. Please try again later."
            }
            
            val friendlyError = Exception(errorMessage, e)
            retrieveVaultAction.value = AsyncAction.Failure(friendlyError)
            Timber.d("Setting retrieveVaultAction to Failure due to exception: ${friendlyError.message}")
        }
    }
}
