package com.chargeroute.app.presentation.screens.summary

import android.content.res.Configuration
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeroute.app.R
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BatteryOrange
import com.chargeroute.app.core.theme.BatteryRed
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.core.theme.WarningTextRed
import com.chargeroute.app.domain.model.BatteryLevels
import com.chargeroute.app.domain.model.DataFreshness
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.presentation.components.RouteMapView
import com.chargeroute.app.presentation.components.StepProgressIndicator
import com.chargeroute.app.presentation.components.TouchAccessibleCard
import com.chargeroute.app.presentation.components.formatDuration
import com.chargeroute.app.presentation.preview.PreviewData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSummaryScreen(
    state: RouteSummaryUiState,
    onStopClick: (TripStop) -> Unit,
    onStartTripClick: () -> Unit,
    onBackClick: () -> Unit,
    isOffline: Boolean = false
) {
    val trip = state.trip
    val plan = trip?.routePlan

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp) // RNF02
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
                        currentStep = 3,
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
            SummaryBottomBar(
                plan = plan,
                isOffline = isOffline,
                onStartTripClick = onStartTripClick
            )
        }
    ) { padding ->
        if (state.isLoading || plan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.summary_ready_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mapa
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    RouteMapView(
                        routePlan = plan,
                        onStationClick = { station ->
                            val stop = plan.stops.firstOrNull { it.station.id == station.id }
                            if (stop != null) onStopClick(stop)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta con destino y metricas del viaje
                TripMetricsCard(
                    plan = plan,
                    destinationName = trip.destination.name,
                    destinationAddress = trip.destination.address
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Aviso si con lo planificado no se llega con carga suficiente
                if (plan.arrivalBatteryPercent < 10) {
                    LowBatteryWarning(hasStops = plan.stops.isNotEmpty())
                }

                // Lista de paradas planificadas
                Text(
                    text = stringResource(R.string.summary_planned_stops),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                plan.stops.forEach { stop ->
                    PlannedStopItem(stop = stop, onClick = { onStopClick(stop) })
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Barra de abajo: boton de iniciar viaje y avisos del plan
@Composable
private fun SummaryBottomBar(
    plan: RoutePlan?,
    isOffline: Boolean,
    onStartTripClick: () -> Unit
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
            Button(
                onClick = onStartTripClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // RNF02
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.summary_start_trip),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Offline First: sin conexion se avisa desde cuando estan guardados los cargadores
            val stationsDate = plan?.let { DataFreshness.oldestStationUpdate(it) }
            if (isOffline && stationsDate != null) {
                val stale = DataFreshness.isStale(stationsDate)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        if (stale) R.string.summary_offline_saved_stale else R.string.summary_offline_saved,
                        DataFreshness.formatDate(stationsDate)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (stale) BatteryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.summary_illustrative_plan,
                    plan?.departureTime ?: "--:--",
                    String.format(Locale.US, "%.1f", plan?.consumptionKwhPer100Km ?: 15.5)
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Tarjeta con destino, cantidad de cargas y metricas del viaje
@Composable
private fun TripMetricsCard(
    plan: RoutePlan,
    destinationName: String,
    destinationAddress: String
) {
    TouchAccessibleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destinationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Direccion que encontro el mapa, para confirmar que es la correcta
                    Text(
                        text = destinationAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.summary_arrival_time, plan.arrivalTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Badge: "1 carga"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BatteryGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = BatteryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (plan.stops.size == 1) {
                            stringResource(R.string.summary_one_charge)
                        } else {
                            stringResource(R.string.summary_charges, plan.stops.size)
                        },
                        color = BatteryGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila de metricas: distancia | tiempo | bateria al llegar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.summary_distance_value, plan.distanceKm.toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.summary_distance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = formatDuration(plan.totalDurationMinutes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.summary_total_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.summary_battery_percent, plan.arrivalBatteryPercent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.arrivalBatteryPercent >= BatteryLevels.LOW_PERCENT) BatteryGreen else BatteryRed
                    )
                    Text(
                        text = stringResource(R.string.summary_battery_at_arrival),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val driveTime = formatDuration(plan.driveDurationMinutes)
            val chargeTime = formatDuration(plan.chargeDurationMinutes)
            Text(
                text = if (plan.detourKm > 0) {
                    stringResource(R.string.summary_trip_breakdown_detour, driveTime, chargeTime, plan.detourKm)
                } else {
                    stringResource(R.string.summary_trip_breakdown, driveTime, chargeTime)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Aviso de que con lo planificado no se llega con reserva
@Composable
private fun LowBatteryWarning(hasStops: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BatteryRed.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Text(
            text = if (hasStops) {
                stringResource(R.string.summary_warning_one_stop)
            } else {
                stringResource(R.string.summary_warning_no_stops)
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = WarningTextRed,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// Una parada de carga de la lista
@Composable
private fun PlannedStopItem(stop: TripStop, onClick: () -> Unit) {
    TouchAccessibleCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EvStation,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stop.station.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.summary_stop_times,
                            stop.arrivalTime,
                            stop.departureTime,
                            stop.stopDurationMinutes
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.summary_stop_battery,
                            stop.arrivalBatteryPercent,
                            stop.departureBatteryPercent
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.summary_stop_detail),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RouteSummaryScreenPreview() {
    ChargeRouteTheme(darkTheme = false) {
        RouteSummaryScreen(
            state = RouteSummaryUiState(trip = PreviewData.trip),
            onStopClick = {},
            onStartTripClick = {},
            onBackClick = {},
            isOffline = false
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RouteSummaryScreenDarkPreview() {
    ChargeRouteTheme(darkTheme = true) {
        RouteSummaryScreen(
            state = RouteSummaryUiState(trip = PreviewData.trip),
            onStopClick = {},
            onStartTripClick = {},
            onBackClick = {},
            isOffline = false
        )
    }
}
