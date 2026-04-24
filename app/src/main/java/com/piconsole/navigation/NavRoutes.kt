package com.piconsole.navigation

sealed class NavRoutes(val route: String) {
    object Dashboard : NavRoutes("dashboard")
    object Timers : NavRoutes("timers")
    object Media : NavRoutes("media")
    object Memory : NavRoutes("memory")
}
