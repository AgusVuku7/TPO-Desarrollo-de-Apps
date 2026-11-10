package com.chargeroute.app.presentation.screens.savedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SavedTripsViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {
    private val _savedTrips = MutableStateFlow<List<Trip>>(emptyList())
    val savedTrips: StateFlow<List<Trip>> = _savedTrips.asStateFlow()

    // Se vuelve a leer cada vez que se abre el historial, asi aparecen los viajes nuevos
    fun refresh() {
        viewModelScope.launch {
            _savedTrips.value = tripRepository.getSavedTrips().firstOrNull() ?: emptyList()
        }
    }
}
