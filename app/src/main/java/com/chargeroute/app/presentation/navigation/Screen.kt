package com.chargeroute.app.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object CarSetup : Screen("car_setup")
    object RoutePlanning : Screen("route_planning")
    object RouteSummary : Screen("route_summary/{tripId}") {
        fun createRoute(tripId: String) = "route_summary/$tripId"
    }
    object StationDetail : Screen("station_detail/{tripId}/{stopId}") {
        fun createRoute(tripId: String, stopId: String) = "station_detail/$tripId/$stopId"
    }
    object ActiveNavigation : Screen("active_navigation/{tripId}") {
        fun createRoute(tripId: String) = "active_navigation/$tripId"
    }
    object Arrival : Screen("arrival/{tripId}") {
        fun createRoute(tripId: String) = "arrival/$tripId"
    }
    object SavedTrips : Screen("saved_trips")
    object Settings : Screen("settings")
}
