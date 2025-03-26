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

/**
 * Production implementation of PasskeyService that connects to the actual API.
 */
@ContributesTo(AppScope::class)
@Module
class AppPasskeyModule @Inject constructor() {
    
    @Provides
    @SingleIn(AppScope::class)
    fun providePasskeyServiceFactory(
        retrofitFactory: RetrofitFactory
    ): PasskeyServiceFactory {
        return DefaultPasskeyServiceFactory(retrofitFactory)
    }
    
    @Provides
    @SingleIn(AppScope::class)
    fun providePasskeyService(
        factory: PasskeyServiceFactory
    ): PasskeyService {
        return factory.create()
    }
} 