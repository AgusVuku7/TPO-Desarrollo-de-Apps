package com.chargeroute.app.presentation.screens.carsetup

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chargeroute.app.R
import com.chargeroute.app.core.theme.BatteryGreen
import com.chargeroute.app.core.theme.BatteryOrange
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.core.theme.ChargeRouteTheme
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.presentation.components.StepProgressIndicator
import com.chargeroute.app.presentation.components.TouchAccessibleCard
import com.chargeroute.app.presentation.preview.PreviewData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarSetupScreen(
    state: CarSetupUiState,
    onVehicleSelected: (com.chargeroute.app.domain.model.Vehicle) -> Unit,
    onBatteryChanged: (Int) -> Unit,
    onAcToggled: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val selected = state.selectedVehicle ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.carsetup_open_menu),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    StepProgressIndicator(
                        currentStep = 1,
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
            CarSetupBottomBar(onNextClick = onNextClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.carsetup_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.carsetup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Selector de vehículo disponible en Argentina
            VehicleSelectorSection(
                availableVehicles = state.availableVehicles,
                selected = selected,
                onVehicleSelected = onVehicleSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Ficha técnica del auto
            VehicleSpecsCard(selected = selected)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Climatización / Aire Acondicionado (Impacto Dinámico en Pantalla)
            AirConditioningCard(state = state, onAcToggled = onAcToggled)

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Batería actual (Ingreso manual directo de % + Botones +/- + Slider sincronizado)
            BatteryCard(state = state, onBatteryChanged = onBatteryChanged)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Barra de abajo con el boton Siguiente
@Composable
private fun CarSetupBottomBar(onNextClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onNextClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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
                    Text(
                        text = stringResource(R.string.carsetup_next),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Titulo + cuadro del vehiculo + menu desplegable de marcas y modelos
@Composable
private fun VehicleSelectorSection(
    availableVehicles: List<Vehicle>,
    selected: Vehicle,
    onVehicleSelected: (Vehicle) -> Unit
) {
    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    var menuBrand by remember { mutableStateOf<String?>(null) }
    val brands = availableVehicles.map { it.brand }.distinct().sortedBy { it.lowercase() }

    Text(
        text = stringResource(R.string.carsetup_vehicle_section),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    // Un solo cuadro: primero se elige la marca y despues el modelo en la misma lista
    Box {
        VehicleSelectorBox(
            label = stringResource(R.string.carsetup_vehicle_label),
            value = "${selected.brand} ${selected.model}",
            subtitle = stringResource(R.string.carsetup_vehicle_subtitle, selected.year, selected.version),
            onClick = {
                menuBrand = null
                vehicleMenuExpanded = true
            }
        )

        DropdownMenu(
            expanded = vehicleMenuExpanded,
            onDismissRequest = { vehicleMenuExpanded = false }
        ) {
            val brandInMenu = menuBrand
            if (brandInMenu == null) {
                // Paso 1: marcas
                brands.forEach { brand ->
                    DropdownMenuItem(
                        text = { Text(text = brand, fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { menuBrand = brand }
                    )
                }
            } else {
                // Paso 2: modelos de esa marca
                DropdownMenuItem(
                    text = { Text(text = brandInMenu, fontWeight = FontWeight.Bold, color = BrandPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.carsetup_back_to_brands),
                            tint = BrandPrimary
                        )
                    },
                    onClick = { menuBrand = null }
                )
                availableVehicles.filter { it.brand == brandInMenu }.forEach { vehicle ->
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = vehicle.model,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.carsetup_vehicle_option_detail,
                                        vehicle.version,
                                        vehicle.wltpRangeKm.toInt(),
                                        vehicle.baseConsumptionKwhPer100Km
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onVehicleSelected(vehicle)
                            vehicleMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

// Ficha tecnica del auto elegido
@Composable
private fun VehicleSpecsCard(selected: Vehicle) {
    TouchAccessibleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.carsetup_spec_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BatteryGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BatteryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.carsetup_autocompleted),
                        color = BatteryGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                R.string.carsetup_kwh_value,
                                String.format(Locale.US, "%.1f", selected.usableBatteryKwh)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.carsetup_usable_battery_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.carsetup_km_value, selected.wltpRangeKm.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.carsetup_factory_range_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.supportedConnectors.joinToString(" / ") { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.carsetup_max_dc, selected.maxDcKw.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Aire acondicionado + consumo y autonomia que se recalculan
@Composable
private fun AirConditioningCard(
    state: CarSetupUiState,
    onAcToggled: (Boolean) -> Unit
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.acActive) BrandPrimary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = if (state.acActive) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.carsetup_ac_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.acActive) stringResource(R.string.carsetup_ac_on)
                            else stringResource(R.string.carsetup_ac_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = state.acActive,
                    onCheckedChange = onAcToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Indicador de consumo dinámico en tiempo real
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.carsetup_consumption_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.carsetup_consumption_value,
                                String.format(Locale.US, "%.1f", state.effectiveConsumptionKwhPer100Km)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.acActive) BatteryOrange else BrandPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.carsetup_estimated_range_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.carsetup_km_value, state.estimatedRangeKm.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }
    }
}

// Bateria actual: campo de % + botones +/- + slider sincronizado
@Composable
private fun BatteryCard(
    state: CarSetupUiState,
    onBatteryChanged: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current

    TouchAccessibleCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.carsetup_battery_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.carsetup_battery_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Caja de ingreso directo editable de %
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledIconButton(
                        onClick = { onBatteryChanged(state.initialBatteryPercent - 5) },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.carsetup_minus_5)
                        )
                    }

                    var textInput by remember(state.initialBatteryPercent) {
                        mutableStateOf(state.initialBatteryPercent.toString())
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { newVal ->
                            val digitsOnly = newVal.filter { it.isDigit() }.take(3)
                            val parsed = digitsOnly.toIntOrNull()
                            // Nunca se puede escribir mas de 100%
                            textInput = if (parsed != null && parsed > 100) "100" else digitsOnly
                            if (parsed != null) {
                                onBatteryChanged(parsed.coerceIn(5, 100))
                            }
                        },
                        modifier = Modifier.width(74.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        ),
                        suffix = {
                            Text(
                                text = stringResource(R.string.carsetup_percent_suffix),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    FilledIconButton(
                        onClick = { onBatteryChanged(state.initialBatteryPercent + 5) },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.carsetup_plus_5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = state.initialBatteryPercent.toFloat(),
                onValueChange = { onBatteryChanged(it.toInt()) },
                valueRange = 5f..100f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = BrandPrimary,
                    activeTrackColor = BrandPrimary
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.carsetup_percent_min),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.carsetup_kwh_available,
                        String.format(Locale.US, "%.1f", state.availableKwh),
                        String.format(Locale.US, "%.1f", state.usableBatteryKwh)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary
                )
                Text(
                    text = stringResource(R.string.carsetup_percent_max),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VehicleSelectorBox(
    label: String,
    value: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    TouchAccessibleCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.carsetup_change),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CarSetupScreenPreview() {
    ChargeRouteTheme(darkTheme = false) {
        CarSetupScreen(
            state = CarSetupUiState(
                availableVehicles = listOf(PreviewData.vehicle),
                selectedVehicle = PreviewData.vehicle
            ),
            onVehicleSelected = {},
            onBatteryChanged = {},
            onAcToggled = {},
            onMenuClick = {},
            onNextClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CarSetupScreenDarkPreview() {
    ChargeRouteTheme(darkTheme = true) {
        CarSetupScreen(
            state = CarSetupUiState(
                availableVehicles = listOf(PreviewData.vehicle),
                selectedVehicle = PreviewData.vehicle
            ),
            onVehicleSelected = {},
            onBatteryChanged = {},
            onAcToggled = {},
            onMenuClick = {},
            onNextClick = {}
        )
    }
}
