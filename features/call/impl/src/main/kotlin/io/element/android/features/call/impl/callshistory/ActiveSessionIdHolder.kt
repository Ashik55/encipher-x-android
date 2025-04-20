/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import io.element.android.libraries.matrix.api.core.SessionId
import javax.inject.Inject

/**
 * Holder for the active session ID.
 */
interface ActiveSessionIdHolder {
    /**
     * Get the active session ID, or null if there is none.
     */
    suspend fun getActiveSessionId(): SessionId?
}

/**
 * Default implementation of [ActiveSessionIdHolder].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultActiveSessionIdHolder @Inject constructor(
    private val authenticationService: MatrixAuthenticationService
) : ActiveSessionIdHolder {
    override suspend fun getActiveSessionId(): SessionId? {
        return authenticationService.getLatestSessionId()
    }
}