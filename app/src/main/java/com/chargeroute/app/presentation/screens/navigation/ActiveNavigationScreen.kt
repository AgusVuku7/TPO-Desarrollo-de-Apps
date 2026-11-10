package com.chargeroute.app.presentation.screens.navigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chargeroute.app.R
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BatteryOrange
import com.chargeroute.app.core.theme.BatteryRed
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.core.theme.NavigationBannerCharging
import com.chargeroute.app.core.theme.NavigationBannerDark
import com.chargeroute.app.domain.model.ReachEstimate
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.presentation.components.BatteryLevelBadge
import com.chargeroute.app.presentation.components.formatDuration
import com.chargeroute.app.presentation.components.RouteMapView
import com.chargeroute.app.presentation.preview.PreviewData

// Modo conductor: el mapa ocupa todo y arriba de el solo va lo necesario para manejar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveNavigationScreen(
    state: ActiveNavigationUiState,
    onViewNextChargeClick: () -> Unit,
    onBatteryUpdated: (Int) -> Unit,
    onToggleSimulation: () -> Unit,
    onToggleGps: () -> Unit,
    onLeaveScreen: () -> Unit,
    onArrival: () -> Unit,
    onBackClick: () -> Unit
) {
    LaunchedEffect(state.isArrived) {
        if (state.isArrived) {
            onArrival()
        }
    }

    // El GPS y la simulacion solo corren mientras se ve esta pantalla
    DisposableEffect(Unit) {
        onDispose { onLeaveScreen() }
    }

    // Proxima parada de carga (o la parada en la que se esta cargando). Null si ya se pasaron todas.
    val nextStop = state.nextStop
    var showBatteryControls by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Mapa a pantalla completa
            RouteMapView(
                routePlan = state.trip?.routePlan,
                activeCarProgress = state.progress,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Cartel de arriba: la proxima indicacion
            ManeuverBanner(
                state = state,
                onBackClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
            )

            // 3. Parte de abajo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Velocidad a la izquierda, botones a la derecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    SpeedIndicator(speedKmH = state.currentSpeedKmH)
                    ActionButtons(
                        state = state,
                        onToggleGps = onToggleGps,
                        onToggleSimulation = onToggleSimulation
                    )
                }

                // Avisos: solo aparecen si hacen falta
                state.gpsNotice?.let { notice ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // RF03: aviso segun si la bateria alcanza para el proximo objetivo
                val reach = state.reachEstimate
                BatteryAlertCard(state = state, reach = reach)

                // Barra de datos del viaje
                TripInfoPanel(
                    state = state,
                    reach = reach,
                    nextStop = nextStop,
                    showBatteryControls = showBatteryControls,
                    onToggleBatteryControls = { showBatteryControls = !showBatteryControls },
                    onBatteryUpdated = onBatteryUpdated,
                    onViewNextChargeClick = onViewNextChargeClick
                )
            }
        }
    }
}

// Cartel de arriba con la proxima indicacion (fondo oscuro a proposito: se lee de un vistazo manejando)
@Composable
private fun ManeuverBanner(
    state: ActiveNavigationUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (state.isCharging) NavigationBannerCharging else NavigationBannerDark,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_exit),
                    tint = Color.White
                )
            }
            Icon(
                imageVector = maneuverIcon(state.maneuverType),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.maneuverDistance,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = state.currentManeuver,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}

