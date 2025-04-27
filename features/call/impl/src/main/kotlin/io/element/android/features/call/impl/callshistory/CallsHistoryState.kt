/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import io.element.android.libraries.architecture.AsyncData
import kotlinx.coroutines.flow.StateFlow

interface CallsHistoryState {
    val callsList: StateFlow<AsyncData<List<Call>>>
    val favorites: StateFlow<List<Call>>
    val currentUserId: StateFlow<String?>
    
    /**
     * Whether there's more data to load (for pagination).
     */
    val hasMoreToLoad: StateFlow<Boolean>
    
    /**
     * Whether we're currently loading the next page.
     */
    val isLoadingMore: StateFlow<Boolean>
    
    /**
     * Event sink for the CallsHistoryView.
     */
    val eventSink: (CallsHistoryEvents) -> Unit
}

/**
 * Events for the calls history screen.
 */
sealed interface CallsHistoryEvents {
    /**
     * Request to load the next page of calls.
     */
    data object LoadMore : CallsHistoryEvents
}