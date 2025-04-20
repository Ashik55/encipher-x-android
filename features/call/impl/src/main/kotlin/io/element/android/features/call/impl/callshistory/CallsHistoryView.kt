/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.navbar.BottomNavBar
import io.element.android.libraries.designsystem.components.navbar.BottomNavRoute
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Scaffold as ElementScaffold
import io.element.android.libraries.designsystem.R as DSR
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsHistoryView(
    state: CallsHistoryState,
    onRoomDetailsClick: (roomId: String) -> Unit,
    currentRoute: BottomNavRoute,
    onRouteSelect: (BottomNavRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val callsListState by state.callsList.collectAsState()
    
    ElementScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onRouteSelect = onRouteSelect
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            TextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ElementTheme.colors.bgSubtleSecondary,
                    unfocusedContainerColor = ElementTheme.colors.bgSubtleSecondary,
                    disabledContainerColor = ElementTheme.colors.bgSubtleSecondary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = DSR.drawable.ic_search),
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Recent calls section
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            when (callsListState) {
                is AsyncData.Loading -> {
                    // Show loading state
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is AsyncData.Failure -> {
                    // Show error state with retry button
                    val error = (callsListState as AsyncData.Failure)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = error.error.message ?: "Unknown error occurred",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.padding(8.dp))
                            Button(
                                text = "Retry",
                                onClick = { /* Trigger retry */ },
                                size = ButtonSize.Medium
                            )
                        }
                    }
                }
                
                is AsyncData.Success -> {
                    // Show call list
                    val calls = (callsListState as AsyncData.Success<List<Call>>).data
                    if (calls.isEmpty()) {
                        // Empty state
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "No calls in your history",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn {
                            items(calls) { call ->
                                CallItem(
                                    call = call,
                                    onItemClick = { /* Handle call click */ },
                                    onInfoClick = { call.room_id?.let { onRoomDetailsClick(it) } }
                                )
                            }
                        }
                    }
                }
                
                AsyncData.Uninitialized -> {
                    // Handle uninitialized state (this could be similar to loading or empty state)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun CallItem(
    call: Call,
    onItemClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Avatar(
            avatarData = AvatarData(
                id = call.caller_user_id ?: "",
                name = extractNameFromUserId(call.caller_user_id),
                url = null,
                size = AvatarSize.CallList
            )
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = extractNameFromUserId(call.caller_user_id),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = formatDate(call.created_ts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call type icon (outgoing, incoming)
                val (icon, contentDescription) = when {
                    call.caller_user_id?.contains("@ben5") == true -> {
                        if (call.call_type == "video") {
                            Pair(DSR.drawable.ic_video_call_outgoing, "Outgoing video call")
                        } else {
                            Pair(DSR.drawable.ic_call_outgoing, "Outgoing call")
                        }
                    }
                    else -> {
                        Pair(DSR.drawable.ic_call_incoming, "Incoming call")
                    }
                }
                
                Icon(
                    imageVector = ImageVector.vectorResource(id = icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(16.dp),
                    tint = if (call.ended_ts == null) ElementTheme.colors.iconPrimary else Color.Red
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = if (call.call_type == "audio") "Audio" else "Video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Info button
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = ImageVector.vectorResource(id = DSR.drawable.ic_info_circle),
                contentDescription = "Call Info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun extractNameFromUserId(userId: String?): String {
    if (userId.isNullOrEmpty()) return "Unknown"
    
    val username = userId.substringAfter("@").substringBefore(":")
    
    return username.replaceFirstChar { it.uppercase() }
}

private fun formatDate(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return ""
    
    return "Yesterday"
}

@PreviewsDayNight
@Composable
internal fun CallsHistoryViewPreview() = ElementPreview {
    val previewState = object : CallsHistoryState {
        override val callsList = MutableStateFlow(AsyncData.Success(listOf(
            Call(
                call_id = 1,
                call_type = "audio",
                caller_user_id = "@ben5:dev.enciph-er.com",
                created_ts = "2025-04-15T09:56:12.505007",
                ended_ts = null,
                room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com"
            ),
            Call(
                call_id = 2,
                call_type = "video",
                caller_user_id = "@john:dev.enciph-er.com",
                created_ts = "2025-04-14T08:30:00.000000",
                ended_ts = "2025-04-14T08:35:00.000000",
                room_id = "!ABCDEFGHIjklmnop:dev.enciph-er.com"
            )
        )))
        override val favorites = MutableStateFlow<List<Call>>(emptyList())
    }
    
    CallsHistoryView(
        state = previewState,
        onRoomDetailsClick = {},
        currentRoute = BottomNavRoute.Calls,
        onRouteSelect = {}
    )
}
