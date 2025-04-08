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
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.encryption.PasskeyService
import javax.inject.Inject

@Module
@ContributesTo(SessionScope::class)
abstract class PasskeyServiceModule {
    companion object {
        @Provides
        fun providePasskeyService(
            factory: PasskeyServiceFactory,
            matrixClient: MatrixClient
        ): PasskeyService {
            // Use the actual user ID from the matrix client
            return factory.create(matrixClient.sessionId.value)
        }
    }
}