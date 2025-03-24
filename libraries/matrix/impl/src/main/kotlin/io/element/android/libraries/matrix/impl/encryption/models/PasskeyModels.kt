/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Data model for passkey save requests.
 */
@Serializable
data class SavePasskeyRequest(
    @SerialName("passkey") val passkey: String,
    @SerialName("passphrase") val passphrase: String
)

/**
 * Data model for passkey retrieve requests.
 */
@Serializable
data class RetrievePasskeyRequest(
    @SerialName("passphrase") val passphrase: String
)

/**
 * Data model for passkey check requests.
 */
@Serializable
data class CheckPasskeyRequest(
    @SerialName("passphrase") val passphrase: String
)

/**
 * Data model for passkey responses.
 */
@Serializable
data class PasskeyResponse(
    @SerialName("passkey") val passkey: String
)

/**
 * Data model for passkey save responses with encryption details.
 */
@Serializable
data class SavePasskeyResponse(
    @SerialName("requester") val requester: String,
    @SerialName("passkey") val passkey: String,
    @SerialName("passphrase") val passphrase: String,
    @SerialName("encrypted_passkey") val encryptedPasskey: String
)

/**
 * Data model for checking if a user has a passkey.
 */
@Serializable
data class CheckPasskeyResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("has_passkey") val hasPasskey: Boolean
) 