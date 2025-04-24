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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.R as DSR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Define the data classes needed for static dummy data
private data class DummyContactInfo(
    val displayName: String,
    val avatarUrl: String?,
    val roomId: String,
    val userId: String?
)

private data class DummyCall(
    val call_id: Int,
    val caller_user_id: String,
    val room_id: String,
    val call_type: String,
    val created_ts: String,
    val ended_ts: String?,
    val caller_display_name: String?
)

private data class DummyCallsResponse(
    val calls: List<DummyCall>,
    val next_page: String?,
    val prev_page: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailsView(
    state: Any, // Changed from CallDetailsState to Any to avoid reference errors
    onBackClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use static dummy data instead of dynamic data
    val contactInfo = DummyContactInfo(
        displayName = "Ben Wilson",
        avatarUrl = "mxc://dev.enciph-er.com/LAlzadbHFqpxvuzXpWiIWbYW",
        roomId = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
        userId = "@benwilson:dev.enciph-er.com"
    )
    
    // Dummy call history data
    val dummyCalls = listOf(
        DummyCall(
            call_id = 421,
            caller_user_id = "@friend:dev.enciph-er.com",
            room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
            call_type = "audio",
            created_ts = "2025-04-23T08:32:21.675023",
            ended_ts = null,
            caller_display_name = "Friend"
        ),
        DummyCall(
            call_id = 416,
            caller_user_id = "@benwilson:dev.enciph-er.com",
            room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
            call_type = "audio",
            created_ts = "2025-04-22T13:28:33.425072",
            ended_ts = "2025-04-22T13:35:27.231343",
            caller_display_name = "Ben Wilson"
        ),
        DummyCall(
            call_id = 395,
            caller_user_id = "@friend:dev.enciph-er.com",
            room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
            call_type = "video",
            created_ts = "2025-04-19T09:51:27.231343",
            ended_ts = "2025-04-19T10:12:43.645872",
            caller_display_name = "Friend"
        ),
        DummyCall(
            call_id = 394,
            caller_user_id = "@benwilson:dev.enciph-er.com",
            room_id = "!OWEQCyKsMRmxkMVFDT:dev.enciph-er.com",
            call_type = "video",
            created_ts = "2025-04-15T17:21:27.231343",
            ended_ts = "2025-04-15T17:32:13.645872",
            caller_display_name = "Ben Wilson"
        )
    )
    
    val callsResponse = DummyCallsResponse(
        calls = dummyCalls,
        next_page = "10",
        prev_page = null
    )
    
    // Dummy current user ID
    val currentUserId = "@benwilson:dev.enciph-er.com"
    
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
                        icon = Icons.AutoMirrored.Filled.Message,
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
            
            // Call history section - using static data directly
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Call History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(callsResponse.calls) { call ->
                        CallDetailItem(
                            call = call,
                            currentUserId = currentUserId,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
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
    call: DummyCall,
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
        "Missed call"
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
                    tint = if (call.ended_ts == null) Color.Red else Color(0xFF0A8741)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = dateTimeString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Duration (or missed)
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
 * Format the timestamp string to a readable date/time.
 */
private fun formatDateTime(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return "Unknown time"
    
    try {
        // Parse the timestamp: "2025-04-15T09:56:12.505007"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp.substring(0, 19)) ?: return "Invalid date"
        
        // Get current time
        val now = Date()
        
        // Determine the format based on how old the timestamp is
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        
        val currentCalendar = java.util.Calendar.getInstance()
        currentCalendar.time = now
        
        return when {
            // Today - show just the time
            isSameDay(calendar, currentCalendar) -> {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }
            // Yesterday
            isYesterday(calendar, currentCalendar) -> {
                "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            }
            // Within same week
            isSameWeek(calendar, currentCalendar) -> {
                SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(date)
            }
            // Same year
            calendar.get(java.util.Calendar.YEAR) == currentCalendar.get(java.util.Calendar.YEAR) -> {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
            }
            // Different year
            else -> {
                SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(date)
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
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val start = inputFormat.parse(startTime.substring(0, 19)) ?: return "Invalid duration"
        val end = inputFormat.parse(endTime.substring(0, 19)) ?: return "Invalid duration"
        
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
    CallDetailsView(
        state = Any(),
        onBackClick = {},
        onMessageClick = {},
        onAudioCallClick = {},
        onVideoCallClick = {}
    )
}


