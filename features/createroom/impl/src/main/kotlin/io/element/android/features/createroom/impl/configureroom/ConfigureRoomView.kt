/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.createroom.impl.configureroom

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.createroom.impl.R
import io.element.android.libraries.designsystem.atomic.atoms.RoundedIconAtom
import io.element.android.libraries.designsystem.atomic.atoms.RoundedIconAtomSize
import io.element.android.libraries.designsystem.components.LabelledTextField
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.async.AsyncActionViewDefaults
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.modifiers.clearFocusOnTap
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.OutlinedTextField
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.ui.components.AvatarActionBottomSheet
import io.element.android.libraries.matrix.ui.components.SelectedUsersRowList
import io.element.android.libraries.matrix.ui.components.UnsavedAvatar
import io.element.android.libraries.permissions.api.PermissionsView
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun ConfigureRoomView(
    state: ConfigureRoomState,
    onBackClick: () -> Unit,
    onCreateRoomSuccess: (RoomId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isAvatarActionsSheetVisible = remember { mutableStateOf(false) }

    fun onAvatarClick() {
        focusManager.clearFocus()
        isAvatarActionsSheetVisible.value = true
    }

    Scaffold(
        modifier = modifier.clearFocusOnTap(focusManager),
        topBar = {
            ConfigureRoomToolbar(
                isNextActionEnabled = state.config.isValid,
                onBackClick = onBackClick,
                onNextClick = {
                    focusManager.clearFocus()
                    state.eventSink(ConfigureRoomEvents.CreateRoom)
                },
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(top =8.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .consumeWindowInsets(padding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                RoomNameWithAvatar(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    avatarUri = state.config.avatarUri,
                    roomName = state.config.roomName.orEmpty(),
                    onAvatarClick = ::onAvatarClick,
                    onChangeRoomName = { state.eventSink(ConfigureRoomEvents.RoomNameChanged(it)) },
                )
                RoomTopic(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    topic = state.config.topic.orEmpty(),
                    onTopicChange = { state.eventSink(ConfigureRoomEvents.TopicChanged(it)) },
                )
                if (state.config.invites.isNotEmpty()) {
                    SelectedUsersRowList(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        selectedUsers = state.config.invites,
                        onUserRemove = {
                            focusManager.clearFocus()
                            state.eventSink(ConfigureRoomEvents.RemoveUserFromSelection(it))
                        },
                    )
                }
                RoomVisibilityOptions(
                    selected = when (state.config.roomVisibility) {
                        is RoomVisibilityState.Private -> RoomVisibilityItem.Private
                        is RoomVisibilityState.Public -> RoomVisibilityItem.Public
                    },
                    onOptionClick = {
                        focusManager.clearFocus()
                        state.eventSink(ConfigureRoomEvents.RoomVisibilityChanged(it))
                    },
                )
                if (state.config.roomVisibility is RoomVisibilityState.Public && state.isKnockFeatureEnabled) {
                    RoomAccessOptions(
                        selected = when (state.config.roomVisibility.roomAccess) {
                            RoomAccess.Anyone -> RoomAccessItem.Anyone
                            RoomAccess.Knocking -> RoomAccessItem.AskToJoin
                        },
                        onOptionClick = {
                            focusManager.clearFocus()
                            state.eventSink(ConfigureRoomEvents.RoomAccessChanged(it))
                        },
                    )
                    RoomAddressField(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        address = state.config.roomVisibility.roomAddress,
                        homeserverName = state.homeserverName,
                        onAddressChange = { state.eventSink(ConfigureRoomEvents.RoomAddressChanged(it)) },
                    )
                }
            }
        }

        AvatarActionBottomSheet(
            actions = state.avatarActions,
            isVisible = isAvatarActionsSheetVisible.value,
            onDismiss = { isAvatarActionsSheetVisible.value = false },
            onSelectAction = { state.eventSink(ConfigureRoomEvents.HandleAvatarAction(it)) }
        )

        AsyncActionView(
            async = state.createRoomAction,
            progressDialog = {
                AsyncActionViewDefaults.ProgressDialog(
                    progressText = stringResource(CommonStrings.common_creating_room),
                )
            },
            onSuccess = { onCreateRoomSuccess(it) },
            errorMessage = { stringResource(R.string.screen_create_room_error_creating_room) },
            onRetry = { state.eventSink(ConfigureRoomEvents.CreateRoom) },
            onErrorDismiss = { state.eventSink(ConfigureRoomEvents.CancelCreateRoom) },
        )

        PermissionsView(
            state = state.cameraPermissionState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigureRoomToolbar(
    isNextActionEnabled: Boolean,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.screen_create_room_title),
                style = ElementTheme.typography.aliasScreenTitle,
            )
        },
        navigationIcon = { BackButton(onClick = onBackClick) },
        // TopAppBar background transparent
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        actions = {
            Button(
                text = stringResource(CommonStrings.action_create),
                enabled = isNextActionEnabled,
                onClick = onNextClick,
                size = ButtonSize.Small,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    )
}

@Composable
private fun RoomNameWithAvatar(
    avatarUri: Uri?,
    roomName: String,
    onAvatarClick: () -> Unit,
    onChangeRoomName: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnsavedAvatar(
            avatarUri = avatarUri,
            modifier = Modifier.clickable(onClick = onAvatarClick),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            OutlinedTextField(
                value = roomName,
                onValueChange = onChangeRoomName,
                placeholder = { Text(stringResource(CommonStrings.common_room_name_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
//        LabelledTextField(
//            label = stringResource(R.string.screen_create_room_room_name_label),
//            value = roomName,
//            placeholder = stringResource(CommonStrings.common_room_name_placeholder),
//            singleLine = true,
//            onValueChange = onChangeRoomName,
//        )
    }
}

@Composable
private fun RoomTopic(
    topic: String,
    onTopicChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        OutlinedTextField(
            value = topic,
            onValueChange = onTopicChange,
            placeholder = { Text(stringResource(CommonStrings.common_topic_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(32.dp),
        )
    }
}


@Composable
private fun ConfigureRoomOptions(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        content()
    }
}

@Composable
private fun RoomVisibilityOptions(
    selected: RoomVisibilityItem,
    onOptionClick: (RoomVisibilityItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfigureRoomOptions(
        title = stringResource(R.string.screen_create_room_room_visibility_section_title),
        modifier = modifier,
    ) {
        RoomVisibilityItem.entries.forEach { item ->
            val isSelected = item == selected
            ListItem(
                leadingContent = ListItemContent.Custom {
                    RoundedIconAtom(
                        size = RoundedIconAtomSize.Big,
                        resourceId = item.icon,
                        tint = if (isSelected) ElementTheme.colors.iconPrimary else ElementTheme.colors.iconSecondary,
                    )
                },
                headlineContent = { Text(text = stringResource(item.title)) },
                supportingContent = { Text(text = stringResource(item.description)) },
                trailingContent = ListItemContent.RadioButton(selected = isSelected),
                onClick = { onOptionClick(item) },
            )
        }
    }
}

@Composable
private fun RoomAccessOptions(
    selected: RoomAccessItem,
    onOptionClick: (RoomAccessItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfigureRoomOptions(
        title = stringResource(R.string.screen_create_room_room_access_section_header),
        modifier = modifier,
    ) {
        RoomAccessItem.entries.forEach { item ->
            ListItem(
                headlineContent = { Text(text = stringResource(item.title)) },
                supportingContent = { Text(text = stringResource(item.description)) },
                trailingContent = ListItemContent.RadioButton(selected = item == selected),
                onClick = { onOptionClick(item) },
            )
        }
    }
}

@Composable
private fun RoomAddressField(
    address: RoomAddress,
    homeserverName: String,
    onAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = MaterialTheme.colorScheme.primary,
            text = stringResource(R.string.screen_create_room_room_address_section_title),
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = address.value,
            leadingIcon = {
                Text(
                    text = "#",
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            trailingIcon = {
                Text(
                    text = homeserverName,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            supportingText = {
                Text(
                    text = stringResource(R.string.screen_create_room_room_address_section_footer),
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            onValueChange = onAddressChange,
            singleLine = true,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ConfigureRoomViewPreview(@PreviewParameter(ConfigureRoomStateProvider::class) state: ConfigureRoomState) = ElementPreview {
    ConfigureRoomView(
        state = state,
        onBackClick = {},
        onCreateRoomSuccess = {},
    )
}
