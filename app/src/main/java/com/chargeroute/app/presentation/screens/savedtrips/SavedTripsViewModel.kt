package com.chargeroute.app.presentation.screens.savedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SavedTripsViewModel(
    tripRepository: TripRepository
) : ViewModel() {

    // Observa Room: cuando se guarda o termina un viaje, la lista se actualiza sola
    val savedTrips: StateFlow<List<Trip>> = tripRepository.getSavedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
