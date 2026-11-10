package com.chargeroute.app.presentation.screens.chargers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.UsedCharger
import com.chargeroute.app.domain.model.UsedChargers
import com.chargeroute.app.domain.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UsedChargersViewModel(
    tripRepository: TripRepository
) : ViewModel() {

    // Sale de los viajes guardados en Room, asi que funciona sin conexion y se actualiza sola
    val chargers: StateFlow<List<UsedCharger>> = tripRepository.getSavedTrips()
        .map { trips -> UsedChargers.fromTrips(trips) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
