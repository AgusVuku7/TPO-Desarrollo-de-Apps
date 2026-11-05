package com.chargeroute.app.presentation.screens.planning

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.presentation.components.ErrorRetryBanner
import com.chargeroute.app.presentation.components.OfflineIndicatorBadge
import com.chargeroute.app.presentation.components.RouteMapView
import com.chargeroute.app.presentation.components.StepProgressIndicator
import com.chargeroute.app.presentation.components.TouchAccessibleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    state: RoutePlanningUiState,
    onOriginTextChange: (String) -> Unit,
    onDestinationTextChange: (String) -> Unit,
    onSwapClick: () -> Unit,
    onGpsClick: () -> Unit,
    onCalculateClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onRecentDestinationClick: (Trip) -> Unit,
    onAddressOptionClick: (LocationPoint) -> Unit,
    onBackClick: () -> Unit,
    onOpenSavedRoute: () -> Unit
) {
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    var originMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.addressOptions) {
        if (state.addressOptions.isNotEmpty()) {
            if (state.optionsAreForOrigin) {
                originMenuExpanded = true
            } else {
                destinationMenuExpanded = true
            }
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
                text = "Ingresá cualquier dirección. Si hay calles iguales en otras ciudades, agregá la ciudad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Origen y Destino editables
            // Card Origen y Destino editables con Autocompletado Tipo Waze y Doble Verificación
            TouchAccessibleCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Campo Origen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Punto de Partida (Origen)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.originVerified) Icons.Default.CheckCircle else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (state.originVerified) BatteryGreen else BrandPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (state.originVerified) "Doble Verificación OK" else "Buscando...",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (state.originVerified) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedTextField(
                            value = state.originInputText,
                            onValueChange = { newText ->
                                onOriginTextChange(newText)
                                originMenuExpanded = true
                            },
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
                                Row {
                                    IconButton(onClick = { originMenuExpanded = !originMenuExpanded }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Ver opciones de origen",
                                            tint = BrandPrimary
                                        )
                                    }
                                    IconButton(onClick = onGpsClick) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Usar GPS actual",
                                            tint = BrandPrimary
                                        )
                                    }
                                }
                            },
                            placeholder = { Text("Ej: Del Comercio 988, CABA") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (state.originVerified) BatteryGreen else BrandPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        DropdownMenu(
                            expanded = originMenuExpanded && (state.addressOptions.isNotEmpty() && state.optionsAreForOrigin || state.originInputText.isNotBlank()),
                            onDismissRequest = { originMenuExpanded = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            if (state.addressOptions.isNotEmpty() && state.optionsAreForOrigin) {
                                DropdownMenuItem(
                                    text = { Text("Doble Verificación - Ubicaciones encontradas:", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                                    enabled = false,
                                    onClick = {}
                                )
                                state.addressOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.address, style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandPrimary)
                                        },
                                        onClick = {
                                            originMenuExpanded = false
                                            onAddressOptionClick(option)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = if (state.originVerified) "✓ Coordenadas verificadas: ${state.origin.address}" else state.origin.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (state.originVerified) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Punto de Llegada (Destino)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.destinationVerified) Icons.Default.CheckCircle else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (state.destinationVerified) BatteryGreen else Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (state.destinationVerified) "Doble Verificación OK" else "Buscando...",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (state.destinationVerified) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedTextField(
                            value = state.destinationInputText,
                            onValueChange = { newText ->
                                onDestinationTextChange(newText)
                                destinationMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { destinationMenuExpanded = !destinationMenuExpanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Ver opciones de destino",
                                        tint = BrandPrimary
                                    )
                                }
                            },
                            placeholder = { Text("Ej: UADE, Pinamar, Güemes 2800...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (state.destinationVerified) BatteryGreen else BrandPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        // Desplegable con opciones de ubicación, últimos destinos e historial
                        DropdownMenu(
                            expanded = destinationMenuExpanded,
                            onDismissRequest = { destinationMenuExpanded = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            if (state.addressOptions.isNotEmpty() && !state.optionsAreForOrigin) {
                                DropdownMenuItem(
                                    text = { Text("Doble Verificación - Ubicaciones encontradas:", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                                    enabled = false,
                                    onClick = {}
                                )
                                state.addressOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.address, style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandPrimary)
                                        },
                                        onClick = {
                                            destinationMenuExpanded = false
                                            onAddressOptionClick(option)
                                        }
                                    )
                                }
                            }
                            if (state.recentTrips.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Destinos recientes:", fontWeight = FontWeight.Bold) },
                                    enabled = false,
                                    onClick = {}
                                )
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
                            }
                            DropdownMenuItem(
                                text = { Text("Ver historial", fontWeight = FontWeight.Bold, color = BrandPrimary) },
                                onClick = {
                                    destinationMenuExpanded = false
                                    onOpenSavedRoute()
                                }
                            )
                        }
                    }
                    Text(
                        text = if (state.destinationVerified) "✓ Coordenadas verificadas: ${state.destination.address}" else state.destination.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (state.destinationVerified) BatteryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Si la dirección existe en varias ciudades, el usuario elige cuál es
            if (state.addressOptions.isNotEmpty()) {
                Text(
                    text = if (state.optionsAreForOrigin) "Doble Verificación - ¿Cuál es tu origen exacto?" else "Doble Verificación - ¿Cuál es tu destino exacto?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                state.addressOptions.forEach { option ->
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

            // Mapa interactivo de la ruta preliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vista previa del mapa de ruta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onPreviewClick,
                    enabled = !state.isLoading
                ) {
                    Text("Ver en el mapa")
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
                                text = if (state.isLoading) "Buscando la ruta..." else "Tocá \"Ver en el mapa\"",
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
                if (plan.isApproximate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ruta aproximada: no se pudo consultar el camino real. Tocá \"Ver en el mapa\" para reintentar.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB91C1C)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (plan.stops.isEmpty()) {
                            "${plan.distanceKm.toInt()} km • sin paradas de carga"
                        } else {
                            "${plan.distanceKm.toInt()} km • parada en " + plan.stops.joinToString { it.station.name }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
