package com.chargeroute.app.presentation.screens.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.core.location.LocationClient
import com.chargeroute.app.domain.model.NavStep
import com.chargeroute.app.domain.model.RangeEstimator
import com.chargeroute.app.domain.model.ReachEstimate
import com.chargeroute.app.domain.model.RouteGeometry
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.usecase.GetNavigationStepsUseCase
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
import java.util.Locale
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
    val isSimulationPlaying: Boolean = false,
    val stopProgresses: List<Float> = emptyList(), // en que parte de la ruta esta cada cargador (0.0 a 1.0)
    val nextStopIndex: Int = 0, // proxima parada (o la parada en la que se esta cargando)
    val isCharging: Boolean = false,
    val isGpsTracking: Boolean = false,
    val gpsNotice: String? = null,
    val maneuverType: String = "straight"
) {
    val isLowBatteryWarning: Boolean
        get() = currentBatteryPercent < 20

    // Proxima parada de carga (null si ya se pasaron todas)
    val nextStop: TripStop?
        get() = trip?.routePlan?.stops?.getOrNull(nextStopIndex)

    // Lugar de la ruta donde esta la proxima parada
    val stopProgress: Float?
        get() = stopProgresses.getOrNull(nextStopIndex)

    /**
     * RF03: con la bateria actual, ¿se llega a la proxima parada o al destino?
     * Si se esta cargando, el objetivo es la parada siguiente (o el destino).
     */
    val reachEstimate: ReachEstimate?
        get() {
            val currentTrip = trip ?: return null
            if (isArrived) return null
            val targetIndex = if (isCharging) nextStopIndex + 1 else nextStopIndex
            val targetProgress = stopProgresses.getOrNull(targetIndex) ?: 1f
            val targetName = currentTrip.routePlan.stops.getOrNull(targetIndex)?.station?.name
                ?: currentTrip.destination.name
            val kmToTarget = ((targetProgress - progress) * currentTrip.routePlan.distanceKm).coerceAtLeast(0.0)
            return RangeEstimator.estimate(
                targetName = targetName,
                kmToTarget = kmToTarget,
                batteryPercent = currentBatteryPercent,
                usableBatteryKwh = currentTrip.vehicle.usableBatteryKwh,
                consumptionKwhPer100Km = currentTrip.routePlan.consumptionKwhPer100Km
            )
        }
}

