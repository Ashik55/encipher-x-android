/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.encryption.PasskeyService
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.CheckPasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.PasskeyResponse
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyRequest
import io.element.android.libraries.matrix.impl.encryption.models.SavePasskeyResponse
import io.element.android.libraries.network.RetrofitFactory
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

/**
 * Default implementation of [PasskeyService] that extends [BasePasskeyService].
 * This implementation uses the base functionality without any modifications.
 */
class DefaultPasskeyService(
    retrofitFactory: RetrofitFactory,
    userId: String
) : BasePasskeyService(retrofitFactory, userId) {
    init {
        Timber.d("Initializing DefaultPasskeyService for user: $userId")
    }
} 
