package com.chargeroute.app.presentation.screens.arrival

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.core.theme.RouteBlue
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.presentation.components.formatDuration
import com.chargeroute.app.presentation.components.TouchAccessibleCard
import com.chargeroute.app.presentation.preview.PreviewData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalScreen(
    trip: Trip?,
    onPlanNewTripClick: () -> Unit,
    onViewSummaryClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val plan = trip?.routePlan

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.arrival_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                actions = {
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(48.dp) // RNF02
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ArrivalBottomBar(
                onPlanNewTripClick = onPlanNewTripClick,
                onViewSummaryClick = onViewSummaryClick
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
            // Tarjeta principal de llegada
            ArrivalHeroCard()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(
                    R.string.arrival_greeting,
                    trip?.destination?.name?.split(",")?.firstOrNull() ?: "Mar del Plata"
                ),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.arrival_subtitle,
                    trip?.destination?.address ?: "Plaza Colón"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta con los resultados del viaje
            TripResultsCard(plan = plan)

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta de bateria restante
            BatteryRemainingCard(batteryRemaining = plan?.arrivalBatteryPercent ?: 22)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Barra de abajo con las dos acciones de la pantalla
@Composable
private fun ArrivalBottomBar(
    onPlanNewTripClick: () -> Unit,
    onViewSummaryClick: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlanNewTripClick,
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
                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.arrival_plan_new_trip),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = onViewSummaryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp), // RNF02
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = stringResource(R.string.arrival_view_summary),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Text(
                text = stringResource(R.string.arrival_illustrative),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Tarjeta azul de arriba con el check de "Llegaste a destino"
@Composable
private fun ArrivalHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(RouteBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BatteryGreen)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.arrival_reached),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Tarjeta con distancia, tiempo, paradas y energia usada
@Composable
private fun TripResultsCard(plan: RoutePlan?) {
    TouchAccessibleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.arrival_distance_value, plan?.distanceKm?.toInt() ?: 414),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.arrival_traveled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = formatDuration(plan?.totalDurationMinutes ?: 0),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.arrival_total_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = "${plan?.stops?.size ?: 1}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.arrival_charge_stop),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Energia usada = distancia x consumo del viaje

            val usedKwh = plan?.let { it.distanceKm * it.consumptionKwhPer100Km / 100.0 } ?: 0.0
            Text(
                text = stringResource(
                    R.string.arrival_trip_breakdown,
                    formatDuration(plan?.driveDurationMinutes ?: 0),
                    formatDuration(plan?.chargeDurationMinutes ?: 0),
                    String.format(Locale.US, "%.1f", usedKwh)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Tarjeta con el porcentaje de bateria que queda al llegar
@Composable
private fun BatteryRemainingCard(batteryRemaining: Int) {
    TouchAccessibleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.arrival_battery_remaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.arrival_battery_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.arrival_battery_percent, batteryRemaining),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BatteryGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { batteryRemaining / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BatteryGreen,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArrivalScreenPreview() {
    ChargeRouteTheme(darkTheme = false) {
        ArrivalScreen(
            trip = PreviewData.trip,
            onPlanNewTripClick = {},
            onViewSummaryClick = {},
            onCloseClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ArrivalScreenDarkPreview() {
    ChargeRouteTheme(darkTheme = true) {
        ArrivalScreen(
            trip = PreviewData.trip,
            onPlanNewTripClick = {},
            onViewSummaryClick = {},
            onCloseClick = {}
        )
    }
}
