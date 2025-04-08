/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.encryption.PasskeyService
import io.element.android.libraries.network.RetrofitFactory
import javax.inject.Inject
import timber.log.Timber

/**
 * Default implementation of [PasskeyService] that extends [BasePasskeyService].
 * This implementation uses the base functionality without any modifications.
 */
@ContributesBinding(SessionScope::class, PasskeyService::class)
class DefaultPasskeyService @Inject constructor(
    retrofitFactory: RetrofitFactory,
    userId: String
) : BasePasskeyService(retrofitFactory, userId) {
    init {
        Timber.d("Initializing DefaultPasskeyService for user: $userId")
    }
}
