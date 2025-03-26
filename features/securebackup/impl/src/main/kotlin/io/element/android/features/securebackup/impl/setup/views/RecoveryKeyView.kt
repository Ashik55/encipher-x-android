/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.setup.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.securebackup.impl.R
import io.element.android.features.securebackup.impl.tools.RecoveryKeyVisualTransformation
import io.element.android.libraries.designsystem.modifiers.autofill
import io.element.android.libraries.designsystem.modifiers.clickableIfNotNull
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.CustomProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun RecoveryKeyView(
    state: RecoveryKeyViewState,
    onClick: (() -> Unit)?,
    onChange: ((String) -> Unit)?,
    onSubmit: (() -> Unit)?,
    onPassphraseChange: ((String) -> Unit)? = null,
    showRecoveryKey: Boolean = true,
    showInputOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
//        Text(
//            text = stringResource(id = CommonStrings.common_recovery_key),
//            style = ElementTheme.typography.fontBodyMdRegular,
//        )
        RecoveryKeyContent(state, onClick, onChange, onSubmit, onPassphraseChange, showRecoveryKey, showInputOnly)
        RecoveryKeyFooter(state)
    }
}

@Composable
private fun RecoveryKeyContent(
    state: RecoveryKeyViewState,
    onClick: (() -> Unit)?,
    onChange: ((String) -> Unit)?,
    onSubmit: (() -> Unit)?,
    onPassphraseChange: ((String) -> Unit)?,
    showRecoveryKey: Boolean,
    showInputOnly: Boolean,
) {
    when (state.recoveryKeyUserStory) {
        RecoveryKeyUserStory.Setup,
        RecoveryKeyUserStory.Change -> RecoveryKeyStaticContent(state, onClick, onPassphraseChange, showRecoveryKey)
        RecoveryKeyUserStory.Enter -> RecoveryKeyFormContent(state, onChange, onSubmit, showInputOnly)
    }
}

@Composable
private fun RecoveryKeyStaticContent(
    state: RecoveryKeyViewState,
    onClick: (() -> Unit)?,
    onPassphraseChange: ((String) -> Unit)?,
    showRecoveryKey: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Only show recovery key box if explicitly requested
        if (showRecoveryKey) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        color = ElementTheme.colors.bgSubtleSecondary,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.formattedRecoveryKey != null) {
                    // Show the actual recovery key when available
                    RecoveryKeyWithCopy(
                        recoveryKey = state.formattedRecoveryKey,
                        alpha = 1f,  // Make it fully visible
                        onClick = onClick
                    )
                } else {
                    // Show loading state when generating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp)
                    ) {
                        if (state.inProgress) {
                            CustomProgressIndicator(
                                modifier = Modifier
                                    .progressSemantics()
                                    .padding(end = 8.dp)
                                    .size(16.dp),
                            )
                        }
                        Text(
                            text = stringResource(
                                id = when {
                                    state.inProgress -> R.string.screen_recovery_key_generating_key
                                    else -> R.string.screen_recovery_key_vault_save_description
                                }
                            ),
                            textAlign = TextAlign.Center,
                            style = ElementTheme.typography.fontBodyLgMedium,
                        )
                    }
                }
            }
        }
        
        // Always show passphrase input field when a recovery key exists
        if (state.formattedRecoveryKey != null && onPassphraseChange != null) {
            PassphraseInput(
                passphrase = state.passphrase,
                onPassphraseChange = onPassphraseChange,
                modifier = Modifier.fillMaxWidth(),
                isRetrievalMode = false,
            )
        }
    }
}

