/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.element.android.features.call.impl.callshistory.ActiveSessionIdHolder
import io.element.android.features.call.impl.callshistory.Call
import io.element.android.features.call.impl.callshistory.CallRepository
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.core.SessionId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Presenter for the call details screen.
 */
class CallDetailsPresenter @Inject constructor(
    private val callRepository: CallRepository,
    private val activeSessionIdHolder: ActiveSessionIdHolder
) : Presenter<CallDetailsState> {

    // Store the initial call that will be provided from the CallDetailsScreen
    private var initialCall: Call? = null
    
    fun initialize(call: Call) {
        this.initialCall = call
    }

    @Composable
    override fun present(): CallDetailsState {
        val callsList = remember { MutableStateFlow<AsyncData<List<Call>>>(AsyncData.Loading()) }
        val currentUserId = remember { MutableStateFlow<String?>(null) }
        
        var retryCount by remember { mutableStateOf(0) }
        
        // Get the initial call or use a placeholder if not set
        val callToUse = initialCall ?: createPlaceholderCall()
        
        LaunchedEffect(retryCount) {
            // Get the active session ID from the holder using the suspend function
            val sessionId = activeSessionIdHolder.getActiveSessionId()
            
            // Store the user ID for determining outgoing calls
            currentUserId.value = sessionId?.value
            
            // Load call history for this specific room
            callToUse.room_id?.let { roomId ->
                loadCallDetailsForRoom(callsList, sessionId, roomId)
            } ?: run {
                // If there's no room ID, just show this call
                callsList.value = AsyncData.Success(listOf(callToUse))
            }
        }
        
        return object : CallDetailsState {
            override val callsList: StateFlow<AsyncData<List<Call>>> = callsList
            override val currentUserId: StateFlow<String?> = currentUserId
            override val initialCall: Call = callToUse
        }
    }

    private fun createPlaceholderCall(): Call {
        return Call(
            call_id = null,
            caller_user_id = "",
            room_id = null,
            call_type = "audio",
            created_ts = null,
            ended_ts = null,
            caller_display_name = "Unknown",
            room_name = null,
            room_avatar = null,
            caller_avatar = null,
            is_caller = false,
            receiver_user_ids = null,
            receiver_display_names = null,
            receiver_avatars = null
        )
    }

    private suspend fun loadCallDetailsForRoom(
        callsListState: MutableStateFlow<AsyncData<List<Call>>>,
        sessionId: SessionId?,
        roomId: String
    ) {
        if (sessionId == null) {
            callsListState.value = AsyncData.Failure(IllegalStateException("No active session"))
            return
        }
        
        callsListState.value = AsyncData.Loading()
        
        try {
            // Get the calls for the specific room
            val result = callRepository.getCallDetails(
                sessionId = sessionId, 
                userId = sessionId.value,
                roomId = roomId
            )
            
            result.fold(
                onSuccess = { calls ->
                    Timber.d("Successfully loaded ${calls.size} calls for room $roomId")
                    callsListState.value = AsyncData.Success(calls)
                },
                onFailure = { error ->
                    Timber.e(error, "Error loading call details for room $roomId")
                    callsListState.value = AsyncData.Failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception loading call details for room $roomId")
            callsListState.value = AsyncData.Failure(e)
        }
    }
}