package com.chargeroute.app.presentation.screens.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.presentation.components.ErrorRetryBanner
import com.chargeroute.app.presentation.components.OfflineIndicatorBadge
import com.chargeroute.app.presentation.components.RouteMapView
import com.chargeroute.app.presentation.components.StepProgressIndicator
import com.chargeroute.app.presentation.components.TouchAccessibleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    state: RoutePlanningUiState,
    cities: List<ArgentineCity>,
    onOriginTextChange: (String) -> Unit,
    onDestinationTextChange: (String) -> Unit,
    onSelectOriginCity: (ArgentineCity) -> Unit,
    onSelectDestinationCity: (ArgentineCity) -> Unit,
    onSwapClick: () -> Unit,
    onGpsClick: () -> Unit,
    onCalculateClick: () -> Unit,
    onBackClick: () -> Unit,
    onOpenSavedRoute: () -> Unit
) {
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
                            contentDescription = "Atrás",
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
                            text = state.vehicle?.let { "${it.brand} ${it.model} • ${it.usableBatteryKwh.toInt()} kWh" } ?: "Vehículo",
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
                                text = "Batería: ${state.initialBatteryPercent}%",
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
                                    text = "Calcular ruta y paradas de carga",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Calculamos las paradas necesarias y el consumo exacto.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OfflineIndicatorBadge(
                isOffline = state.isOffline,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            state.errorMessage?.let { msg ->
                ErrorRetryBanner(
                    errorMessage = msg,
                    onRetry = onCalculateClick,
                    onOpenSavedRoute = onOpenSavedRoute,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            state.infoNotice?.let { notice ->
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

            Text(
                text = "¿A dónde vamos?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ingresá origen y destino o elegí una de las ciudades sugeridas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Origen y Destino editables
            TouchAccessibleCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Campo Origen
                    Text(
                        text = "Punto de Partida (Origen)",
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
                            IconButton(onClick = onGpsClick) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Usar GPS actual",
                                    tint = BrandPrimary
                                )
                            }
                        },
                        placeholder = { Text("Ej: Buenos Aires, Obelisco") },
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
                                contentDescription = "Intercambiar origen y destino",
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
                        text = "Punto de Llegada (Destino)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.destinationInputText,
                        onValueChange = onDestinationTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        placeholder = { Text("Ej: Mar del Plata, Dolores, Pinamar") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = state.destination.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chips rápidos de destinos populares en Argentina
            Text(
                text = "Destinos frecuentes en Argentina",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cities.forEach { city ->
                    val isSelected = state.destination.name.contains(city.name, ignoreCase = true) ||
                            state.destinationInputText.contains(city.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDestinationCity(city) },
                        label = { Text(city.name) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = BrandPrimary
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = BrandPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mapa interactivo de la ruta preliminar
            Text(
                text = "Vista previa del mapa de ruta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                RouteMapView(
                    routePlan = state.calculatedPlan ?: com.chargeroute.app.domain.model.RoutePlan(
                        origin = state.origin,
                        destination = state.destination,
                        distanceKm = 404.0,
                        totalDurationMinutes = 295,
                        driveDurationMinutes = 260,
                        chargeDurationMinutes = 35,
                        stops = emptyList(),
                        polylinePoints = listOf(
                            Pair(state.origin.latitude, state.origin.longitude),
                            Pair(-36.3132, -57.6792), // Dolores
                            Pair(state.destination.latitude, state.destination.longitude)
                        ),
                        encodedPolyline = "",
                        arrivalBatteryPercent = 25,
                        consumptionKwhPer100Km = 15.5,
                        departureTime = "09:41",
                        arrivalTime = "14:36",
                        isLowBatteryWarning = false,
                        warningMessage = null
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
