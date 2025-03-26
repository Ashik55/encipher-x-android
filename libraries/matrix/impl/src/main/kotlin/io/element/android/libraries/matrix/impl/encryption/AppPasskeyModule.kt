/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.encryption.PasskeyService
import io.element.android.libraries.network.RetrofitFactory
import javax.inject.Inject
import timber.log.Timber

/**
 * Provides a factory for creating PasskeyService instances at app scope.
 */
@ContributesTo(AppScope::class)
@Module
class AppPasskeyModule @Inject constructor() {
    
    @Provides
    @SingleIn(AppScope::class)
    fun providePasskeyServiceFactory(
        retrofitFactory: RetrofitFactory
    ): PasskeyServiceFactory {
        Timber.d("Creating PasskeyServiceFactory in AppScope")
        return AppScopePasskeyServiceFactory(retrofitFactory)
    }
    
    /**
     * App-scoped implementation of PasskeyServiceFactory that doesn't require MatrixClient.
     */
    private class AppScopePasskeyServiceFactory(
        private val retrofitFactory: RetrofitFactory
    ) : PasskeyServiceFactory {
        override fun create(userId: String): PasskeyService {
            Timber.d("Creating PasskeyService with user ID: $userId")
            return ProductionPasskeyService(
                retrofitFactory = retrofitFactory,
                userId = userId
            )
        }
    }
} 