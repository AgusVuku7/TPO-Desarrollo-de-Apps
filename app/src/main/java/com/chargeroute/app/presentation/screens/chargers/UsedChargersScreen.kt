package com.chargeroute.app.presentation.screens.chargers

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.domain.model.UsedCharger
import com.chargeroute.app.domain.model.UsedChargers
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.presentation.components.TouchAccessibleCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsedChargersViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {
    private val _chargers = MutableStateFlow<List<UsedCharger>>(emptyList())
    val chargers: StateFlow<List<UsedCharger>> = _chargers.asStateFlow()

    // Sale de los viajes guardados en Room, asi que funciona sin conexion
    fun refresh() {
        viewModelScope.launch {
            val trips = tripRepository.getSavedTrips().firstOrNull() ?: emptyList()
            _chargers.value = UsedChargers.fromTrips(trips)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsedChargersScreen(
    viewModel: UsedChargersViewModel,
    onChargerClick: (UsedCharger) -> Unit,
    onBackClick: () -> Unit
) {
    val chargers by viewModel.chargers.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis cargadores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp) // RNF02
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (chargers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EvStation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Todavía no paraste en ningún cargador",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Acá aparecen los cargadores de las paradas de tus viajes guardados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chargers, key = { it.station.id }) { charger ->
                    UsedChargerCard(charger = charger, onClick = { onChargerClick(charger) })
                }
            }
        }
    }
}

@Composable
private fun UsedChargerCard(charger: UsedCharger, onClick: () -> Unit) {
    val station = charger.station
    val connectors = station.connectors
        .distinctBy { it.type }
        .joinToString(" • ") { "${it.type.displayName} ${it.powerKw.toInt()} kW" }
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(charger.lastUsedMillis))

    TouchAccessibleCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EvStation,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (station.address.isNotBlank()) {
                    Text(
                        text = station.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (connectors.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = connectors,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val times = if (charger.tripsCount > 1) " • en ${charger.tripsCount} viajes" else ""
                Text(
                    text = "${charger.lastTripLabel} ($date)$times",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandPrimary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
