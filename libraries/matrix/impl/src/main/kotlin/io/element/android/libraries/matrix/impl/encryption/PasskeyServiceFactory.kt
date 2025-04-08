/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.matrix.api.encryption.PasskeyService
import io.element.android.libraries.network.RetrofitFactory
import timber.log.Timber

/**
 * Interface for creating PasskeyService instances.
 */
interface PasskeyServiceFactory {
    /**
     * Creates a new PasskeyService instance.
     * 
     * @param userId The Matrix user ID to create the service for.
     * @return A new PasskeyService instance.
     */
    fun create(userId: String): PasskeyService
}

/**
 * Implementation of PasskeyServiceFactory that creates PasskeyService instances.
 */
class DefaultPasskeyServiceFactory(
    private val retrofitFactory: RetrofitFactory
) : PasskeyServiceFactory {
    override fun create(userId: String): PasskeyService {
        Timber.d("Creating PasskeyService for user: $userId")
        return DefaultPasskeyService(
            retrofitFactory = retrofitFactory,
            userId = userId
        )
    }
} 
