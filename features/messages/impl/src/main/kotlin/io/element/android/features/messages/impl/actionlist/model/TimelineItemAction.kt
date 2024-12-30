/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.actionlist.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.icons.CompoundDrawables
import io.element.android.libraries.ui.strings.CommonStrings

@Immutable
enum class TimelineItemAction(
    @StringRes val titleRes: Int,
    @DrawableRes val icon: Int,
    val destructive: Boolean = false
) {
    ViewInTimeline(CommonStrings.action_view_in_timeline, CompoundDrawables.ic_compound_visibility_on),
    Forward(CommonStrings.action_forward, R.drawable.ic_forward),
    CopyText(CommonStrings.action_copy_text, R.drawable.ic_copy),
    CopyCaption(CommonStrings.action_copy_caption, R.drawable.ic_copy),
//    CopyLink(CommonStrings.action_copy_link_to_message, CompoundDrawables.ic_compound_link),
    Redact(CommonStrings.action_remove, R.drawable.ic_remove, destructive = true),
    Reply(CommonStrings.action_reply, R.drawable.ic_reply),
    ReplyInThread(CommonStrings.action_reply_in_thread, R.drawable.ic_reply),
    Edit(CommonStrings.action_edit, R.drawable.ic_edit_reaction),
    EditPoll(CommonStrings.action_edit_poll, R.drawable.ic_edit_reaction),
    EditCaption(CommonStrings.action_edit_caption, R.drawable.ic_edit_reaction),
    AddCaption(CommonStrings.action_add_caption, R.drawable.ic_edit_reaction),
    RemoveCaption(CommonStrings.action_remove_caption, CompoundDrawables.ic_compound_close, destructive = true),
//    ViewSource(CommonStrings.action_view_source, CompoundDrawables.ic_compound_code),
    ReportContent(CommonStrings.action_report_content, R.drawable.ic_report_chat, destructive = true),
    EndPoll(CommonStrings.action_end_poll, R.drawable.ic_end_polls, destructive = true),
    Pin(CommonStrings.action_pin, R.drawable.ic_pin),
    Unpin(CommonStrings.action_unpin, R.drawable.ic_unpin, destructive = true),
}
