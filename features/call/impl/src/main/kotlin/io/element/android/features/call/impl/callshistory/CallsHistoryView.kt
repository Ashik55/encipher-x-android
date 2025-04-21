/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val currentUserId by state.currentUserId.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    ElementScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = !isSearchActive) {
                        Text(
                            text = "Calls",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    AnimatedVisibility(visible = isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            leadingIcon = {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = DSR.drawable.ic_call_incoming),
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                actions = {
                    // Only show search icon when not in search mode
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = DSR.drawable.ic_search),
                                contentDescription = "Search"
                            )
                        }
                    }
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
        when (callsListState) {
            is AsyncData.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is AsyncData.Failure -> {
                val error = (callsListState as AsyncData.Failure)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                val calls = (callsListState as AsyncData.Success<List<Call>>).data
                val filteredCalls = if (searchQuery.isNotEmpty()) {
                    calls.filter { call ->
                        val roomName = call.room_name ?: ""
                        val callerName = call.caller_display_name ?: ""
                        val receiverNames = getDisplayNamesString(call.receiver_display_names)
                        
                        roomName.contains(searchQuery, ignoreCase = true) || 
                        callerName.contains(searchQuery, ignoreCase = true) ||
                        receiverNames.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    calls
                }
                
                if (filteredCalls.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching results found" else "No calls in your history",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Recent calls section header (only when not searching)
                        if (searchQuery.isEmpty()) {
                            item {
                                Text(
                                    text = "Recent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        
                        // Call items
                        items(filteredCalls) { call ->
                            CallItem(
                                call = call,
                                currentUserId = currentUserId,
                                onItemClick = { call.room_id?.let { onRoomDetailsClick(it) } }
                            )
                        }
                    }
                }
            }
            
            AsyncData.Uninitialized -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun CallItem(
    call: Call,
    currentUserId: String?,
    onItemClick: () -> Unit,
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
                id =  if(call.room_name == null) call.receiver_user_ids.toString() else " ",
                name = if(call.is_caller == true){
                    if(call.room_name == null) getDisplayNamesString(call.receiver_display_names) else call.room_name
                } else {
                    if(call.room_name == null) call.caller_display_name.toString() else call.room_name
                },

                url = if(call.is_caller == true){
                    if(call.room_avatar == null) getDisplayNamesString(call.receiver_avatars) else call.room_avatar
                } else {
                    if(call.room_avatar == null) (call.caller_avatar) else call.room_avatar
                },
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
                    text = if(call.room_name == null) getDisplayNamesString(call.receiver_display_names) else call.room_name!!,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call type icon (outgoing, incoming)
                val isOutgoingCall = currentUserId?.isNotEmpty() == true && call.caller_user_id == currentUserId
                val (icon, contentDescription) = if (isOutgoingCall) {
                    Pair(DSR.drawable.ic_call_outgoing, "Outgoing call")
                } else {
                    Pair(DSR.drawable.ic_call_incoming, "Incoming call")
                }
                
                Icon(
                    imageVector = ImageVector.vectorResource(id = icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(16.dp),
                    tint = if (call.ended_ts == null) ElementTheme.colors.iconPrimary else Color.Red
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Show date here instead of call type text
                Text(
                    text = formatDate(call.created_ts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Call type indicator icon (audio/video)
        val callTypeIcon = if (call.call_type == "audio") {
            DSR.drawable.ic_calls_nav
        } else {
            DSR.drawable.ic_video_call_outgoing
        }
        
        Icon(
            imageVector = ImageVector.vectorResource(id = callTypeIcon),
            contentDescription = if (call.call_type == "audio") "Audio call" else "Video call",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp).size(24.dp)
        )
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Helper function to convert receiver display names map to a readable string
 */
private fun getDisplayNamesString(displayNames: Map<String, String>?): String {
    if (displayNames.isNullOrEmpty()) return "Unknown"
    
    return displayNames.values.joinToString(", ")
}

private fun extractNameFromUserId(userId: String?): String {
    if (userId.isNullOrEmpty()) return "Unknown"
    
    // Matrix IDs are in the format "@username:server.com"
    val username = userId.substringAfter("@").substringBefore(":")
    
    // Custom username mapping for known users
    return username.replaceFirstChar { it.uppercase() }
}

private fun formatDate(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return ""
    
    // Example timestamp: "2025-04-15T09:56:12.505007"
    try {
        val date = timestamp.substringBefore("T") // Extract "2025-04-15"
        val parts = date.split("-")
        
        if (parts.size != 3) return "Invalid date"
        
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        
        // Get current date
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar months are 0-based
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        // Calculate difference in days for simple cases
        return when {
            // Today
            year == currentYear && month == currentMonth && day == currentDay -> "Today"
            
            // Yesterday
            isYesterday(year, month, day, currentYear, currentMonth, currentDay) -> "Yesterday"
            
            // Within last week (7 days)
            isWithinLastWeek(year, month, day, currentYear, currentMonth, currentDay) -> {
                val daysAgo = calculateDaysAgo(year, month, day, currentYear, currentMonth, currentDay)
                "$daysAgo days ago"
            }
            
            // This year
            year == currentYear -> {
                // Format as Month Day
                val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${months[month]} $day"
            }
            
            // Previous years
            else -> {
                val months = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${months[month]} $day, $year"
            }
        }
    } catch (e: Exception) {
        return "Unknown date"
    }
}

// Helper function to check if a date is yesterday
private fun isYesterday(year: Int, month: Int, day: Int, currentYear: Int, currentMonth: Int, currentDay: Int): Boolean {
    val yesterday = java.util.Calendar.getInstance().apply {
        set(currentYear, currentMonth - 1, currentDay)
        add(java.util.Calendar.DAY_OF_MONTH, -1)
    }
    return year == yesterday.get(java.util.Calendar.YEAR) && 
           month == yesterday.get(java.util.Calendar.MONTH) + 1 && 
           day == yesterday.get(java.util.Calendar.DAY_OF_MONTH)
}

// Helper function to check if a date is within the last week
private fun isWithinLastWeek(year: Int, month: Int, day: Int, currentYear: Int, currentMonth: Int, currentDay: Int): Boolean {
    val oneWeekAgo = java.util.Calendar.getInstance().apply {
        set(currentYear, currentMonth - 1, currentDay)
        add(java.util.Calendar.DAY_OF_MONTH, -7)
    }
    val dateCalendar = java.util.Calendar.getInstance().apply {
        set(year, month - 1, day)
    }
    return dateCalendar.after(oneWeekAgo) && 
           (year != currentYear || month != currentMonth || day != currentDay) &&
           !isYesterday(year, month, day, currentYear, currentMonth, currentDay)
}

// Helper function to calculate days ago
private fun calculateDaysAgo(year: Int, month: Int, day: Int, currentYear: Int, currentMonth: Int, currentDay: Int): Int {
    val date = java.util.Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val current = java.util.Calendar.getInstance().apply {
        set(currentYear, currentMonth - 1, currentDay, 0, 0, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val diffInMillis = current.timeInMillis - date.timeInMillis
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
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
                room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
                caller_display_name = "Ben 5",
                room_name = null,
                is_caller = true,
                receiver_display_names = mapOf("@ben12:dev.enciph-er.com" to "Ben 12")
            ),
            Call(
                call_id = 2,
                call_type = "video",
                caller_user_id = "@john:dev.enciph-er.com",
                created_ts = "2025-04-14T08:30:00.000000",
                ended_ts = "2025-04-14T08:35:00.000000",
                room_id = "!ABCDEFGHIjklmnop:dev.enciph-er.com",
                room_name = "Team Meeting",
                caller_display_name = "John"
            )
        )))
        override val favorites = MutableStateFlow<List<Call>>(emptyList())
        override val currentUserId = MutableStateFlow("@ben5:dev.enciph-er.com")
    }
    
    CallsHistoryView(
        state = previewState,
        onRoomDetailsClick = {},
        currentRoute = BottomNavRoute.Calls,
        onRouteSelect = {}
    )
}
