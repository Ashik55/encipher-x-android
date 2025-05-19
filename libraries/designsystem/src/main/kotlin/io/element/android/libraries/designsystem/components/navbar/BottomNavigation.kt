package io.element.android.libraries.designsystem.components.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.R
import io.element.android.libraries.designsystem.atomic.atoms.RedIndicatorAtom
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon

enum class BottomNavRoute {
    Chats,
    Calls,
    Settings
}

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    currentRoute: BottomNavRoute,
    onRouteSelect: (BottomNavRoute) -> Unit,
    showSettingsIndicator: Boolean = false
) {
    val isDarkTheme = !ElementTheme.isLightTheme

    val backgroundColor = if (isDarkTheme) Color(0xFF1A2329) else Color(0xFFF8F9FA)
    val primaryColor = Color(0xFF0A8741)
    val unselectedColor = Color(0xFF8E9294)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavRoute.values().forEach { route ->
                val selected = route == currentRoute

                val iconColor = if (selected) primaryColor else unselectedColor
                val textColor = if (selected) Color.Black else unselectedColor

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .clickable(
                            onClick = { onRouteSelect(route) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pill-shaped background for selected item
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(width = 60.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(
                                        if (isDarkTheme) primaryColor.copy(alpha = 0.12f)
                                        else primaryColor.copy(alpha = 0.08f)
                                    )
                            )
                        }

                        // Icon
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (route) {
                                    BottomNavRoute.Chats -> ImageVector.vectorResource(
                                        id = if (selected) R.drawable.message_circle else R.drawable.message_circle
                                    )
                                    BottomNavRoute.Calls -> ImageVector.vectorResource(
                                        id = if (selected) R.drawable.phone else R.drawable.phone
                                    )
                                    BottomNavRoute.Settings -> ImageVector.vectorResource(
                                        id = if (selected) R.drawable.cog else R.drawable.cog
                                    )
                                },
                                contentDescription = route.name,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )

                            // Red indicator for Settings
                            if (route == BottomNavRoute.Settings && showSettingsIndicator) {
                                RedIndicatorAtom(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-3).dp)
                                        .size(8.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = route.name,
                        modifier = Modifier.height(16.dp),
                        color = textColor,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun BottomNavBarPreview() = ElementPreview {
    BottomNavBar(
        currentRoute = BottomNavRoute.Chats,
        onRouteSelect = {}
    )
}

@PreviewsDayNight
@Composable
internal fun BottomNavBarWithIndicatorPreview() = ElementPreview {
    BottomNavBar(
        currentRoute = BottomNavRoute.Settings,
        onRouteSelect = {},
        showSettingsIndicator = true
    )
}

