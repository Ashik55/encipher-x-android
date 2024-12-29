/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemRedactedContent
import io.element.android.libraries.designsystem.icons.CompoundDrawables
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun TimelineItemRedactedView(
    @Suppress("UNUSED_PARAMETER") content: TimelineItemRedactedContent,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    modifier: Modifier = Modifier
) {
    TimelineItemInformativeView(
        text = stringResource(id = CommonStrings.common_message_removed),
        iconDescription = stringResource(id = CommonStrings.common_message_removed),
        iconResourceId = R.drawable.ic_remove,
        onContentLayoutChange = onContentLayoutChange,
        modifier = modifier
    )
}

@PreviewsDayNight
@Composable
internal fun TimelineItemRedactedViewPreview() = ElementPreview {
    TimelineItemRedactedView(
        TimelineItemRedactedContent,
        onContentLayoutChange = {},
    )
}
