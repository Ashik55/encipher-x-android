/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.runtime.Composable
import io.element.android.libraries.architecture.Presenter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CallsHistoryPresenter @Inject constructor() : Presenter<CallsHistoryState> {
    @Composable
    override fun present(): CallsHistoryState {
        // Static dummy data for UI demonstration
        val callsList = MutableStateFlow<List<Call>>(
            listOf(
                Call(
                    call_id = 396,
                    caller_user_id = "@ben5:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "audio",
                    created_ts = "2025-04-15T09:56:12.505007",
                    ended_ts = null
                ),
                Call(
                    call_id = 395,
                    caller_user_id = "@ben5:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "video",
                    created_ts = "2025-04-15T09:51:27.231343",
                    ended_ts = null
                ),
                Call(
                    call_id = 394,
                    caller_user_id = "@ben5:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "audio",
                    created_ts = "2025-04-15T09:51:00.953547",
                    ended_ts = "2025-04-15T09:52:00.953547"
                ),
                Call(
                    call_id = 390,
                    caller_user_id = "@rahat:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "audio",
                    created_ts = "2025-04-15T08:34:57.248704",
                    ended_ts = null
                ),
                Call(
                    call_id = 389,
                    caller_user_id = "@rahat:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "video",
                    created_ts = "2025-04-15T08:34:30.286147",
                    ended_ts = "2025-04-15T08:34:52.989864"
                ),
                Call(
                    call_id = 388,
                    caller_user_id = "@zobayer:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "audio",
                    created_ts = "2025-04-15T07:54:35.969466",
                    ended_ts = null
                ),
                Call(
                    call_id = 386,
                    caller_user_id = "@zobayer:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "video",
                    created_ts = "2025-04-15T07:53:28.482478",
                    ended_ts = null
                ),
                Call(
                    call_id = 385,
                    caller_user_id = "@hasib:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "audio",
                    created_ts = "2025-04-15T07:41:07.701234",
                    ended_ts = null
                )
            )
        )
        
        // Example favorite call for demonstration
        val favorites = MutableStateFlow<List<Call>>(
            listOf(
                Call(
                    call_id = 390,
                    caller_user_id = "@rahat:dev.enciph-er.com",
                    room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                    call_type = "audio",
                    created_ts = "2025-04-15T08:34:57.248704",
                    ended_ts = null
                )
            )
        )
        
        return object : CallsHistoryState {
            override val callsList: StateFlow<List<Call>> = callsList
            override val favorites: StateFlow<List<Call>> = favorites
        }
    }
}
