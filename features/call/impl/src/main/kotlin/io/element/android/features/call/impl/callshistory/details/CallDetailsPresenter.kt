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
        val hasMoreToLoad = remember { MutableStateFlow(true) }
        val isLoadingMore = remember { MutableStateFlow(false) }
        
        var nextPageToken by remember { mutableStateOf<String?>(null) }
        var currentCalls by remember { mutableStateOf<List<Call>>(emptyList()) }
        var retryCount by remember { mutableStateOf(0) }
        var sessionIdValue by remember { mutableStateOf<SessionId?>(null) }
        
        // Get the initial call or use a placeholder if not set
        val callToUse = initialCall ?: createPlaceholderCall()

        // Event to trigger loading more content
        var loadMoreEvent by remember { mutableStateOf(0) }
        
        LaunchedEffect(retryCount) {
            // Get the active session ID from the holder using the suspend function
            val sessionId = activeSessionIdHolder.getActiveSessionId()
            sessionIdValue = sessionId
            
            // Store the user ID for determining outgoing calls
            currentUserId.value = sessionId?.value
            
            // Load call history for this specific room
            callToUse.room_id?.let { roomId ->
                loadCallDetailsForRoom(
                    callsListState = callsList,
                    sessionId = sessionId,
                    roomId = roomId,
                    page = null,
                    isInitialLoad = true,
                    onDataLoaded = { calls, nextPage, prevPage ->
                        currentCalls = calls
                        nextPageToken = nextPage
                        hasMoreToLoad.value = nextPage != null
                    }
                )
            } ?: run {
                // If there's no room ID, just show this call
                callsList.value = AsyncData.Success(listOf(callToUse))
                hasMoreToLoad.value = false
            }
        }

        // Handle loading more content with LaunchedEffect
        LaunchedEffect(loadMoreEvent) {
            if (loadMoreEvent > 0 && hasMoreToLoad.value && !isLoadingMore.value && nextPageToken != null && callToUse.room_id != null) {
                isLoadingMore.value = true
                
                // Load the next page of calls
                callToUse.room_id?.let { roomId ->
                    loadCallDetailsForRoom(
                        callsListState = callsList,
                        sessionId = sessionIdValue,
                        roomId = roomId,
                        page = nextPageToken,
                        isInitialLoad = false,
                        onDataLoaded = { newCalls, nextPage, prevPage ->
                            // Append the new calls to the existing list
                            val combinedList = currentCalls + newCalls
                            currentCalls = combinedList
                            callsList.value = AsyncData.Success(combinedList)
                            
                            // Update pagination state
                            nextPageToken = nextPage
                            hasMoreToLoad.value = nextPage != null
                            isLoadingMore.value = false
                        }
                    )
                }
            }
        }
        
        fun handleEvents(event: CallDetailsEvents) {
            when (event) {
                is CallDetailsEvents.LoadMore -> {
                    // Increment the counter to trigger the LaunchedEffect
                    loadMoreEvent++
                }
            }
        }
        
        return object : CallDetailsState {
            override val callsList: StateFlow<AsyncData<List<Call>>> = callsList
            override val currentUserId: StateFlow<String?> = currentUserId
            override val initialCall: Call = callToUse
            override val hasMoreToLoad: StateFlow<Boolean> = hasMoreToLoad
            override val isLoadingMore: StateFlow<Boolean> = isLoadingMore
            override val eventSink: (CallDetailsEvents) -> Unit = ::handleEvents
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
        roomId: String,
        page: String?,
        isInitialLoad: Boolean,
        onDataLoaded: (List<Call>, String?, String?) -> Unit
    ) {
        if (sessionId == null) {
            if (isInitialLoad) {
                callsListState.value = AsyncData.Failure(IllegalStateException("No active session"))
            }
            onDataLoaded(emptyList(), null, null)
            return
        }
        
        if (isInitialLoad) {
            callsListState.value = AsyncData.Loading()
        }
        
        try {
            // Get the calls for the specific room with pagination
            val result = callRepository.getCallDetails(
                sessionId = sessionId, 
                userId = sessionId.value,
                roomId = roomId,
                page = page
            )
            
            result.fold(
                onSuccess = { paginatedResult ->
                    Timber.d("Successfully loaded ${paginatedResult.calls.size} calls for room $roomId, page: $page")
                    
                    if (isInitialLoad) {
                        callsListState.value = AsyncData.Success(paginatedResult.calls)
                    }
                    
                    // Pass the data back to the caller
                    onDataLoaded(paginatedResult.calls, paginatedResult.nextPage, paginatedResult.prevPage)
                },
                onFailure = { error ->
                    Timber.e(error, "Error loading call details for room $roomId, page: $page")
                    
                    if (isInitialLoad) {
                        callsListState.value = AsyncData.Failure(error)
                    }
                    
                    onDataLoaded(emptyList(), null, null)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception loading call details for room $roomId, page: $page")
            
            if (isInitialLoad) {
                callsListState.value = AsyncData.Failure(e)
            }
            
            onDataLoaded(emptyList(), null, null)
        }
    }
}