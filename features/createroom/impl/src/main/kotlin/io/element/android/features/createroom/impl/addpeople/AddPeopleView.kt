/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.createroom.impl.addpeople

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.createroom.impl.R
import io.element.android.features.createroom.impl.components.UserListView
import io.element.android.features.createroom.impl.userlist.UserListEvents
import io.element.android.features.createroom.impl.userlist.UserListState
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun AddPeopleView(
    state: UserListState,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localView = LocalView.current

    // Create a nested scroll connection that hides keyboard on scroll
    val keyboardDismissingScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) {
                    localView.hideKeyboard()
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AddPeopleViewTopBar(
                hasSelectedUsers = state.selectedUsers.isNotEmpty(),
                onBackClick = {
                    if (state.isSearchActive) {
                        state.eventSink(UserListEvents.OnSearchActiveChanged(false))
                    } else {
                        onBackClick()
                    }
                },
                onNextClick = onNextClick,
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            Image(
                painter = painterResource(id = R.drawable.bg),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Add keyboard dismissing box
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        localView.hideKeyboard()
                    }
                    .nestedScroll(keyboardDismissingScrollConnection)
            ) {

                // User list content
                UserListView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding),
                    state = state,
                    showBackButton = false,
                    onSelectUser = {},
                    onDeselectUser = {},
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPeopleViewTopBar(
    hasSelectedUsers: Boolean,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.screen_create_room_add_people_title),
                style = ElementTheme.typography.aliasScreenTitle
            )
        },
        navigationIcon = { BackButton(onClick = onBackClick) },
        actions = {
            val textActionResId = if (hasSelectedUsers) CommonStrings.action_next else CommonStrings.action_skip
            Button(
                text = stringResource(id = textActionResId),
                onClick = onNextClick,
                size = ButtonSize.Small,
                modifier = Modifier.padding(end = 16.dp)
            )
        },
        // Transparent background for the TopAppBar
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@PreviewsDayNight
@Composable
internal fun AddPeopleViewPreview(@PreviewParameter(AddPeopleUserListStateProvider::class) state: UserListState) = ElementPreview {
    AddPeopleView(
        state = state,
        onBackClick = {},
        onNextClick = {},
    )
}
