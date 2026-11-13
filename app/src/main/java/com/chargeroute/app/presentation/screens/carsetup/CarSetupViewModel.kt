package com.chargeroute.app.presentation.screens.carsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.BatteryLevels
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.usecase.GetVehicleCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CarSetupUiState(
    val availableVehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val initialBatteryPercent: Int = BatteryLevels.DEFAULT_INITIAL_PERCENT,
    val acActive: Boolean = true
) {
    val usableBatteryKwh: Double
        get() = selectedVehicle?.usableBatteryKwh ?: 64.0

    val availableKwh: Double
        get() = (usableBatteryKwh * initialBatteryPercent / 100.0)

    val baseConsumptionKwhPer100Km: Double
        get() = selectedVehicle?.baseConsumptionKwhPer100Km ?: 15.5

    val acMultiplier: Double
        get() = if (acActive) 1.10 else 1.00

    val effectiveConsumptionKwhPer100Km: Double
        get() = baseConsumptionKwhPer100Km * acMultiplier

    val estimatedRangeKm: Double
        get() {
            val cons = effectiveConsumptionKwhPer100Km
            return if (cons > 0) (availableKwh / cons) * 100.0 else 0.0
        }
}

class CarSetupViewModel(
    private val getVehicleCatalogUseCase: GetVehicleCatalogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarSetupUiState())
    val uiState: StateFlow<CarSetupUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        val vehicles = getVehicleCatalogUseCase.getAvailableVehicles()
        _uiState.update { it.copy(availableVehicles = vehicles) }
        viewModelScope.launch {
            // Se lee una sola vez el auto guardado en DataStore.
            // Si todavia no se eligio ninguno, el repositorio devuelve el auto por defecto.
            val savedVehicle = getVehicleCatalogUseCase.getDefaultVehicle().first()
            _uiState.update { it.copy(selectedVehicle = savedVehicle) }
        }
    }

    fun selectVehicle(vehicle: Vehicle) {
        _uiState.update { it.copy(selectedVehicle = vehicle) }
        viewModelScope.launch {
            getVehicleCatalogUseCase.saveSelectedVehicle(vehicle)
        }
    }

    fun updateBatteryPercent(percent: Int) {
        _uiState.update { it.copy(initialBatteryPercent = percent.coerceIn(5, 100)) }
    }

    fun toggleAc(active: Boolean) {
        _uiState.update { it.copy(acActive = active) }
    }
}
