/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appnav

import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.room.RoomMembershipObserver
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoggedInEventProcessor @Inject constructor(
    private val snackbarDispatcher: SnackbarDispatcher,
    roomMembershipObserver: RoomMembershipObserver,
    private val matrixClient: MatrixClient,
) {
    private var observingJob: Job? = null

    private val displayLeftRoomMessage = roomMembershipObserver.updates

    fun observeEvents(coroutineScope: CoroutineScope) {
        observingJob = coroutineScope.launch {
            displayLeftRoomMessage
                .collect { update ->
                    if (!update.isUserInRoom) {
                        displayMessage(CommonStrings.common_current_user_left_room)
                        
                        // Force refresh the room list if needed
                        if (update.forceRefreshRoomList) {
                            matrixClient.roomListService.forceRefreshRoomList()
                        }
                    }
                }
        }
    }

    fun stopObserving() {
        observingJob?.cancel()
        observingJob = null
    }

    private fun displayMessage(message: Int) {
        snackbarDispatcher.post(SnackbarMessage(message))
    }
}
