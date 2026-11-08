package com.chargeroute.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chargeroute.app.ChargeRouteApp
import com.chargeroute.app.core.theme.ThemeMode
import com.chargeroute.app.presentation.components.OfflineBanner
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
import com.chargeroute.app.presentation.screens.sidebar.SidebarDrawer
import com.chargeroute.app.presentation.screens.chargers.UsedChargersScreen
import com.chargeroute.app.presentation.screens.chargers.UsedChargersViewModel
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
            container.geocodeAddressUseCase,
            container.tripRepository
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
            container.updateBatteryUseCase,
            container.locationClient,
            container.getNavigationStepsUseCase
        )
    }

    val savedTripsViewModel: SavedTripsViewModel = viewModel {
        SavedTripsViewModel(container.tripRepository)
    }

    val usedChargersViewModel: UsedChargersViewModel = viewModel {
        UsedChargersViewModel(container.tripRepository)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                val savedTrips by savedTripsViewModel.savedTrips.collectAsState()
                // Cada vez que se abre el menu se actualiza la cantidad de viajes
                LaunchedEffect(drawerState.isOpen) {
                    if (drawerState.isOpen) savedTripsViewModel.refresh()
                }
                SidebarDrawer(
                    userName = "Sofía Martín",
                    userEmail = "sofia.martin@gmail.com",
                    savedTripsCount = savedTrips.size,
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = onThemeModeChange,
                    // El inicio es la pantalla del auto: se vuelve a ella sin apilar copias
                    onNavigateToPlan = {
                        navController.popBackStack(Screen.CarSetup.route, inclusive = false)
                    },
                    onNavigateToSavedTrips = {
                        navController.navigate(Screen.SavedTrips.route)
                    },
                    onNavigateToChargers = {
                        navController.navigate(Screen.UsedChargers.route)
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
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
        // RNF01: aviso de "Sin conexión" arriba de cualquier pantalla, en tiempo real
        val isOnline by remember { container.networkObserver.observeIsOnline() }
            .collectAsStateWithLifecycle(initialValue = true)

        Column(modifier = Modifier.fillMaxSize()) {
        OfflineBanner(visible = !isOnline)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // Con el aviso visible, la barra de estado ya queda cubierta por el aviso
                .then(if (!isOnline) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Login.route
            ) {
                // 1. Login
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            // Sin volver al login con "atrás"; para eso está Cerrar sesión
                            navController.navigate(Screen.CarSetup.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
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
                        onMenuClick = { scope.launch { drawerState.open() } },
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
                        onOriginTextChange = { routePlanningViewModel.updateOriginInput(it) },
                        onDestinationTextChange = { routePlanningViewModel.updateDestinationInput(it) },
                        onPreviewClick = { routePlanningViewModel.previewRoute() },
                        onRecentDestinationClick = { routePlanningViewModel.selectRecentDestination(it) },
                        onAddressOptionClick = { routePlanningViewModel.selectAddressOption(it) },
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
                    // Solo una vez: llamarlo directo recargaba la pantalla (y el mapa) todo el tiempo
                    LaunchedEffect(tripId) {
                        routeSummaryViewModel.loadTrip(tripId)
                    }
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
                        onBackClick = { navController.popBackStack() },
                        isOffline = !isOnline
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
                            // La proxima parada; si ya se pasaron todas, la ultima
                            val stopId = state.nextStop?.id
                                ?: state.trip?.routePlan?.stops?.lastOrNull()?.id
                                ?: "stop_1"
                            navController.navigate(Screen.StationDetail.createRoute(tripId, stopId))
                        },
                        onBatteryUpdated = { newPercent ->
                            activeNavigationViewModel.updateBattery(newPercent)
                        },
                        onToggleSimulation = {
                            activeNavigationViewModel.toggleSimulation()
                        },
                        onToggleGps = {
                            activeNavigationViewModel.toggleGpsTracking()
                        },
                        onLeaveScreen = {
                            activeNavigationViewModel.stopSimulation()
                            activeNavigationViewModel.stopGpsTracking()
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

                // Cargadores donde se paro en los viajes guardados
                composable(Screen.UsedChargers.route) {
                    UsedChargersScreen(
                        viewModel = usedChargersViewModel,
                        onChargerClick = { charger ->
                            routeSummaryViewModel.loadTrip(charger.lastTripId)
                            navController.navigate(
                                Screen.StationDetail.createRoute(charger.lastTripId, charger.lastStopId)
                            )
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
        }
    }
}
