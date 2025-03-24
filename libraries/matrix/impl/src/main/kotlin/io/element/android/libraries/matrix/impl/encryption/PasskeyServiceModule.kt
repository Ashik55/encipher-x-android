/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.encryption.PasskeyService

@Module
@ContributesTo(SessionScope::class)
abstract class PasskeyServiceModule {
    @Binds
    abstract fun bindPasskeyService(implementation: DefaultPasskeyService): PasskeyService
} 