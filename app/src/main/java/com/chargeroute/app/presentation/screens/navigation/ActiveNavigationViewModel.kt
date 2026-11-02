package com.chargeroute.app.presentation.screens.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.usecase.UpdateBatteryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ActiveNavigationUiState(
    val trip: Trip? = null,
    val progress: Float = 0.0f, // 0.0 a 1.0 a lo largo del viaje
    val currentSpeedKmH: Int = 92,
    val currentBatteryPercent: Int = 70,
    val distanceToNextStopKm: Int = 210,
    val minutesToNextStop: Int = 145,
    val distanceRemainingTotalKm: Int = 404,
    val currentManeuver: String = "Continuá por Autovía 2",
    val maneuverDistance: String = "2.4 km",
    val maneuverSubtext: String = "Hacia Dolores y Mar del Plata",
    val roadName: String = "Autovía 2 (Sentido Sur)",
    val isArrived: Boolean = false,
    val isSimulationPlaying: Boolean = false
) {
    val isLowBatteryWarning: Boolean
        get() = currentBatteryPercent < 20
}

class ActiveNavigationViewModel(
    private val tripRepository: TripRepository,
    private val updateBatteryUseCase: UpdateBatteryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveNavigationUiState())
    val uiState: StateFlow<ActiveNavigationUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var updateBatteryJob: Job? = null

    /**
     * Carga un viaje utilizando el currentBatteryPercent persistido en la base de datos.
     * Sincroniza con cualquier actualización de batería pendiente para evitar condiciones de carrera.
     */
    fun loadTrip(tripId: String): Job {
        stopSimulation()
        val job = viewModelScope.launch {
            // Espera a que cualquier persistencia de batería en vuelo haya finalizado
            updateBatteryJob?.join()

            val trip = tripRepository.getTripById(tripId).firstOrNull() ?: tripRepository.getLatestTrip().firstOrNull()
            if (trip != null) {
                val totalDist = trip.routePlan.distanceKm.toInt()
                val nextStop = trip.routePlan.stops.firstOrNull()
                val stopDist = if (nextStop != null) (totalDist * 0.52).toInt() else totalDist
                val stopMin = if (nextStop != null) (trip.routePlan.driveDurationMinutes * 0.52).toInt() else trip.routePlan.driveDurationMinutes

                // Conserva el progreso si ya estamos en curso de este mismo viaje
                val isSameTrip = _uiState.value.trip?.id == trip.id
                val currentProg = if (isSameTrip && !_uiState.value.isArrived) _uiState.value.progress else 0.0f
                val currentSpeed = if (isSameTrip) _uiState.value.currentSpeedKmH else 95

                _uiState.value = ActiveNavigationUiState(
                    trip = trip,
                    progress = currentProg,
                    isArrived = false,
                    isSimulationPlaying = false,
                    currentSpeedKmH = currentSpeed,
                    currentBatteryPercent = trip.currentBatteryPercent,
                    distanceRemainingTotalKm = if (isSameTrip) _uiState.value.distanceRemainingTotalKm else totalDist,
                    distanceToNextStopKm = if (isSameTrip) _uiState.value.distanceToNextStopKm else stopDist,
                    minutesToNextStop = if (isSameTrip) _uiState.value.minutesToNextStop else stopMin,
                    currentManeuver = if (isSameTrip) _uiState.value.currentManeuver else "Tomá la salida hacia Autovía 2",
                    maneuverDistance = if (isSameTrip) _uiState.value.maneuverDistance else "1.2 km",
                    maneuverSubtext = if (nextStop != null) "Próxima parada: ${nextStop.station.name}" else "Hacia ${trip.destination.name}",
                    roadName = "Autovía 2 (Sentido Sur)"
                )
            }
        }
        return job
    }

    /**
     * Actualiza el nivel de batería en ruta y recalcula alertas.
     * Permite persistir tanto si el ViewModel ya tiene cargado el trip como si se le pasa el tripId explícito.
     */
    fun updateBattery(newPercent: Int, tripId: String? = null): Job {
        val clamped = newPercent.coerceIn(5, 100)
        _uiState.update { it.copy(currentBatteryPercent = clamped) }
        val job = viewModelScope.launch {
            val targetTripId = tripId
                ?: _uiState.value.trip?.id
                ?: tripRepository.getLatestTrip().firstOrNull()?.id
            if (targetTripId != null) {
                updateBatteryUseCase(targetTripId, clamped)
                _uiState.update { state ->
                    if (state.trip?.id == targetTripId) {
                        state.copy(
                            currentBatteryPercent = clamped,
                            trip = state.trip.copy(currentBatteryPercent = clamped)
                        )
                    } else {
                        state.copy(currentBatteryPercent = clamped)
                    }
                }
            }
        }
        updateBatteryJob = job
        return job
    }

    /**
     * Sobrecarga conveniente para invocar con tripId como primer argumento
     */
    fun updateBattery(tripId: String, newPercent: Int): Job = updateBattery(newPercent, tripId)

    /**
     * Inicia o pausa la simulación en tiempo real (copiloto activo)
     */
    fun toggleSimulation() {
        if (_uiState.value.isSimulationPlaying) {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        _uiState.update { it.copy(isSimulationPlaying = true) }

        simulationJob = viewModelScope.launch {
            while (isActive && _uiState.value.progress < 0.98f) {
                delay(1200) // cada 1.2 segundos avanza un paso realista
                advanceProgress(0.035f) // avanza ~3.5%
            }
            if (_uiState.value.progress >= 0.98f) {
                stopSimulation()
                markArrived()
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _uiState.update { it.copy(isSimulationPlaying = false) }
    }

    /**
     * Simula el avance del vehículo a lo largo de la ruta hacia el destino.
     * Descuenta batería de forma gradual a partir del porcentaje actual (incluso tras recargas intermedias).
     */
    fun advanceProgress(delta: Float) {
        val current = _uiState.value
        val newProgress = (current.progress + delta).coerceIn(0f, 1f)
        val arrived = newProgress >= 0.98f

        val totalDist = current.trip?.routePlan?.distanceKm ?: 404.0
        val remainingKm = ((1.0 - newProgress) * totalDist).toInt().coerceAtLeast(0)

        val nextStop = current.trip?.routePlan?.stops?.firstOrNull()
        val stopDistanceKm = if (nextStop != null) {
            val distToStation = (totalDist * 0.52).toInt()
            (distToStation - (newProgress * totalDist).toInt()).coerceAtLeast(0)
        } else {
            remainingKm
        }

        // Batería desciende progresivamente desde el valor actual del auto
        val targetArrivalBat = current.trip?.routePlan?.arrivalBatteryPercent ?: 25
        val remainingBattery = if (arrived) {
            targetArrivalBat
        } else {
            val stepDrop = (delta * 30).toInt().coerceAtLeast(1)
            (current.currentBatteryPercent - stepDrop).coerceIn(5, 100)
        }

        // Maniobra contextual según el punto de la ruta
        val maneuver = when {
            arrived -> "Has llegado a tu destino"
            newProgress > 0.85f -> "Tomá la rotonda de ingreso a Mar del Plata"
            newProgress in 0.48f..0.55f -> "Ingresá al parador de carga Dolores"
            newProgress in 0.40f..0.47f -> "En 12 km tomá la salida a la estación de servicio"
            newProgress > 0.20f -> "Continuá por Autovía 2 hacia Dolores"
            else -> "Seguí derecho por Autovía 2"
        }

        val maneuverDist = when {
            arrived -> "0 m"
            newProgress in 0.48f..0.55f -> "300 m"
            newProgress in 0.40f..0.47f -> "12 km"
            newProgress > 0.85f -> "1.5 km"
            else -> "45 km"
        }

        val speed = if (arrived) 0 else Random.nextInt(88, 112)

        _uiState.update {
            it.copy(
                progress = newProgress,
                distanceRemainingTotalKm = remainingKm,
                distanceToNextStopKm = stopDistanceKm,
                minutesToNextStop = (stopDistanceKm / 1.4).toInt().coerceAtLeast(1),
                currentSpeedKmH = speed,
                currentBatteryPercent = remainingBattery,
                currentManeuver = maneuver,
                maneuverDistance = maneuverDist,
                isArrived = arrived
            )
        }

        if (arrived) {
            markArrived()
        }
    }

    private fun markArrived() {
        _uiState.update { it.copy(isArrived = true, isSimulationPlaying = false) }
        _uiState.value.trip?.let { trip ->
            viewModelScope.launch {
                tripRepository.markTripCompleted(trip.id)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}
