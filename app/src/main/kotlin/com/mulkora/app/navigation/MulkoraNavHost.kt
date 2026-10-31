package com.mulkora.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mulkora.app.navigation.TopLevelDestination

@Composable
fun MulkoraNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom bar should be shown
    val showBottomBar = currentDestination?.route in TopLevelDestination.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MulkoraBottomBar(
                    destinations = TopLevelDestination.entries,
                    currentDestination = currentDestination,
                    onNavigateToDestination = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth graph
            authGraph(navController)

            // Onboarding
            onboardingGraph(navController)

            // Main graphs (shown after login)
            feedGraph(navController)
            exploreGraph(navController)
            postCreateGraph(navController)
            notificationsGraph(navController)
            profileGraph(navController)

            // Additional feature graphs
            referencesGraph(navController)
            contractsGraph(navController)
            settingsGraph(navController)
        }
    }
}