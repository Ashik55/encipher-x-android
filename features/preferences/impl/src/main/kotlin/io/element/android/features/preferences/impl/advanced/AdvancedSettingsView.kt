/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.preferences.impl.advanced

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.dialogs.ListOption
import io.element.android.libraries.designsystem.components.dialogs.SingleSelectionDialog
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.components.preferences.PreferenceSwitch
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.theme.theme.Theme
import io.element.android.libraries.theme.theme.themes
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AdvancedSettingsView(
    state: AdvancedSettingsState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        modifier = modifier,
        onBackPressed = onBackPressed,
        title = stringResource(id = CommonStrings.common_advanced_settings)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = CommonStrings.common_appearance)
                )
            },
            trailingContent = ListItemContent.Text(
                state.theme.toHumanReadable()
            ),
            onClick = {
                state.eventSink(AdvancedSettingsEvents.ChangeTheme)
            }
        )
        PreferenceSwitch(
            title = stringResource(id = CommonStrings.common_rich_text_editor),
            subtitle = stringResource(id = R.string.screen_advanced_settings_rich_text_editor_description),
            isChecked = state.isRichTextEditorEnabled,
            onCheckedChange = { state.eventSink(AdvancedSettingsEvents.SetRichTextEditorEnabled(it)) },
        )
        PreferenceSwitch(
            title = stringResource(id = R.string.screen_advanced_settings_developer_mode),
            subtitle = stringResource(id = R.string.screen_advanced_settings_developer_mode_description),
            isChecked = state.isDeveloperModeEnabled,
            onCheckedChange = { state.eventSink(AdvancedSettingsEvents.SetDeveloperModeEnabled(it)) },
        )
    }

    if (state.showChangeThemeDialog) {
        SingleSelectionDialog(
            options = getOptions(),
            initialSelection = themes.indexOf(state.theme),
            onOptionSelected = {
                state.eventSink(
                    AdvancedSettingsEvents.SetTheme(
                        themes[it]
                    )
                )
            },
            onDismissRequest = { state.eventSink(AdvancedSettingsEvents.CancelChangeTheme) },
        )
    }
}

@Composable
private fun getOptions(): ImmutableList<ListOption> {
    return themes.map {
        ListOption(title = it.toHumanReadable())
    }.toImmutableList()
}

@Composable
private fun Theme.toHumanReadable(): String {
    return stringResource(
        id = when (this) {
            Theme.System -> CommonStrings.common_system
            Theme.Dark -> CommonStrings.common_dark
            Theme.Light -> CommonStrings.common_light
        }
    )
}

@PreviewsDayNight
@Composable
internal fun AdvancedSettingsViewPreview(@PreviewParameter(AdvancedSettingsStateProvider::class) state: AdvancedSettingsState) =
    ElementPreview {
        AdvancedSettingsView(state = state, onBackPressed = { })
    }