// Circulo con la velocidad actual
@Composable
private fun SpeedIndicator(speedKmH: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier
                .size(84.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$speedKmH",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.nav_speed_unit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Botones de la derecha: GPS y simulacion
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButtons(
    state: ActiveNavigationUiState,
    onToggleGps: () -> Unit,
    onToggleSimulation: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // GPS real (solo el icono, para no llenar la pantalla)
        Surface(
            onClick = onToggleGps,
            shape = CircleShape,
            color = if (state.isGpsTracking) BrandPrimary else MaterialTheme.colorScheme.surface,
            shadowElevation = 5.dp
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = if (state.isGpsTracking) {
                        stringResource(R.string.nav_gps_stop)
                    } else {
                        stringResource(R.string.nav_gps_start)
                    },
                    tint = if (state.isGpsTracking) Color.White else BrandPrimary
                )
            }
        }

        // Simular viaje
        Surface(
            onClick = onToggleSimulation,
            shape = RoundedCornerShape(24.dp),
            color = if (state.isSimulationPlaying) BrandPrimary else MaterialTheme.colorScheme.surface,
            shadowElevation = 5.dp
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.isSimulationPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (state.isSimulationPlaying) Color.White else BrandPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.isSimulationPlaying) {
                        stringResource(R.string.nav_pause)
                    } else {
                        stringResource(R.string.nav_simulate)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isSimulationPlaying) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Aviso de bateria (rojo si no alcanza, naranja si llega justo o esta baja). Si no hace falta, no dibuja nada.
@Composable
private fun BatteryAlertCard(state: ActiveNavigationUiState, reach: ReachEstimate?) {
    val alertText = when {
        reach != null && !reach.reaches ->
            stringResource(R.string.nav_alert_cannot_reach, reach.targetName, -reach.arrivalBatteryPercent)
        reach != null && !reach.reachesWithReserve ->
            stringResource(R.string.nav_alert_tight, reach.targetName, reach.arrivalBatteryPercent)
        state.isLowBatteryWarning ->
            stringResource(R.string.nav_alert_low_battery, state.currentBatteryPercent)
        else -> null
    }
    if (alertText != null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (reach != null && !reach.reaches) BatteryRed else BatteryOrange,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = alertText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Barra de datos del viaje: tiempo, distancia, bateria y proximo cargador
@Composable
private fun TripInfoPanel(
    state: ActiveNavigationUiState,
    reach: ReachEstimate?,
    nextStop: TripStop?,
    showBatteryControls: Boolean,
    onToggleBatteryControls: () -> Unit,
    onBatteryUpdated: (Int) -> Unit,
    onViewNextChargeClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val driveMinutes = state.trip?.routePlan?.driveDurationMinutes ?: 0
            val minutesLeft = (driveMinutes * (1f - state.progress)).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDuration(minutesLeft),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(
                            R.string.nav_remaining,
                            state.distanceRemainingTotalKm,
                            state.trip?.destination?.name ?: stringResource(R.string.nav_destination_default)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                // Al tocar la bateria aparecen los botones para ajustarla
                BatteryLevelBadge(
                    percent = state.currentBatteryPercent,
                    modifier = Modifier.clickable { onToggleBatteryControls() }
                )
            }

            // RF03: alcance con la bateria actual (se recalcula al ajustar el %)
            reach?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (it.reaches) {
                        stringResource(R.string.nav_reach_ok, it.targetName, it.arrivalBatteryPercent, it.rangeKm)
                    } else {
                        stringResource(R.string.nav_reach_no, it.targetName, it.kmToTarget, it.rangeKm)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        !it.reaches -> BatteryRed
                        !it.reachesWithReserve -> BatteryOrange
                        else -> BatteryGreen
                    },
                    maxLines = 2
                )
            }

            if (showBatteryControls) {
                Spacer(modifier = Modifier.height(8.dp))
                BatteryControls(
                    batteryPercent = state.currentBatteryPercent,
                    onBatteryUpdated = onBatteryUpdated
                )
            }

            // Proximo cargador: se toca para ver el detalle
            if (nextStop != null || state.isCharging) {
                val stationName = nextStop?.station?.name
                    ?: state.trip?.routePlan?.stops?.firstOrNull()?.station?.name
                    ?: stringResource(R.string.nav_charger_default)
                Spacer(modifier = Modifier.height(8.dp))
                NextChargerRow(
                    stationName = stationName,
                    isCharging = state.isCharging,
                    distanceToNextStopKm = state.distanceToNextStopKm,
                    onClick = onViewNextChargeClick
                )
            }
        }
    }
}

// Botones para ajustar la bateria de a 5%
@Composable
private fun BatteryControls(batteryPercent: Int, onBatteryUpdated: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nav_adjust_battery),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBatteryUpdated(batteryPercent - 5) }) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.nav_battery_less))
            }
            IconButton(onClick = { onBatteryUpdated(batteryPercent + 5) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_battery_more))
            }
        }
    }
}

// Fila del proximo cargador (o aviso de que ya se cargo)
@Composable
private fun NextChargerRow(
    stationName: String,
    isCharging: Boolean,
    distanceToNextStopKm: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp) // RNF02
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(NavigationBannerCharging, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isCharging) {
                stringResource(R.string.nav_charged_hint)
            } else {
                stringResource(R.string.nav_next_charger, stationName, distanceToNextStopKm)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private fun maneuverIcon(type: String): ImageVector {
    return when (type) {
        "left" -> Icons.Default.TurnLeft
        "right" -> Icons.Default.TurnRight
        "slight left" -> Icons.Default.TurnSlightLeft
        "slight right" -> Icons.Default.TurnSlightRight
        "sharp left" -> Icons.Default.TurnSharpLeft
        "sharp right" -> Icons.Default.TurnSharpRight
        "uturn" -> Icons.Default.UTurnLeft
        "roundabout" -> Icons.Default.RoundaboutRight
        "arrive" -> Icons.Default.Flag
        "charging" -> Icons.Default.EvStation
        else -> Icons.Default.Straight
    }
}

private val PreviewState = ActiveNavigationUiState(
    trip = PreviewData.trip,
    progress = 0.3f,
    currentSpeedKmH = 95,
    distanceToNextStopKm = 80,
    distanceRemainingTotalKm = 290,
    currentManeuver = "Seguí por Autovía 2",
    maneuverDistance = "2.4 km"
)

@Preview(showBackground = true)
@Composable
private fun ActiveNavigationScreenPreview() {
    ChargeRouteTheme(darkTheme = false) {
        ActiveNavigationScreen(
            state = PreviewState,
            onViewNextChargeClick = {},
            onBatteryUpdated = {},
            onToggleSimulation = {},
            onToggleGps = {},
            onLeaveScreen = {},
            onArrival = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActiveNavigationScreenDarkPreview() {
    ChargeRouteTheme(darkTheme = true) {
        ActiveNavigationScreen(
            state = PreviewState,
            onViewNextChargeClick = {},
            onBatteryUpdated = {},
            onToggleSimulation = {},
            onToggleGps = {},
            onLeaveScreen = {},
            onArrival = {},
            onBackClick = {}
        )
    }
}
