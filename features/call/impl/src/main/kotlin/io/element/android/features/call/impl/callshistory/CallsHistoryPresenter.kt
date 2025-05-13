/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.element.android.features.call.api.CurrentCall
import io.element.android.features.call.api.CurrentCallService
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.core.SessionId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

class CallsHistoryPresenter @Inject constructor(
    private val callRepository: CallRepository,
    private val matrixClientProvider: MatrixClientProvider,
    private val activeSessionIdHolder: ActiveSessionIdHolder,
    private val currentCallService: CurrentCallService
) : Presenter<CallsHistoryState> {
    @Composable
    override fun present(): CallsHistoryState {
        val callsList = remember { MutableStateFlow<AsyncData<List<Call>>>(AsyncData.Loading()) }
        val favorites = remember { MutableStateFlow<List<Call>>(emptyList()) }
        val currentUserId = remember { MutableStateFlow<String?>(null) }
        val hasMoreToLoad = remember { MutableStateFlow(true) }
        val isLoadingMore = remember { MutableStateFlow(false) }
        
        var retryCount by remember { mutableStateOf(0) }
        var nextPageToken by remember { mutableStateOf<String?>(null) }
        var currentCalls by remember { mutableStateOf<List<Call>>(emptyList()) }
        var sessionIdValue by remember { mutableStateOf<SessionId?>(null) }
        
        // Event to trigger loading more content
        var loadMoreEvent by remember { mutableStateOf(0) }
        
        // Function to refresh call history
        suspend fun refreshCallHistory() {
            Timber.d("Refreshing call history")
            sessionIdValue?.let { sessionId ->
                loadCallHistory(
                    callsListState = callsList,
                    sessionId = sessionId,
                    page = null,
                    isInitialLoad = true,
                    onDataLoaded = { calls, nextPage, prevPage ->
                        currentCalls = calls
                        nextPageToken = nextPage
                        hasMoreToLoad.value = nextPage != null
                    }
                )
            }
        }
        
        LaunchedEffect(retryCount) {
            // Get the active session ID from the holder using the suspend function
            val sessionId = activeSessionIdHolder.getActiveSessionId()
            sessionIdValue = sessionId
            loadCallHistory(
                callsListState = callsList,
                sessionId = sessionId,
                page = null,
                isInitialLoad = true,
                onDataLoaded = { calls, nextPage, prevPage ->
                    currentCalls = calls
                    nextPageToken = nextPage
                    hasMoreToLoad.value = nextPage != null
                }
            )
            
            // Store the user ID for determining outgoing calls
            currentUserId.value = sessionId?.value
        }
        
        // Observe the current call state to refresh the call list when a call ends
        LaunchedEffect(Unit) {
            var previousCallState: CurrentCall? = null
            
            currentCallService.currentCall.collectLatest { currentCall ->
                // If we had a call before and now we don't, refresh the call list
                if (previousCallState != null && previousCallState != CurrentCall.None && currentCall == CurrentCall.None) {
                    Timber.d("Call ended, refreshing call history")
                    refreshCallHistory()
                }
                
                previousCallState = currentCall
            }
        }
        
        // Handle loading more content with LaunchedEffect
        LaunchedEffect(loadMoreEvent) {
            if (loadMoreEvent > 0 && hasMoreToLoad.value && !isLoadingMore.value && nextPageToken != null) {
                Timber.d("Starting to load next page: $nextPageToken, current items: ${currentCalls.size}")
                isLoadingMore.value = true
                
                // Load the next page of calls
                loadCallHistory(
                    callsListState = callsList,
                    sessionId = sessionIdValue,
                    page = nextPageToken,
                    isInitialLoad = false,
                    onDataLoaded = { newCalls, nextPage, prevPage ->
                        // Append the new calls to the existing list
                        val combinedList = currentCalls + newCalls
                        Timber.d("Loaded ${newCalls.size} more items, new total: ${combinedList.size}, hasMore: ${nextPage != null}")
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
        
        fun handleEvents(event: CallsHistoryEvents) {
            when (event) {
                is CallsHistoryEvents.LoadMore -> {
                    // Log the event to verify it's being triggered
                    Timber.d("LoadMore event triggered in CallsHistoryPresenter")
                    // Increment the counter to trigger the LaunchedEffect
                    loadMoreEvent++
                }
            }
        }
        
        return object : CallsHistoryState {
            override val callsList: StateFlow<AsyncData<List<Call>>> = callsList
            override val favorites: StateFlow<List<Call>> = favorites
            override val currentUserId: StateFlow<String?> = currentUserId
            override val hasMoreToLoad: StateFlow<Boolean> = hasMoreToLoad
            override val isLoadingMore: StateFlow<Boolean> = isLoadingMore
            override val eventSink: (CallsHistoryEvents) -> Unit = ::handleEvents
        }
    }
    
    private suspend fun loadCallHistory(
        callsListState: MutableStateFlow<AsyncData<List<Call>>>,
        sessionId: SessionId?,
        page: String? = null,
        isInitialLoad: Boolean = true,
        onDataLoaded: (List<Call>, String?, String?) -> Unit = { _, _, _ -> }
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
            // Get call history data from repository with pagination
            val result = callRepository.getCallDetails(sessionId, sessionId.value, page = page)
            
            result.fold(
                onSuccess = { paginatedResult ->
                    Timber.d("Successfully loaded ${paginatedResult.calls.size} calls, page: $page")
                    
                    if (isInitialLoad) {
                        callsListState.value = AsyncData.Success(paginatedResult.calls)
                    }
                    
                    // Pass the data back to the caller
                    onDataLoaded(paginatedResult.calls, paginatedResult.nextPage, paginatedResult.prevPage)
                },
                onFailure = { error ->
                    Timber.e(error, "Error loading call history, page: $page")
                    
                    if (isInitialLoad) {
                        callsListState.value = AsyncData.Failure(error)
                    }
                    
                    onDataLoaded(emptyList(), null, null)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception loading call history, page: $page")
            
            if (isInitialLoad) {
                callsListState.value = AsyncData.Failure(e)
            }
            
            onDataLoaded(emptyList(), null, null)
        }
    }

    // Function to get the active session ID for call initiation
    fun getActiveSessionId(): SessionId? {
        return activeSessionIdHolder.getActiveSessionIdSync()
    }
}
