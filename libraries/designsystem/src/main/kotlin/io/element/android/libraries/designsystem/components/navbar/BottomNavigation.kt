package io.element.android.libraries.designsystem.components.navbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur

enum class BottomNavRoute {
    Home,
    Group,
    Settings
}

data class BottomNavItem(
    val route: BottomNavRoute,
    val iconResId: Int,
    val label: String
)

@Composable
fun BottomNavBar(
    currentRoute: BottomNavRoute,
    onRouteSelected: (BottomNavRoute) -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 25.dp)
                .background(
                    if(ElementTheme.isLightTheme)
                        Color.White.copy(alpha = 0.99f)
                    else
                        Color(0xFF11181C).copy(alpha = 0.99f)
                )
        )

        NavigationBar(
            modifier = Modifier.graphicsLayer(alpha = 0.99f),
            tonalElevation = 0.dp,
            containerColor = Color.Transparent
        ) {
            BottomNavRoute.values().forEach { route ->
                val selected = route == currentRoute
                val selectedColor = Color(0xFF0A8741)

                NavigationBarItem(
                    selected = selected,
                    onClick = { onRouteSelected(route) },
                    icon = {
                        Icon(
                            imageVector = when (route) {
                                BottomNavRoute.Home -> ImageVector.vectorResource(id = R.drawable.ic_home_nav)
                                BottomNavRoute.Group -> ImageVector.vectorResource(id = R.drawable.ic_grp_nav)
                                BottomNavRoute.Settings -> ImageVector.vectorResource(id = R.drawable.ic_settings_nav)
                            },
                            contentDescription = route.name,
                            tint = if (selected) selectedColor else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Text(
                            text = route.name,
                            color = if (selected) selectedColor else Color.Gray,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun BottomNavBarPreview() = ElementPreview {
    BottomNavBar(
        currentRoute = BottomNavRoute.Home,
        onRouteSelected = {}
    )
}