class ActiveNavigationViewModel(
    private val tripRepository: TripRepository,
    private val updateBatteryUseCase: UpdateBatteryUseCase,
    private val locationClient: LocationClient? = null,
    private val getNavigationStepsUseCase: GetNavigationStepsUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveNavigationUiState())
    val uiState: StateFlow<ActiveNavigationUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var updateBatteryJob: Job? = null
    private var gpsJob: Job? = null
    private var batteryExact: Float? = null // con decimales, para que baje de a poco
    private var navSteps: List<NavStep> = emptyList()
    private var navStepsTripId: String? = null

    // Pide las indicaciones del viaje (una sola vez por viaje)
    private fun loadNavigationSteps(trip: Trip) {
        if (navStepsTripId == trip.id) {
            refreshInstruction()
            return
        }
        navSteps = emptyList()
        navStepsTripId = trip.id
        val useCase = getNavigationStepsUseCase ?: return
        viewModelScope.launch {
            navSteps = useCase(trip.routePlan)
            refreshInstruction()
        }
    }

    private fun refreshInstruction() {
        val state = _uiState.value
        if (state.isCharging || state.isArrived) return
        val next = nextStep(state.progress) ?: return
        val totalKm = state.trip?.routePlan?.distanceKm ?: return
        _uiState.update {
            it.copy(
                currentManeuver = next.instruction,
                maneuverDistance = distanceText((next.progress - state.progress) * totalKm),
                maneuverType = next.maneuver
            )
        }
    }

    private fun nextStep(progress: Float): NavStep? {
        return navSteps.firstOrNull { it.progress > progress + 0.00001f }
    }

    private fun distanceText(km: Double): String {
        return if (km < 1.0) {
            "${(km * 100).toInt() * 10} m"
        } else {
            String.format(Locale.US, "%.1f km", km)
        }
    }

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
                // Lugar real de cada cargador sobre la ruta
                val stopProgresses = trip.routePlan.stops.map {
                    RouteGeometry.nearestOnRoute(trip.routePlan.polylinePoints, it.station.latitude, it.station.longitude).progress
                }
                batteryExact = null

                // Conserva el progreso si ya estamos en curso de este mismo viaje
                // Si ya habia llegado, el viaje vuelve a empezar de cero
                val isSameTrip = _uiState.value.trip?.id == trip.id && !_uiState.value.isArrived
                val currentProg = if (isSameTrip) _uiState.value.progress else 0.0f
                val currentSpeed = 0 // recien arranca, el auto todavia no se mueve

                // Proxima parada: la primera que todavia no se alcanzo
                val nextStopIndex = if (isSameTrip) {
                    _uiState.value.nextStopIndex
                } else {
                    stopProgresses.indexOfFirst { it > currentProg }.let { if (it == -1) stopProgresses.size else it }
                }
                val nextStop = trip.routePlan.stops.getOrNull(nextStopIndex)
                val stopProgress = stopProgresses.getOrNull(nextStopIndex)
                val stopDist = if (stopProgress != null) (totalDist * stopProgress).toInt() else totalDist
                val stopMin = if (stopProgress != null) (trip.routePlan.driveDurationMinutes * stopProgress).toInt() else trip.routePlan.driveDurationMinutes

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
                    currentManeuver = if (isSameTrip) _uiState.value.currentManeuver else "Seguí la ruta marcada",
                    maneuverDistance = if (isSameTrip) _uiState.value.maneuverDistance else "$stopDist km",
                    maneuverSubtext = if (nextStop != null) "Próxima parada: ${nextStop.station.name}" else "Hacia ${trip.destination.name}",
                    roadName = "Hacia ${trip.destination.name}",
                    stopProgresses = stopProgresses,
                    nextStopIndex = nextStopIndex,
                    isCharging = isSameTrip && _uiState.value.isCharging,
                    maneuverType = if (isSameTrip) _uiState.value.maneuverType else "straight"
                )
                loadNavigationSteps(trip)
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
        batteryExact = null
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
            stopGpsTracking()
            startSimulation()
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        _uiState.update { it.copy(isSimulationPlaying = true) }

        simulationJob = viewModelScope.launch {
            while (isActive && _uiState.value.progress < ARRIVED_PROGRESS) {
                delay(150) // pasos chiquitos para que el auto no pegue saltos
                advanceProgress(0.004f)
                // En el cargador se frena: para seguir hay que volver a apretar "Simular viaje"
                if (_uiState.value.isCharging) {
                    stopSimulation()
                    return@launch
                }
            }
            if (_uiState.value.progress >= ARRIVED_PROGRESS) {
                stopSimulation()
                markArrived()
            }
        }
    }

    /**
     * Sigue la posicion real del celular y la pega a la ruta marcada.
     */
    fun toggleGpsTracking() {
        if (_uiState.value.isGpsTracking) {
            stopGpsTracking()
        } else {
            startGpsTracking()
        }
    }

    private fun startGpsTracking() {
        val client = locationClient ?: return
        stopSimulation()
        _uiState.update { it.copy(isGpsTracking = true, gpsNotice = "Buscando señal GPS...") }
        gpsJob = viewModelScope.launch {
            try {
                client.locationUpdates().collect { location ->
                    applyGpsPosition(location.latitude, location.longitude, location.speed)
                }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(isGpsTracking = false, gpsNotice = "Falta el permiso de ubicación.") }
            }
        }
    }

    fun stopGpsTracking() {
        gpsJob?.cancel()
        gpsJob = null
        _uiState.update { it.copy(isGpsTracking = false, gpsNotice = null) }
    }

    fun applyGpsPosition(latitude: Double, longitude: Double, speedMetersPerSecond: Float) {
        val points = _uiState.value.trip?.routePlan?.polylinePoints ?: return
        val match = RouteGeometry.nearestOnRoute(points, latitude, longitude)

        // Lejos de la ruta: el puntero se queda en el ultimo punto valido y se avisa
        if (match.distanceToRouteKm > MAX_DISTANCE_FROM_ROUTE_KM) {
            val km = match.distanceToRouteKm.toInt()
            _uiState.update { it.copy(gpsNotice = "Estás fuera de la ruta (a $km km).") }
            return
        }

        val delta = match.progress - _uiState.value.progress
        if (delta > 0f) {
            advanceProgress(delta)
        }
        _uiState.update {
            it.copy(gpsNotice = null, currentSpeedKmH = (speedMetersPerSecond * 3.6f).toInt())
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _uiState.update { it.copy(isSimulationPlaying = false, currentSpeedKmH = 0) }
    }

    /**
     * Simula el avance del vehículo a lo largo de la ruta hacia el destino.
     * Descuenta batería de forma gradual a partir del porcentaje actual (incluso tras recargas intermedias).
     */
    fun advanceProgress(delta: Float) {
        val current = _uiState.value
        var newProgress = (current.progress + delta).coerceIn(0f, 1f)

        // Si estaba cargando, al volver a avanzar esa parada queda atras y se apunta a la siguiente
        val stopIndex = if (current.isCharging) current.nextStopIndex + 1 else current.nextStopIndex
        val stops = current.trip?.routePlan?.stops.orEmpty()
        val nextStop = stops.getOrNull(stopIndex)
        val stopProgress = current.stopProgresses.getOrNull(stopIndex)

        // Si en este paso pasaria por el cargador, se frena justo ahi
        val reachedStop = nextStop != null && stopProgress != null &&
                current.progress < stopProgress && newProgress >= stopProgress
        if (reachedStop && stopProgress != null) {
            newProgress = stopProgress
        }
        val arrived = newProgress >= ARRIVED_PROGRESS

        val totalDist = current.trip?.routePlan?.distanceKm ?: 404.0
        val remainingKm = ((1.0 - newProgress) * totalDist).toInt().coerceAtLeast(0)

        val stopDistanceKm = if (nextStop != null && stopProgress != null) {
            (((stopProgress - newProgress) * totalDist).toInt()).coerceAtLeast(0)
        } else {
            remainingKm
        }

        // Batería baja según los km recorridos y el consumo del auto.
        // Al llegar al cargador sube al porcentaje planificado.
        val targetArrivalBat = current.trip?.routePlan?.arrivalBatteryPercent ?: 25
        val trip = current.trip
        val batteryNow = batteryExact ?: current.currentBatteryPercent.toFloat()
        val movedProgress = newProgress - current.progress
        val batteryDrop = if (trip != null) {
            (movedProgress * totalDist * trip.routePlan.consumptionKwhPer100Km / trip.vehicle.usableBatteryKwh).toFloat()
        } else {
            movedProgress * 30f
        }
        val newBatteryExact = (batteryNow - batteryDrop).coerceIn(5f, 100f)
        val remainingBattery = when {
            arrived -> targetArrivalBat.coerceIn(0, 100)
            reachedStop && nextStop != null -> nextStop.departureBatteryPercent.coerceIn(0, 100)
            else -> newBatteryExact.toInt()
        }
        batteryExact = if (reachedStop) null else newBatteryExact

        // Indicaciones segun lo que viene en la ruta
        val destinationName = trip?.destination?.name ?: "tu destino"
        val step = nextStep(newProgress)
        val maneuver = when {
            arrived -> "Has llegado a tu destino"
            reachedStop && nextStop != null -> "Cargando en ${nextStop.station.name}"
            step != null -> step.instruction
            // Sin indicaciones (por ejemplo sin internet) se muestra hacia donde se va
            nextStop != null -> "Seguí la ruta hacia ${nextStop.station.name}"
            else -> "Seguí la ruta hacia $destinationName"
        }

        // Debajo de la indicacion: la parada que viene despues (o el destino)
        val upcomingStop = if (reachedStop) stops.getOrNull(stopIndex + 1) else nextStop
        val subtext = upcomingStop?.let { "Próxima parada: ${it.station.name}" } ?: "Hacia $destinationName"

        val maneuverDist = when {
            arrived || reachedStop -> "0 m"
            step != null -> distanceText((step.progress - newProgress) * totalDist)
            else -> "$stopDistanceKm km"
        }

        val maneuverType = when {
            arrived -> "arrive"
            reachedStop -> "charging"
            else -> step?.maneuver ?: "straight"
        }

        val speed = when {
            arrived || reachedStop -> 0
            current.currentSpeedKmH < 80 -> Random.nextInt(88, 112)
            else -> (current.currentSpeedKmH + Random.nextInt(-2, 3)).coerceIn(85, 115)
        }

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
                isArrived = arrived,
                nextStopIndex = stopIndex,
                isCharging = reachedStop,
                maneuverType = maneuverType,
                maneuverSubtext = subtext
            )
        }

        if (reachedStop && trip != null) {
            viewModelScope.launch { updateBatteryUseCase(trip.id, remainingBattery) }
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
        stopGpsTracking()
    }

    companion object {
        private const val ARRIVED_PROGRESS = 0.999f
        private const val MAX_DISTANCE_FROM_ROUTE_KM = 1.0
    }
}
