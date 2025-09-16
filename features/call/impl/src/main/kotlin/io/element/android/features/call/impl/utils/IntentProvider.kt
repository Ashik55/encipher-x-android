/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import io.element.android.features.call.api.CallType
import io.element.android.features.call.impl.DefaultElementCallEntryPoint
import io.element.android.features.call.impl.ui.ElementCallActivity

internal object IntentProvider {
    fun createIntent(context: Context, callType: CallType, isAudioCall: Boolean?, isIncomingCall: Boolean = false, roomName: String? = null): Intent = Intent(context, ElementCallActivity::class.java).apply {
        putExtra(DefaultElementCallEntryPoint.EXTRA_CALL_TYPE, callType)
        putExtra(DefaultElementCallEntryPoint.IS_AUDIO_CALL, isAudioCall)
        putExtra(DefaultElementCallEntryPoint.IS_INCOMING_CALL, isIncomingCall)
        if (roomName != null) {
            putExtra("EXTRA_ROOM_NAME", roomName)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    fun getPendingIntent(context: Context, callType: CallType, isAudioCall: Boolean? = null, isIncomingCall: Boolean = false, roomName: String? = null): PendingIntent {
        return PendingIntentCompat.getActivity(
            context,
            DefaultElementCallEntryPoint.REQUEST_CODE,
            createIntent(context, callType, isAudioCall, isIncomingCall, roomName),
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
        )!!
    }
}
