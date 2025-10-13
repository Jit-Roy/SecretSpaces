package com.secretspaces32.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavDestination {
    HOME, MAP, CREATE, TRENDS, PROFILE
}

@Composable
fun BottomNavigationBar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Black,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            NavItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                isSelected = currentDestination == NavDestination.HOME,
                onClick = { onNavigate(NavDestination.HOME) }
            )

            // Map
            NavItem(
                icon = Icons.Outlined.LocationOn,
                label = "Map",
                isSelected = currentDestination == NavDestination.MAP,
                onClick = { onNavigate(NavDestination.MAP) }
            )

            // Create (Center FAB)
            IconButton(
                onClick = { onNavigate(NavDestination.CREATE) },
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Circle with plus icon
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer circle
                        Icon(
                            imageVector = Icons.Outlined.AddCircle,
                            contentDescription = "Create",
                            tint = if (currentDestination == NavDestination.CREATE) Color(0xFFE85D75) else Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            // Trends
            NavItem(
                icon = Icons.Outlined.TrendingUp,
                label = "Trends",
                isSelected = currentDestination == NavDestination.TRENDS,
                onClick = { onNavigate(NavDestination.TRENDS) }
            )

            // Profile
            NavItem(
                icon = Icons.Outlined.Person,
                label = "You",
                isSelected = currentDestination == NavDestination.PROFILE,
                onClick = { onNavigate(NavDestination.PROFILE) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) Color(0xFFE85D75) else Color.White
    val textColor = if (isSelected) Color.White else Color.White

    IconButton(
        onClick = onClick,
        modifier = modifier.size(64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )

            // Active indicator
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(6.dp)
                        .background(Color(0xFFE85D75), CircleShape)
                )
            }
        }
    }
}
