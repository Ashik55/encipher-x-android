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
import io.element.android.libraries.matrix.impl.encryption.services.PasskeyApiService
import io.element.android.libraries.network.RetrofitFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import timber.log.Timber

@Module
@ContributesTo(AppScope::class)
class AppEncryptionModule @Inject constructor() {

    @Provides
    @SingleIn(AppScope::class)
    fun providePasskeyApiService(): PasskeyApiService =
        Retrofit.Builder()
            .run {
                addConverterFactory(GsonConverterFactory.create())
                baseUrl("https://dev.enciph-er.com/")
                build()
            }.create(PasskeyApiService::class.java)
            
    @Provides
    @SingleIn(AppScope::class)
    fun providePasskeyServiceFactory(
        retrofitFactory: RetrofitFactory
    ): PasskeyServiceFactory {
        Timber.d("Creating PasskeyServiceFactory in AppScope")
        return DefaultPasskeyServiceFactory(retrofitFactory)
    }
    
    @Provides
    @SingleIn(AppScope::class)
    fun provideNoOpPasskeyService(): PasskeyService {
        Timber.d("Creating NoOpPasskeyService in AppScope")
        return NoOpPasskeyService()
    }
}