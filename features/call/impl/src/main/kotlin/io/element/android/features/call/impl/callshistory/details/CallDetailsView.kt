/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory.details

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.features.call.impl.callshistory.Call
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.CustomProgressIndicator
import io.element.android.libraries.designsystem.R as DSR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailsView(
    state: CallDetailsState,
    onBackClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callsListState by state.callsList.collectAsState()
    val currentUserId by state.currentUserId.collectAsState()
    val hasMoreToLoad by state.hasMoreToLoad.collectAsState()
    val isLoadingMore by state.isLoadingMore.collectAsState()
    val initialCall = state.initialCall

    // Extract contact info from the initial call
    val contactInfo = extractContactInfo(initialCall, currentUserId)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // User info card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Avatar
                Avatar(
                    avatarData = AvatarData(
                        id = contactInfo.userId ?: contactInfo.roomId,
                        name = contactInfo.displayName,
                        url = contactInfo.avatarUrl,
                        size = AvatarSize.UserHeader
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Display Name
                Text(
                    text = contactInfo.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // User ID or Room ID (smaller text)
                Text(
                    text = contactInfo.userId ?: contactInfo.roomId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons row
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Message button
                    ActionButton(
                        icon = ImageVector.vectorResource(
                            id = DSR.drawable.ic_home_nav
                        ),
                        label = "Message",
                        onClick = onMessageClick
                    )
                    
                    // Audio call button
                    ActionButton(
                        icon = ImageVector.vectorResource(
                            id = DSR.drawable.ic_call
                        ),
                        label = "Audio",
                        onClick = onAudioCallClick
                    )
                    
                    // Video call button  
                    ActionButton(
                       icon = ImageVector.vectorResource(
                            id = DSR.drawable.ic_video_call
                        ),
                        label = "Video",
                        onClick = onVideoCallClick
                    )
                }
            }
            
            HorizontalDivider()
            
            // Call history section - using dynamic data from state
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Call History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                when (callsListState) {
                    is AsyncData.Loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CustomProgressIndicator()
                        }
                    }
                    
                    is AsyncData.Failure -> {
                        val error = (callsListState as AsyncData.Failure).error
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Failed to load calls",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = error.message ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    is AsyncData.Success -> {
                        val calls = (callsListState as AsyncData.Success<List<Call>>).data
                        if (calls.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "No call history found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            CallHistoryList(
                                calls = calls,
                                currentUserId = currentUserId,
                                hasMoreToLoad = hasMoreToLoad,
                                isLoadingMore = isLoadingMore,
                                onLoadMore = { state.eventSink(CallDetailsEvents.LoadMore) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    AsyncData.Uninitialized -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CustomProgressIndicator()
                        }
                    }
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
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    
    // Check if we should trigger loading more data
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            val lastItemIndex = lastVisibleItem?.index ?: 0
            val totalItemsCount = calls.size
            
            // If we're close to the end of the list and there's more data to load
            hasMoreToLoad && lastItemIndex >= totalItemsCount - 3
        }
    }
    
    // Trigger the load more event when we're close to the end of the list
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore) {
            onLoadMore()
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        itemsIndexed(calls) { _, call ->
            CallDetailItem(
                call = call,
                currentUserId = currentUserId,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Loading indicator at the bottom when loading more items
        if (isLoadingMore) {
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

// Data class for contact information
private data class ContactInfo(
    val displayName: String,
    val avatarUrl: String?,
    val roomId: String,
    val userId: String?
)

// Helper function to extract contact info from a call
private fun extractContactInfo(call: Call, currentUserId: String?): ContactInfo {
    val isOutgoingCall = currentUserId == call.caller_user_id
    
    return if (call.room_name != null) {
        // This is a room call
        ContactInfo(
            displayName = call.room_name,
            avatarUrl = call.room_avatar,
            roomId = call.room_id ?: "",
            userId = null
        )
    } else if (isOutgoingCall) {
        // This is an outgoing direct call
        val receiverName = call.receiver_display_names?.values?.joinToString(", ") ?: "Unknown"
        val receiverId = call.receiver_user_ids?.firstOrNull()
        val avatar = call.receiver_avatars?.values?.firstOrNull()
        
        ContactInfo(
            displayName = receiverName,
            avatarUrl = avatar,
            roomId = call.room_id ?: "",
            userId = receiverId
        )
    } else {
        // This is an incoming direct call
        ContactInfo(
            displayName = call.caller_display_name ?: "Unknown",
            avatarUrl = call.caller_avatar,
            roomId = call.room_id ?: "",
            userId = call.caller_user_id
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CallDetailItem(
    call: Call,
    currentUserId: String?,
    modifier: Modifier = Modifier
) {
    val isOutgoingCall = currentUserId?.isNotEmpty() == true && call.caller_user_id == currentUserId
    val (icon, contentDescription) = if (isOutgoingCall) {
        Pair(DSR.drawable.ic_call_outgoing, "Outgoing call")
    } else {
        Pair(DSR.drawable.ic_call_incoming, "Incoming call")
    }
    
    val dateTimeString = formatDateTime(call.created_ts)
    val durationString = if (call.ended_ts != null) {
        calculateDuration(call.created_ts, call.ended_ts)
    } else {
        "Ongoing call"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Call type icon (audio/video)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(
                    id = if (call.call_type == "audio") {
                        DSR.drawable.ic_call
                    } else {
                        DSR.drawable.ic_video_call
                    }
                ),
                contentDescription = if (call.call_type == "audio") "Audio call" else "Video call",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Direction and datetime
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(16.dp),
                    tint = if (call.ended_ts == null) Color(0xFF0A8741) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = dateTimeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Duration (or status)
            Text(
                text = durationString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Format the timestamp string to a readable date/time in the device's local timezone.
 */
private fun formatDateTime(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return "Unknown time"
    
    try {
        // Parse the timestamp: "2025-04-15T09:56:12.505007"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Assume input is in UTC
        
        // First try to parse with milliseconds
        val utcDate = try {
            inputFormat.parse(timestamp)
        } catch (e: Exception) {
            // If that fails, try without milliseconds
            try {
                val simpleFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                simpleFormat.timeZone = TimeZone.getTimeZone("UTC")
                simpleFormat.parse(timestamp.substring(0, Math.min(19, timestamp.length)))
            } catch (e2: Exception) {
                return "Invalid date"
            }
        } ?: return "Invalid date"
        
        // Use the device's local timezone
        val deviceTimeZone = TimeZone.getDefault()
        
        // Get current time in device timezone
        val now = Date()
        val deviceCalendar = java.util.Calendar.getInstance(deviceTimeZone)
        deviceCalendar.time = now
        
        // Convert the timestamp to device's local time
        val timestampCalendar = java.util.Calendar.getInstance(deviceTimeZone)
        timestampCalendar.time = utcDate
        
        return when {
            // Today - show just the time
            isSameDay(timestampCalendar, deviceCalendar) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).apply { 
                    timeZone = deviceTimeZone 
                }.format(utcDate)
            }
            // Yesterday
            isYesterday(timestampCalendar, deviceCalendar) -> {
                "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
                    timeZone = deviceTimeZone
                }.format(utcDate)
            }
            // Within same week
            isSameWeek(timestampCalendar, deviceCalendar) -> {
                SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).apply {
                    timeZone = deviceTimeZone
                }.format(utcDate)
            }
            // Same year
            timestampCalendar.get(java.util.Calendar.YEAR) == deviceCalendar.get(java.util.Calendar.YEAR) -> {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).apply {
                    timeZone = deviceTimeZone
                }.format(utcDate)
            }
            // Different year
            else -> {
                SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).apply {
                    timeZone = deviceTimeZone
                }.format(utcDate)
            }
        }
    } catch (e: Exception) {
        return "Unknown time"
    }
}

/**
 * Calculate duration between two timestamps.
 */
private fun calculateDuration(startTime: String?, endTime: String?): String {
    if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty()) return "Unknown duration"
    
    try {
        // First try to parse with milliseconds
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        
        val start = try {
            inputFormat.parse(startTime)
        } catch (e: Exception) {
            // If that fails, try without milliseconds
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(startTime.substring(0, Math.min(19, startTime.length)))
            } catch (e2: Exception) {
                return "Invalid duration"
            }
        } ?: return "Invalid duration"
        
        val end = try {
            inputFormat.parse(endTime)
        } catch (e: Exception) {
            // If that fails, try without milliseconds
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(endTime.substring(0, Math.min(19, endTime.length)))
            } catch (e2: Exception) {
                return "Invalid duration"
            }
        } ?: return "Invalid duration"
        
        val durationMillis = end.time - start.time
        val seconds = durationMillis / 1000
        
        return when {
            seconds < 60 -> "$seconds sec"
            seconds < 3600 -> "${seconds / 60} min ${seconds % 60} sec"
            else -> "${seconds / 3600} hr ${(seconds % 3600) / 60} min"
        }
    } catch (e: Exception) {
        return "Unknown duration"
    }
}

/**
 * Check if two dates are on the same day.
 */
private fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

/**
 * Check if the first date is yesterday compared to the second date.
 */
private fun isYesterday(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    val yesterday = java.util.Calendar.getInstance()
    yesterday.timeInMillis = cal2.timeInMillis
    yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1)
    
    return isSameDay(cal1, yesterday)
}

