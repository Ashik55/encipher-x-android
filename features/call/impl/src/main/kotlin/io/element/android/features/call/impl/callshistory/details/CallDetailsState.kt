/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory.details

import io.element.android.features.call.impl.callshistory.Call
import io.element.android.libraries.architecture.AsyncData
import kotlinx.coroutines.flow.StateFlow

/**
 * State holder for the call details screen.
 */
interface CallDetailsState {
    /**
     * The list of calls for the current room.
     */
    val callsList: StateFlow<AsyncData<List<Call>>>

    /**
     * The current user ID (to determine outgoing vs incoming calls).
     */
    val currentUserId: StateFlow<String?>
    
    /**
     * The initial call that was used to navigate to this screen.
     */
    val initialCall: Call
    
    /**
     * Whether there's more data to load (for pagination).
     */
    val hasMoreToLoad: StateFlow<Boolean>
    
    /**
     * Whether we're currently loading the next page.
     */
    val isLoadingMore: StateFlow<Boolean>
    
    /**
     * Event sink for the CallDetailsView.
     */
    val eventSink: (CallDetailsEvents) -> Unit
}

/**
 * Events for the call details screen.
 */
sealed interface CallDetailsEvents {
    /**
     * Request to load the next page of calls.
     */
    data object LoadMore : CallDetailsEvents
}