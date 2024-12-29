/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.appconfig

object ApplicationConfig {
    /**
     * Application name used in the UI for string. If empty, the value is taken from the resources `R.string.app_name`.
     * Note that this value is not used for the launcher icon.
     * For Encipher, the value is empty, and so read from `R.string.app_name`, which depends on the build variant:
     * - "Encipher X" for release builds;
     * - "Encipher X dbg" for debug builds;
     * - "Encipher X nightly" for nightly builds.
     */
    const val APPLICATION_NAME: String = ""

    /**
     * Used in the strings to reference the Encipher client.
     * Cannot be empty.
     * For Encipher, the value is "Encipher".
     */
    const val PRODUCTION_APPLICATION_NAME: String = "Encipher"

    /**
     * Used in the strings to reference the Encipher Desktop client, for instance Element Web.
     * Cannot be empty.
     * For Encipher, the value is "Encipher". We use the same name for desktop and mobile for now.
     */
    const val DESKTOP_APPLICATION_NAME: String = "Encipher"
}
