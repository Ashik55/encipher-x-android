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
}