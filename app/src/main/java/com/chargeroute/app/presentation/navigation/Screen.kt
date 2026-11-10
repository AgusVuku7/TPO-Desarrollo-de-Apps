package com.chargeroute.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object CarSetup : Screen("car_setup")
    data object RoutePlanning : Screen("route_planning")
    data object RouteSummary : Screen("route_summary/{tripId}") {
        fun createRoute(tripId: String) = "route_summary/$tripId"
    }
    data object StationDetail : Screen("station_detail/{tripId}/{stopId}") {
        fun createRoute(tripId: String, stopId: String) = "station_detail/$tripId/$stopId"
    }
    data object ActiveNavigation : Screen("active_navigation/{tripId}") {
        fun createRoute(tripId: String) = "active_navigation/$tripId"
    }
    data object Arrival : Screen("arrival/{tripId}") {
        fun createRoute(tripId: String) = "arrival/$tripId"
    }
    data object SavedTrips : Screen("saved_trips")
    data object UsedChargers : Screen("used_chargers")

    companion object {
        // Nombres de los argumentos que viajan en las rutas
        const val ARG_TRIP_ID = "tripId"
        const val ARG_STOP_ID = "stopId"
    }
}
