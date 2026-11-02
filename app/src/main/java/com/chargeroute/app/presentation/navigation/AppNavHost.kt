package com.chargeroute.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chargeroute.app.ChargeRouteApp
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.presentation.screens.arrival.ArrivalScreen
import com.chargeroute.app.presentation.screens.carsetup.CarSetupScreen
import com.chargeroute.app.presentation.screens.carsetup.CarSetupViewModel
import com.chargeroute.app.presentation.screens.login.LoginScreen
import com.chargeroute.app.presentation.screens.navigation.ActiveNavigationScreen
import com.chargeroute.app.presentation.screens.navigation.ActiveNavigationViewModel
import com.chargeroute.app.presentation.screens.planning.RoutePlanningScreen
import com.chargeroute.app.presentation.screens.planning.RoutePlanningViewModel
import com.chargeroute.app.presentation.screens.savedtrips.SavedTripsScreen
import com.chargeroute.app.presentation.screens.savedtrips.SavedTripsViewModel
import com.chargeroute.app.presentation.screens.settings.SettingsScreen
import com.chargeroute.app.presentation.screens.sidebar.SidebarDrawer
import com.chargeroute.app.presentation.screens.station.StationDetailScreen
import com.chargeroute.app.presentation.screens.summary.RouteSummaryScreen
import com.chargeroute.app.presentation.screens.summary.RouteSummaryViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val container = ChargeRouteApp.instance.container

    val carSetupViewModel: CarSetupViewModel = viewModel {
        CarSetupViewModel(container.getVehicleCatalogUseCase)
    }

    val routePlanningViewModel: RoutePlanningViewModel = viewModel {
        RoutePlanningViewModel(
            container.calculateRouteUseCase,
            container.saveTripUseCase,
            container.getOfflineRouteUseCase,
            container.locationClient,
            container.networkObserver
        )
    }

    val routeSummaryViewModel: RouteSummaryViewModel = viewModel {
        RouteSummaryViewModel(
            container.tripRepository,
            container.getOfflineRouteUseCase
        )
    }

    val activeNavigationViewModel: ActiveNavigationViewModel = viewModel {
        ActiveNavigationViewModel(
            container.tripRepository,
            container.updateBatteryUseCase
        )
    }

    val savedTripsViewModel: SavedTripsViewModel = viewModel {
        SavedTripsViewModel(container.tripRepository)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                val carSetupState by carSetupViewModel.uiState.collectAsState()
                val vehicleName = carSetupState.selectedVehicle?.displayName ?: "Kona Electric • 64 kWh"
                SidebarDrawer(
                    userName = "Sofía Martín",
                    userEmail = "sofia.martin@gmail.com",
                    carName = vehicleName,
                    savedTripsCount = 1,
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = onThemeModeChange,
                    onNavigateToPlan = {
                        navController.navigate(Screen.CarSetup.route)
                    },
                    onNavigateToMyCar = {
                        navController.navigate(Screen.CarSetup.route)
                    },
                    onNavigateToSavedTrips = {
                        navController.navigate(Screen.SavedTrips.route)
                    },
                    onNavigateToChargers = {
                        navController.navigate(Screen.SavedTrips.route)
                    },
                    onNavigateToPreferences = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Login.route
            ) {
                // 1. Login
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Screen.CarSetup.route)
                        }
                    )
                }

                // 2. Car Setup (Paso 1 de 3)
                composable(Screen.CarSetup.route) {
                    val state by carSetupViewModel.uiState.collectAsState()
                    CarSetupScreen(
                        state = state,
                        onVehicleSelected = { carSetupViewModel.selectVehicle(it) },
                        onBatteryChanged = { carSetupViewModel.updateBatteryPercent(it) },
                        onAcToggled = { carSetupViewModel.toggleAc(it) },
                        onBackClick = { navController.popBackStack() },
                        onNextClick = {
                            state.selectedVehicle?.let { vehicle ->
                                routePlanningViewModel.initParams(
                                    vehicle = vehicle,
                                    batteryPercent = state.initialBatteryPercent,
                                    acActive = state.acActive
                                )
                            }
                            navController.navigate(Screen.RoutePlanning.route)
                        }
                    )
                }

                // 3. Route Planning (Paso 2 de 3)
                composable(Screen.RoutePlanning.route) {
                    val state by routePlanningViewModel.uiState.collectAsState()
                    RoutePlanningScreen(
                        state = state,
                        cities = routePlanningViewModel.argentineCities,
                        onOriginTextChange = { routePlanningViewModel.updateOriginInput(it) },
                        onDestinationTextChange = { routePlanningViewModel.updateDestinationInput(it) },
                        onSelectOriginCity = { routePlanningViewModel.setOriginCity(it) },
                        onSelectDestinationCity = { routePlanningViewModel.setDestinationCity(it) },
                        onSwapClick = { routePlanningViewModel.swapOriginDestination() },
                        onGpsClick = { routePlanningViewModel.useCurrentLocationAsOrigin() },
                        onCalculateClick = {
                            routePlanningViewModel.calculateRoute { tripId ->
                                routeSummaryViewModel.loadTrip(tripId)
                                navController.navigate(Screen.RouteSummary.createRoute(tripId))
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onOpenSavedRoute = {
                            navController.navigate(Screen.SavedTrips.route)
                        }
                    )
                }

                // 4. Route Summary (Paso 3 de 3)
                composable(
                    route = Screen.RouteSummary.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                    routeSummaryViewModel.loadTrip(tripId)
                    val state by routeSummaryViewModel.uiState.collectAsState()

                    RouteSummaryScreen(
                        state = state,
                        onStopClick = { stop ->
                            navController.navigate(Screen.StationDetail.createRoute(tripId, stop.id))
                        },
                        onStartTripClick = {
                            activeNavigationViewModel.loadTrip(tripId)
                            navController.navigate(Screen.ActiveNavigation.createRoute(tripId))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 5. Station Detail
                composable(
                    route = Screen.StationDetail.route,
                    arguments = listOf(
                        navArgument("tripId") { type = NavType.StringType },
                        navArgument("stopId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                    val stopId = backStackEntry.arguments?.getString("stopId") ?: ""
                    val summaryState by routeSummaryViewModel.uiState.collectAsState()
                    val stop = summaryState.trip?.routePlan?.stops?.firstOrNull { it.id == stopId }
                        ?: summaryState.trip?.routePlan?.stops?.firstOrNull()

                    StationDetailScreen(
                        stop = stop,
                        vehicle = summaryState.trip?.vehicle,
                        onUpdateBatteryAfterCharge = { newPercent ->
                            activeNavigationViewModel.updateBattery(newPercent, tripId)
                        },
                        onNavigateToCharger = {
                            activeNavigationViewModel.loadTrip(tripId)
                            navController.navigate(Screen.ActiveNavigation.createRoute(tripId))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 6. Active Navigation (Copiloto)
                composable(
                    route = Screen.ActiveNavigation.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                    val state by activeNavigationViewModel.uiState.collectAsState()

                    ActiveNavigationScreen(
                        state = state,
                        onViewNextChargeClick = {
                            val stopId = state.trip?.routePlan?.stops?.firstOrNull()?.id ?: "stop_1"
                            navController.navigate(Screen.StationDetail.createRoute(tripId, stopId))
                        },
                        onBatteryUpdated = { newPercent ->
                            activeNavigationViewModel.updateBattery(newPercent)
                        },
                        onToggleSimulation = {
                            activeNavigationViewModel.toggleSimulation()
                        },
                        onSimulateAdvance = {
                            activeNavigationViewModel.advanceProgress(0.15f)
                        },
                        onArrival = {
                            navController.navigate(Screen.Arrival.createRoute(tripId))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // 7. Arrival Screen
                composable(
                    route = Screen.Arrival.route,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                    val navState by activeNavigationViewModel.uiState.collectAsState()

                    ArrivalScreen(
                        trip = navState.trip,
                        onPlanNewTripClick = {
                            navController.navigate(Screen.CarSetup.route) {
                                popUpTo(Screen.CarSetup.route) { inclusive = true }
                            }
                        },
                        onViewSummaryClick = {
                            navController.navigate(Screen.RouteSummary.createRoute(tripId))
                        },
                        onCloseClick = {
                            navController.navigate(Screen.CarSetup.route) {
                                popUpTo(Screen.CarSetup.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Saved Trips
                composable(Screen.SavedTrips.route) {
                    SavedTripsScreen(
                        viewModel = savedTripsViewModel,
                        onTripClick = { trip ->
                            routeSummaryViewModel.loadTrip(trip.id)
                            navController.navigate(Screen.RouteSummary.createRoute(trip.id))
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = onThemeModeChange,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