/**
 * Check if two dates are in the same week.
 */
private fun isSameWeek(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.WEEK_OF_YEAR) == cal2.get(java.util.Calendar.WEEK_OF_YEAR)
}

@PreviewsDayNight
@Composable
internal fun CallDetailsViewPreview() = ElementPreview {
    val mockCall = Call(
        call_id = 421,
        caller_user_id = "@friend:dev.enciph-er.com",
        room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
        call_type = "audio",
        created_ts = "2025-04-23T08:32:21.675023",
        ended_ts = null,
        caller_display_name = "Friend",
        room_name = null,
        room_avatar = null,
        caller_avatar = "mxc://dev.enciph-er.com/vhWvelzQGRAFmvJaUYtHlZQE",
        is_caller = false,
        receiver_user_ids = listOf("@ben12:dev.enciph-er.com"),
        receiver_display_names = mapOf("@ben12:dev.enciph-er.com" to "Ben 12"),
        receiver_avatars = mapOf("@ben12:dev.enciph-er.com" to "mxc://dev.enciph-er.com/LAlzadbHFqpxvuzXpWiIWbYW")
    )
    
    val mockCalls = listOf(
        mockCall,
        Call(
            call_id = 416,
            caller_user_id = "@ben12:dev.enciph-er.com",
            room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
            call_type = "audio",
            created_ts = "2025-04-22T13:28:33.425072",
            ended_ts = "2025-04-22T13:35:27.231343",
            caller_display_name = "Ben 12",
            room_name = null,
            room_avatar = null,
            caller_avatar = "mxc://dev.enciph-er.com/LAlzadbHFqpxvuzXpWiIWbYW",
            is_caller = true,
            receiver_user_ids = listOf("@friend:dev.enciph-er.com"),
            receiver_display_names = mapOf("@friend:dev.enciph-er.com" to "Friend"),
            receiver_avatars = mapOf("@friend:dev.enciph-er.com" to "mxc://dev.enciph-er.com/vhWvelzQGRAFmvJaUYtHlZQE")
        )
    )
    
    val mockState = object : CallDetailsState {
        override val callsList = kotlinx.coroutines.flow.MutableStateFlow(
            io.element.android.libraries.architecture.AsyncData.Success(mockCalls)
        )
        override val currentUserId = kotlinx.coroutines.flow.MutableStateFlow("@ben12:dev.enciph-er.com")
        override val hasMoreToLoad = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val isLoadingMore = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val initialCall = mockCall
        override val eventSink: (CallDetailsEvents) -> Unit = {}
    }
    
    CallDetailsView(
        state = mockState,
        onBackClick = {},
        onMessageClick = {},
        onAudioCallClick = {},
        onVideoCallClick = {}
    )
}