@Composable
private fun RecoveryKeyWithCopy(
    recoveryKey: String,
    alpha: Float,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickableIfNotNull(onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = recoveryKey,
            color = ElementTheme.colors.textSecondary,
            style = ElementTheme.typography.fontBodyLgRegular.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = CompoundIcons.Copy(),
            contentDescription = stringResource(id = CommonStrings.action_copy),
            tint = ElementTheme.colors.iconSecondary,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RecoveryKeyFormContent(
    state: RecoveryKeyViewState,
    onChange: ((String) -> Unit)?,
    onSubmit: (() -> Unit)?,
    showInputOnly: Boolean = false,
) {
    onChange ?: error("onChange should not be null")
    onSubmit ?: error("onSubmit should not be null")
    
    if (state.isVaultMode || showInputOnly) {
        // In vault mode or when explicitly showing only input, show passphrase input instead of recovery key
        PassphraseInput(
            passphrase = state.formattedRecoveryKey.orEmpty(),
            onPassphraseChange = onChange,
            onSubmit = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            isRetrievalMode = true,
        )
    } else {
        // Regular recovery key input
        val keyHasSpace = state.formattedRecoveryKey.orEmpty().contains(" ")
        val recoveryKeyVisualTransformation = remember(keyHasSpace) {
            // Do not apply a visual transformation if the key has spaces, to let user enter passphrase
            if (keyHasSpace) VisualTransformation.None else RecoveryKeyVisualTransformation()
        }
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.recoveryKey)
                .autofill(
                    autofillTypes = listOf(AutofillType.Password),
                    onFill = { onChange(it) },
                ),
            minLines = 2,
            value = state.formattedRecoveryKey.orEmpty(),
            onValueChange = onChange,
            enabled = state.inProgress.not(),
            visualTransformation = recoveryKeyVisualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            placeholder = stringResource(id = R.string.screen_recovery_key_confirm_key_placeholder),
        )
    }
}

@Composable
private fun RecoveryKeyFooter(state: RecoveryKeyViewState) {
    when (state.recoveryKeyUserStory) {
        RecoveryKeyUserStory.Setup,
        RecoveryKeyUserStory.Change -> {
            if (state.formattedRecoveryKey == null) {
                Text(
                    text = stringResource(
                        id = if (state.recoveryKeyUserStory == RecoveryKeyUserStory.Change) {
                            R.string.screen_recovery_key_change_generate_key_description
                        } else {
                            R.string.screen_recovery_key_setup_generate_key_description
                        }
                    ),
                    color = if(ElementTheme.isLightTheme){
                        Color(0xFF0A8741)
                    }
                    else {
                        Color(0xFFFFFFFF)
                         },
//                    color = ElementTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 16.dp),
                    style = ElementTheme.typography.fontBodySmRegular,
                )
            } else {
                Text(
//                    text = stringResource(id = R.string.screen_recovery_key_save_key_description),
                    text = stringResource(id = R.string.screen_recovery_key_change_generate_key_description),
//                    color = ElementTheme.colors.textSecondary,
                    color = if(ElementTheme.isLightTheme){
                        Color(0xFF0A8741)
                    }
                    else {
                        Color(0xFFFFFFFF)
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    style = ElementTheme.typography.fontBodySmRegular,
                )
            }
        }
        RecoveryKeyUserStory.Enter -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.screen_recovery_key_setup_generate_key_description),
                    color = if(ElementTheme.isLightTheme){
                        Color(0xFF0A8741)
                    }
                    else {
                        Color(0xFFFFFFFF)
                    },
                    modifier = Modifier.padding(start = 16.dp),
                    textAlign = TextAlign.Start,
                    style = ElementTheme.typography.fontBodySmRegular,
                )
            }
        }
    }
}

@Composable
private fun PassphraseInput(
    passphrase: String,
    onPassphraseChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    isRetrievalMode: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(
                id = if (isRetrievalMode) {
                    R.string.screen_recovery_key_vault_hint_input
                } else {
                    R.string.screen_recovery_key_vault_passphrase_hint
                }
            ),
            modifier = Modifier.padding(start = 8.dp),
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
        )
        TextField(
            value = passphrase,
            onValueChange = onPassphraseChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(id = R.string.screen_recovery_key_vault_passphrase_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit?.invoke() }
            ),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun RecoveryKeyViewPreview(
    @PreviewParameter(RecoveryKeyViewStateProvider::class) state: RecoveryKeyViewState
) = ElementPreview {
    RecoveryKeyView(
        state = state,
        onClick = {},
        onChange = {},
        onSubmit = {},
    )
}
