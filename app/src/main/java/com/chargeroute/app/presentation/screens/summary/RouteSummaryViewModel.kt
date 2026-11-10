package com.chargeroute.app.presentation.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.usecase.GetOfflineRouteUseCase
import com.chargeroute.app.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RouteSummaryUiState(
    val trip: Trip? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false
)

class RouteSummaryViewModel(
    private val tripRepository: TripRepository,
    private val getOfflineRouteUseCase: GetOfflineRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteSummaryUiState())
    val uiState: StateFlow<RouteSummaryUiState> = _uiState.asStateFlow()

    fun loadTrip(tripId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId).firstOrNull()
            if (trip != null) {
                _uiState.update { it.copy(trip = trip, isLoading = false) }
            } else {
                // Si no lo encuentra, usa el ultimo viaje
                val latest = tripRepository.getLatestTrip().firstOrNull()
                _uiState.update { it.copy(trip = latest, isLoading = false) }
            }
        }
    }
}
