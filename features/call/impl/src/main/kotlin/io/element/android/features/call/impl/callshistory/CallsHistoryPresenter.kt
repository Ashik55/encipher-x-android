/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.SessionId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class CallsHistoryPresenter @Inject constructor(
    private val callRepository: CallRepository,
    private val matrixClientProvider: MatrixClientProvider,
    private val activeSessionIdHolder: ActiveSessionIdHolder
) : Presenter<CallsHistoryState> {
    @Composable
    override fun present(): CallsHistoryState {
        val callsList = remember { MutableStateFlow<AsyncData<List<Call>>>(AsyncData.Loading()) }
        val favorites = remember { MutableStateFlow<List<Call>>(emptyList()) }
        val currentUserId = remember { MutableStateFlow<String?>(null) }
        
        var retryCount by remember { mutableStateOf(0) }
        
        LaunchedEffect(retryCount) {
            // Get the active session ID from the holder using the suspend function
            val sessionId = activeSessionIdHolder.getActiveSessionId()
            loadCallHistory(callsList, sessionId)
            
            // Store the user ID for determining outgoing calls
            currentUserId.value = sessionId?.value
        }
        
        return object : CallsHistoryState {
            override val callsList: StateFlow<AsyncData<List<Call>>> = callsList
            override val favorites: StateFlow<List<Call>> = favorites
            override val currentUserId: StateFlow<String?> = currentUserId
        }
    }
    
    private suspend fun loadCallHistory(callsListState: MutableStateFlow<AsyncData<List<Call>>>, sessionId: SessionId?) {
        if (sessionId == null) {
            callsListState.value = AsyncData.Failure(IllegalStateException("No active session"))
            return
        }
        
        callsListState.value = AsyncData.Loading()
        
        try {
            // Get the current user ID from the session
            val result = callRepository.getCallDetails(sessionId, sessionId.value)
            
            result.fold(
                onSuccess = { calls ->
                    Timber.d("Successfully loaded ${calls.size} calls")
                    callsListState.value = AsyncData.Success(calls)
                },
                onFailure = { error ->
                    Timber.e(error, "Error loading call history")
                    callsListState.value = AsyncData.Failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception loading call history")
            callsListState.value = AsyncData.Failure(e)
        }
    }
}
