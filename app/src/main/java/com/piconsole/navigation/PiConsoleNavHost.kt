package com.piconsole.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piconsole.screens.dashboard.DashboardScreen
import com.piconsole.screens.media.MediaScreen
import com.piconsole.screens.memory.MemoryVaultScreen
import com.piconsole.screens.timers.ClockHubScreen
import com.piconsole.viewmodel.DashboardViewModel
import com.piconsole.viewmodel.MediaViewModel
import com.piconsole.viewmodel.ClockViewModel

@Composable
fun PiConsoleNavHost() {
    val navController = rememberNavController()

    val dashboardViewModel: DashboardViewModel = viewModel()
    val clockViewModel: ClockViewModel = viewModel()
    val mediaViewModel: MediaViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    Triple(NavRoutes.Dashboard.route, "Dashboard", Icons.Default.Home),
                    Triple(NavRoutes.Timers.route, "Timers", Icons.Default.DateRange),
                    Triple(NavRoutes.Media.route, "Media", Icons.Default.PlayArrow),
                    Triple(NavRoutes.Memory.route, "Memory", Icons.AutoMirrored.Filled.List)
                )

                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Dashboard.route) { DashboardScreen(dashboardViewModel) }
            composable(NavRoutes.Timers.route) { ClockHubScreen(clockViewModel) }
            composable(NavRoutes.Media.route) { MediaScreen(mediaViewModel) }
            composable(NavRoutes.Memory.route) { MemoryVaultScreen() }
        }
    }
}
