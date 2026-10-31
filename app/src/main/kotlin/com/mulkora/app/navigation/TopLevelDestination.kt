package com.mulkora.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Alt navigasyon çubuğundaki 5 ana sekme.
 * CLAUDE.md Bölüm 5: Ana Navigasyon
 */
enum class TopLevelDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: String,    // Gerçek projede R.string.xxx kullanılır
    val badgeCount: Int = 0
) {
    FEED(
        route = AppRoute.Feed.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        labelResId = "Ana Akış"
    ),
    EXPLORE(
        route = AppRoute.Explore.route,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        labelResId = "Keşfet"
    ),
    POST_CREATE(
        route = AppRoute.PostCreate.route,
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Outlined.Add,
        labelResId = "Paylaş"
    ),
    NOTIFICATIONS(
        route = AppRoute.Notifications.route,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        labelResId = "Bildirimler"
    ),
    PROFILE(
        route = AppRoute.Profile.route,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        labelResId = "Profil"
    )
}