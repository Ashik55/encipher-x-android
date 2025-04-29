/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.call

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.impl.call.services.CallApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Inject
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Provider
import retrofit2.converter.gson.GsonConverterFactory

@Module
@ContributesTo(AppScope::class)
class AppCallModule  @Inject constructor() {

//    private val json: Provider<Json>

    @Provides
    @SingleIn(AppScope::class)
    fun provideCallApiService(): CallApiService =
        Retrofit.Builder()
            .run {
                addConverterFactory(GsonConverterFactory.create())
                baseUrl("https://prod.enciph-er.com/_matrix/client/v3/")
                build()
            }.create(CallApiService::class.java)

}
