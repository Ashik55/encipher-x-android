/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.navbar.BottomNavBar
import io.element.android.libraries.designsystem.components.navbar.BottomNavRoute
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.aliasScreenTitle
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.CustomProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Scaffold as ElementScaffold
import io.element.android.libraries.designsystem.R as DSR
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CallsHistoryView(
    state: CallsHistoryState,
    onRoomDetailsClick: (roomId: String) -> Unit,
    currentRoute: BottomNavRoute,
    onRouteSelect: (BottomNavRoute) -> Unit,
    onStartCall: (roomId: String, isAudioCall: Boolean) -> Unit = { _, _ -> },
    onCallDetailsClick: (call: Call) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    val callsListState by state.callsList.collectAsState()
    val currentUserId by state.currentUserId.collectAsState()
    val hasMoreToLoad by state.hasMoreToLoad.collectAsState()
    val isLoadingMore by state.isLoadingMore.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    
    ElementScaffold(
        modifier = modifier,
        topBar = {
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                isSearchActive = false
                                searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = CompoundIcons.ArrowLeft(),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        )
                        
                        // Clear button when there's text
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Icon(
                                    imageVector = CompoundIcons.Close(),
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Calls",
                            style = ElementTheme.typography.aliasScreenTitle,
                        )
                    },
                    actions = {
                        // Only show search icon when there are calls in history
                        val hasCallsInHistory = callsListState is AsyncData.Success && (callsListState as AsyncData.Success<List<Call>>).data.isNotEmpty()
                        if (hasCallsInHistory) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = CompoundIcons.Search(),
                                    contentDescription = "Search"
                                )
                            }
                        }
                    }
                )
            }
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
                    CustomProgressIndicator()
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
                        val name = if(call.is_caller == true){
                            if(call.room_name == null) getDisplayNamesString(call.receiver_display_names) else call.room_name
                        } else {
                            if(call.room_name == null) call.caller_display_name.toString() else call.room_name
                        }
                        
                        name.contains(searchQuery, ignoreCase = true)
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
                        Column(
                            modifier = Modifier.padding(horizontal = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) 
                                    CompoundIcons.Search()
                                else 
                                    ImageVector.vectorResource(id = DSR.drawable.ic_call),
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = ElementTheme.colors.iconSecondary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (searchQuery.isNotEmpty()) 
                                    "No matching results" 
                                else 
                                    "No calls yet",
                                style = ElementTheme.typography.fontHeadingMdBold,
                                color = ElementTheme.colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (searchQuery.isNotEmpty())
                                    "Try adjusting your search terms"
                                else
                                    "When you make or receive calls, they'll appear here",
                                style = ElementTheme.typography.fontBodyLgRegular,
                                color = ElementTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Button(
                                    text = "Clear search",
                                    onClick = { searchQuery = "" }
                                )
                            }
                        }
                    }
                } else {
                    CallHistoryList(
                        calls = filteredCalls,
                        currentUserId = currentUserId,
                        hasMoreToLoad = hasMoreToLoad,
                        isLoadingMore = isLoadingMore,
                        onItemClick = { call -> call.room_id?.let { onRoomDetailsClick(it) } },
                        onStartCall = onStartCall,
                        onCallDetailsClick = onCallDetailsClick,
                        onLoadMore = { state.eventSink(CallsHistoryEvents.LoadMore) },
                        isSearchActive = isSearchActive,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
            
            AsyncData.Uninitialized -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CustomProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun CallHistoryList(
    calls: List<Call>,
    currentUserId: String?,
    hasMoreToLoad: Boolean,
    isLoadingMore: Boolean,
    onItemClick: (Call) -> Unit,
    onStartCall: (roomId: String, isAudioCall: Boolean) -> Unit,
    onCallDetailsClick: (Call) -> Unit,
    onLoadMore: () -> Unit,
    isSearchActive: Boolean,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    
    // Add scroll detection for pagination - only when not searching
    val reachedEnd by remember {
        derivedStateOf {
            // Don't paginate if we're searching, there are no calls, already loading, or have no more data
            if (isSearchActive || calls.isEmpty() || isLoadingMore || !hasMoreToLoad) {
                false
            } else {
                // Check if we're near the end of the list
                val lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
                
                lastVisibleItemIndex >= totalItemsCount - 3
            }
        }
    }
    
    // Trigger load more when reaching the end - only if not in search mode
    LaunchedEffect(reachedEnd) {
        if (reachedEnd) {
            Timber.d("End of list reached. Loading more calls.")
            onLoadMore()
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        // Recent calls section header (only when not searching)
        if (!isSearchActive) {
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
        items(calls) { call ->
            CallItem(
                call = call,
                currentUserId = currentUserId,
                onItemClick = { onItemClick(call) },
                onStartCall = onStartCall,
                onCallDetailsClick = onCallDetailsClick
            )
        }
        
        // Loading indicator at the bottom when loading more items - only when not searching
        if (isLoadingMore && !isSearchActive) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
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
    onStartCall: (roomId: String, isAudioCall: Boolean) -> Unit = { _, _ -> },
    onCallDetailsClick: (call: Call) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCallDetailsClick(call) }  // Calling onCallDetailsClick with the call object
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
                    text = if(call.is_caller == true){
                        if(call.room_name == null) getDisplayNamesString(call.receiver_display_names) else call.room_name
                    } else {
                        if(call.room_name == null) call.caller_display_name.toString() else call.room_name
                    },
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
                    tint = if (call.ended_ts != null) Color(0xFF0A8741) else Color.Red
                )
                
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = formatDate(call.created_ts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Call type indicator icon (audio/video)
        val callTypeIcon = if (call.call_type == "audio") {
            DSR.drawable.ic_call
        } else {
            DSR.drawable.ic_video_call
        }
        
        Icon(
            imageVector = ImageVector.vectorResource(id = callTypeIcon),
            contentDescription = if (call.call_type == "audio") "Audio call" else "Video call",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp)
                .clickable { onStartCall(call.room_id ?: "", call.call_type == "audio") }
        )
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/*
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
        override val hasMoreToLoad = MutableStateFlow(true)
        override val isLoadingMore = MutableStateFlow(false)
        override val eventSink: (CallsHistoryEvents) -> Unit = {}
    }
    
    CallsHistoryView(
        state = previewState,
        onRoomDetailsClick = {},
        currentRoute = BottomNavRoute.Calls,
        onRouteSelect = {}
    )
}
