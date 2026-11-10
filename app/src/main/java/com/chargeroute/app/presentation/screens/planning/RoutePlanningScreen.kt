package com.chargeroute.app.presentation.screens.planning

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeroute.app.R
import com.chargeroute.app.core.location.LocationPermission
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BatteryRed
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.presentation.components.ErrorRetryBanner
import com.chargeroute.app.presentation.components.RouteMapView
import com.chargeroute.app.presentation.components.StepProgressIndicator
import com.chargeroute.app.presentation.components.TouchAccessibleCard
import com.chargeroute.app.presentation.preview.PreviewData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    state: RoutePlanningUiState,
    onOriginTextChange: (String) -> Unit,
    onDestinationTextChange: (String) -> Unit,
    onSwapClick: () -> Unit,
    onGpsClick: () -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onLocationPermissionDenied: () -> Unit,
    onCalculateClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onRecentDestinationClick: (Trip) -> Unit,
    onAddressOptionClick: (LocationPoint) -> Unit,
    onBackClick: () -> Unit,
    onOpenSavedRoute: () -> Unit
) {
    // Permiso de ubicacion: se pide al entrar (para usar la ubicacion actual como origen)
    // o al tocar el boton de GPS. Si se rechaza, el origen se escribe a mano.
    val context = LocalContext.current
    var permissionAskedFromGpsButton by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        when {
            result.values.none { it } -> onLocationPermissionDenied()
            permissionAskedFromGpsButton -> onGpsClick()
            else -> onLocationPermissionGranted()
        }
        permissionAskedFromGpsButton = false
    }
    LaunchedEffect(Unit) {
        if (LocationPermission.isGranted(context)) {
            onLocationPermissionGranted()
        } else {
            locationPermissionLauncher.launch(LocationPermission.REQUESTED)
        }
    }
    val onGpsButtonClick = {
        if (LocationPermission.isGranted(context)) {
            onGpsClick()
        } else {
            permissionAskedFromGpsButton = true
            locationPermissionLauncher.launch(LocationPermission.REQUESTED)
        }
    }

    // Al entrar se muestra la vista previa con las paradas
    LaunchedEffect(Unit) {
        if (state.calculatedPlan == null) {
            onPreviewClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    StepProgressIndicator(
                        currentStep = 2,
                        totalSteps = 3,
                        modifier = Modifier
                            .width(180.dp)
                            .padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            PlanningBottomBar(
                state = state,
                onCalculateClick = onCalculateClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            state.errorMessage?.let { msg ->
                ErrorRetryBanner(
                    errorMessage = msg,
                    onRetry = onCalculateClick,
                    onOpenSavedRoute = onOpenSavedRoute,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            state.infoNotice?.let { notice ->
                PlanningInfoNotice(notice = notice)
            }

            Text(
                text = stringResource(R.string.planning_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.planning_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlanningRouteCard(
                state = state,
                onOriginTextChange = onOriginTextChange,
                onDestinationTextChange = onDestinationTextChange,
                onSwapClick = onSwapClick,
                onGpsButtonClick = onGpsButtonClick,
                onRecentDestinationClick = onRecentDestinationClick,
                onOpenSavedRoute = onOpenSavedRoute
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Si la dirección existe en varias ciudades, el usuario elige cuál es
            if (state.addressOptions.isNotEmpty()) {
                PlanningAddressOptions(
                    options = state.addressOptions,
                    optionsAreForOrigin = state.optionsAreForOrigin,
                    onAddressOptionClick = onAddressOptionClick
                )
            }

            PlanningMapSection(
                state = state,
                onPreviewClick = onPreviewClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Barra inferior: resumen del vehículo y botón de calcular
@Composable
private fun PlanningBottomBar(
    state: RoutePlanningUiState,
    onCalculateClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Resumen del vehículo seleccionado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.vehicle?.let {
                        stringResource(
                            R.string.planning_vehicle_summary,
                            it.brand,
                            it.model,
                            it.usableBatteryKwh.toInt()
                        )
                    } ?: stringResource(R.string.planning_vehicle_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = BatteryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.planning_battery, state.initialBatteryPercent),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = BatteryGreen
                    )
                }
            }

            Button(
                onClick = onCalculateClick,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.White
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.planning_calculate_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.planning_calculate_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Aviso informativo (texto que viene del ViewModel)
@Composable
private fun PlanningInfoNotice(notice: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BrandPrimary.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Tarjeta con origen y destino editables
@Composable
private fun PlanningRouteCard(
    state: RoutePlanningUiState,
    onOriginTextChange: (String) -> Unit,
    onDestinationTextChange: (String) -> Unit,
    onSwapClick: () -> Unit,
    onGpsButtonClick: () -> Unit,
    onRecentDestinationClick: (Trip) -> Unit,
    onOpenSavedRoute: () -> Unit
) {
    var destinationMenuExpanded by remember { mutableStateOf(false) }

    TouchAccessibleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Campo Origen
            Text(
                text = stringResource(R.string.planning_origin_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.originInputText,
                onValueChange = onOriginTextChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onGpsButtonClick) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = stringResource(R.string.planning_gps_description),
                            tint = BrandPrimary
                        )
                    }
                },
                placeholder = { Text(stringResource(R.string.planning_origin_placeholder)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Text(
                text = state.origin.address,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            // Botón de intercambio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                IconButton(
                    onClick = onSwapClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.planning_swap_description),
                        tint = BrandPrimary
                    )
                }
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }

            // Campo Destino
            Text(
                text = stringResource(R.string.planning_destination_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedTextField(
                    value = state.destinationInputText,
                    onValueChange = onDestinationTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BatteryRed,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { destinationMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.planning_recent_description),
                                tint = BrandPrimary
                            )
                        }
                    },
                    placeholder = { Text(stringResource(R.string.planning_destination_placeholder)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Desplegable con los últimos destinos y el historial
                DropdownMenu(
                    expanded = destinationMenuExpanded,
                    onDismissRequest = { destinationMenuExpanded = false }
                ) {
                    if (state.recentTrips.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.planning_no_trips)) },
                            enabled = false,
                            onClick = {}
                        )
                    }
                    state.recentTrips.forEach { trip ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = trip.destination.name,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = trip.destination.address,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = BrandPrimary
                                )
                            },
                            onClick = {
                                destinationMenuExpanded = false
                                onRecentDestinationClick(trip)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.planning_history),
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        },
                        onClick = {
                            destinationMenuExpanded = false
                            onOpenSavedRoute()
                        }
                    )
                }
            }
            Text(
                text = state.destination.address,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

// Lista de direcciones posibles cuando la búsqueda es ambigua
@Composable
private fun PlanningAddressOptions(
    options: List<LocationPoint>,
    optionsAreForOrigin: Boolean,
    onAddressOptionClick: (LocationPoint) -> Unit
) {
    Text(
        text = stringResource(
            if (optionsAreForOrigin) R.string.planning_options_origin else R.string.planning_options_destination
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
    options.forEach { option ->
        TouchAccessibleCard(
            onClick = { onAddressOptionClick(option) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = option.address,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

// Mapa interactivo de la ruta preliminar y resumen de paradas
@Composable
private fun PlanningMapSection(
    state: RoutePlanningUiState,
    onPreviewClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.planning_map_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(
            onClick = onPreviewClick,
            enabled = !state.isLoading
        ) {
            Text(stringResource(R.string.planning_map_button))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteMapView(
                routePlan = state.calculatedPlan,
                modifier = Modifier.fillMaxSize()
            )

            if (state.calculatedPlan == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = stringResource(
                            if (state.isLoading) R.string.planning_map_loading else R.string.planning_map_hint
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    // Paradas que aparecen en el mapa
    state.calculatedPlan?.let { plan ->
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val distanceKm = plan.distanceKm.toInt()
            Text(
                text = when {
                    plan.stops.isEmpty() ->
                        stringResource(R.string.planning_route_no_stops, distanceKm)
                    plan.detourKm > 0 ->
                        stringResource(
                            R.string.planning_route_with_stops_detour,
                            distanceKm,
                            plan.detourKm,
                            plan.stops.joinToString { it.station.name }
                        )
                    else ->
                        stringResource(
                            R.string.planning_route_with_stops,
                            distanceKm,
                            plan.stops.joinToString { it.station.name }
                        )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutePlanningScreenPreview() {
    ChargeRouteTheme(darkTheme = false) {
        PreviewPlanningScreen()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoutePlanningScreenDarkPreview() {
    ChargeRouteTheme(darkTheme = true) {
        PreviewPlanningScreen()
    }
}

// Pantalla con datos de ejemplo, compartida por las dos previews
@Composable
private fun PreviewPlanningScreen() {
    RoutePlanningScreen(
        state = RoutePlanningUiState(
            vehicle = PreviewData.vehicle,
            calculatedPlan = PreviewData.routePlan,
            recentTrips = listOf(PreviewData.trip)
        ),
        onOriginTextChange = {},
        onDestinationTextChange = {},
        onSwapClick = {},
        onGpsClick = {},
        onLocationPermissionGranted = {},
        onLocationPermissionDenied = {},
        onCalculateClick = {},
        onPreviewClick = {},
        onRecentDestinationClick = {},
        onAddressOptionClick = {},
        onBackClick = {},
        onOpenSavedRoute = {}
    )
}
