/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight

/**
 * Creates a fake conference UI that mimics the native Jitsi conference interface.
 * This is shown instead of loading screens to provide instant visual feedback.
 */
@Composable
fun FakeConferenceUI(
    isAudioCall: Boolean = false,
    participantName: String = "You",
    roomName: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main conference area
        if (isAudioCall) {
            AudioCallLayout(
                participantName = participantName,
                roomName = roomName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VideoCallLayout(
                participantName = participantName,
                roomName = roomName,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom toolbar
        ConferenceToolbar(
            isAudioCall = isAudioCall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // Top info bar
        TopInfoBar(
            roomName = roomName,
            participantCount = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

@Composable
private fun AudioCallLayout(
    participantName: String,
    roomName: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E3A8A).copy(alpha = 0.3f),
                    Color.Black
                ),
                radius = size.width * 0.8f
            )
            drawRect(gradient)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar with pulse animation
            PulsingAvatar(
                name = participantName,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Participant name
            Text(
                text = participantName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Connection status
            ConnectingIndicator()
        }
    }
}

@Composable
private fun VideoCallLayout(
    participantName: String,
    roomName: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Main video area (placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            // Simulated video placeholder with participant avatar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large avatar for video off state
                PulsingAvatar(
                    name = participantName,
                    size = 100.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = participantName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Small self-video preview (bottom right)
        SelfVideoPreview(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(120.dp, 160.dp)
        )
    }
}

@Composable
private fun PulsingAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha * 0.3f)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    CircleShape
                )
        )
        
        // Main avatar
        Box(
            modifier = Modifier
                .fillMaxSize(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4F46E5),
                            Color(0xFF7C3AED)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                color = Color.White,
                fontSize = (size.value * 0.3f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SelfVideoPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        // Simulated self-preview with camera icon
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = "Camera preview",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ConferenceToolbar(
    isAudioCall: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        ToolbarButton(
            icon = Icons.Default.MicOff,
            backgroundColor = Color(0xFF374151),
            contentDescription = "Mute"
        )

        // Video button (only for video calls)
        if (!isAudioCall) {
            ToolbarButton(
                icon = Icons.Default.VideocamOff,
                backgroundColor = Color(0xFF374151),
                contentDescription = "Camera"
            )
        }

        // End call button
        ToolbarButton(
            icon = Icons.Default.CallEnd,
            backgroundColor = Color(0xFFDC2626),
            contentDescription = "End call"
        )

        // More options
        ToolbarButton(
            icon = Icons.Default.MoreVert,
            backgroundColor = Color(0xFF374151),
            contentDescription = "More options"
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TopInfoBar(
    roomName: String?,
    participantCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connection indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )

            // Room name or participant count
            Text(
                text = roomName ?: "$participantCount participant${if (participantCount != 1) "s" else ""}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ConnectingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "connecting_alpha"
    )

    Text(
        text = "Connecting...",
        color = Color.White.copy(alpha = alpha),
        fontSize = 14.sp,
        fontWeight = FontWeight.Light
    )
}

@PreviewsDayNight
@Composable
internal fun FakeConferenceUIPreviewVideo() = ElementPreview {
    FakeConferenceUI(
        isAudioCall = false,
        participantName = "John Doe",
        roomName = "Team Meeting"
    )
}

@PreviewsDayNight
@Composable
internal fun FakeConferenceUIPreviewAudio() = ElementPreview {
    FakeConferenceUI(
        isAudioCall = true,
        participantName = "Alice Smith",
        roomName = "Quick Call"
    )
}
