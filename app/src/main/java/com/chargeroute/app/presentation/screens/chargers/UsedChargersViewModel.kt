package com.chargeroute.app.presentation.screens.chargers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.UsedCharger
import com.chargeroute.app.domain.model.UsedChargers
import com.chargeroute.app.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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
