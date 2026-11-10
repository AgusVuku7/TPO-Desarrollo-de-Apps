package com.chargeroute.app.presentation.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.core.location.LocationClient
import com.chargeroute.app.domain.model.BatteryLevels
import com.chargeroute.app.domain.model.KnownCities
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.NoConnectionException
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.RouteServiceException
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.usecase.CalculateRouteUseCase
import com.chargeroute.app.domain.usecase.GeocodeAddressUseCase
import com.chargeroute.app.domain.usecase.SaveTripUseCase
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutePlanningUiState(
    val origin: LocationPoint = KnownCities.OBELISCO,
    val destination: LocationPoint = KnownCities.DEFAULT_DESTINATION,
    val originInputText: String = KnownCities.OBELISCO.name,
    val destinationInputText: String = KnownCities.DEFAULT_DESTINATION.name,
    val vehicle: Vehicle? = null,
    val initialBatteryPercent: Int = BatteryLevels.DEFAULT_INITIAL_PERCENT,
    val acActive: Boolean = true,
    val isLoading: Boolean = false,
    val calculatedPlan: RoutePlan? = null,
    val errorMessage: String? = null,
    val infoNotice: String? = null,
    val createdTripId: String? = null,
    val recentTrips: List<Trip> = emptyList(),
    val addressOptions: List<LocationPoint> = emptyList(),
    val optionsAreForOrigin: Boolean = true
)

// Eventos de una sola vez (no son estado): la pantalla los escucha para navegar
sealed interface RoutePlanningEvent {
    data class TripCreated(val tripId: String) : RoutePlanningEvent
}

class RoutePlanningViewModel(
    private val calculateRouteUseCase: CalculateRouteUseCase,
    private val saveTripUseCase: SaveTripUseCase,
    private val locationClient: LocationClient,
    private val geocodeAddressUseCase: GeocodeAddressUseCase,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePlanningUiState())
    val uiState: StateFlow<RoutePlanningUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RoutePlanningEvent>()
    val events: SharedFlow<RoutePlanningEvent> = _events.asSharedFlow()

    init {
        loadRecentTrips()
    }

    // Ultimos destinos distintos, del mas nuevo al mas viejo
    fun loadRecentTrips() {
        viewModelScope.launch {
            val trips = tripRepository.getSavedTrips().firstOrNull() ?: emptyList()
            _uiState.update {
                it.copy(recentTrips = trips.distinctBy { trip -> trip.destination.name }.take(5))
            }
        }
    }

    fun selectRecentDestination(trip: Trip) {
        _uiState.update {
            it.copy(
                destination = trip.destination,
                destinationInputText = trip.destination.name,
                calculatedPlan = null,
                errorMessage = null
            )
        }
        previewRoute()
    }

    /**
     * Busca las coordenadas de lo que escribio el usuario.
     * Si ya estan buscadas (ciudad conocida, GPS o busqueda anterior) no vuelve a consultar.
     */
    private suspend fun resolvePoint(text: String, current: LocationPoint): List<LocationPoint> {
        val cleanText = text.trim()
        if (current.name == cleanText && current.address != CUSTOM_ADDRESS) {
            return listOf(current)
        }
        return geocodeAddressUseCase(cleanText)
    }

    // Una sola coincidencia se usa directo. Si hay varias, el usuario elige cual es.
    private fun pickResult(results: List<LocationPoint>, text: String, isOrigin: Boolean): LocationPoint? {
        if (results.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "No encontramos \"$text\". Probá agregando la ciudad.")
            }
            return null
        }
        if (results.size > 1) {
            _uiState.update {
                it.copy(isLoading = false, addressOptions = results, optionsAreForOrigin = isOrigin)
            }
            return null
        }
        return results.first()
    }

    fun selectAddressOption(point: LocationPoint) {
        _uiState.update {
            if (it.optionsAreForOrigin) {
                it.copy(origin = point, addressOptions = emptyList())
            } else {
                it.copy(destination = point, addressOptions = emptyList())
            }
        }
        previewRoute()
    }

    private fun needsSearch(text: String, current: LocationPoint): Boolean {
        return current.name != text.trim() || current.address == CUSTOM_ADDRESS
    }

    // Devuelve origen y destino con coordenadas reales, o null si alguno no se encontro
    private suspend fun resolveOriginAndDestination(): Pair<LocationPoint, LocationPoint>? {
        val state = _uiState.value
        if (state.originInputText.isBlank() || state.destinationInputText.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Ingresá origen y destino.") }
            return null
        }

        try {
            val origin = pickResult(resolvePoint(state.originInputText, state.origin), state.originInputText, true)
                ?: return null
            _uiState.update { it.copy(origin = origin) }

            // Nominatim pide no hacer mas de una consulta por segundo
            if (needsSearch(state.originInputText, state.origin) && needsSearch(state.destinationInputText, state.destination)) {
                delay(1100)
            }

            val destination = pickResult(resolvePoint(state.destinationInputText, state.destination), state.destinationInputText, false)
                ?: return null

            _uiState.update { it.copy(destination = destination) }
            return Pair(origin, destination)
        } catch (e: CancellationException) {
            // Si se cancelo la corrutina no es un error de red: se deja seguir la cancelacion
            throw e
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "No se pudo buscar la dirección. Revisá tu conexión.")
            }
            return null
        }
    }

    /**
     * Calcula la ruta para la vista previa del mapa, sin guardar el viaje.
     */
    fun previewRoute() {
        val vehicle = _uiState.value.vehicle ?: return
        val originText = _uiState.value.originInputText
        val destinationText = _uiState.value.destinationInputText
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val points = resolveOriginAndDestination() ?: return@launch
            val state = _uiState.value
            val result = calculateRouteUseCase(
                origin = points.first,
                destination = points.second,
                vehicle = vehicle,
                initialBatteryPercent = state.initialBatteryPercent,
                acActive = state.acActive
            )
            // Si mientras tanto cambio lo escrito, este resultado ya no sirve
            val stillValid = _uiState.value.originInputText == originText &&
                    _uiState.value.destinationInputText == destinationText
            _uiState.update {
                it.copy(
                    isLoading = false,
                    calculatedPlan = if (stillValid) result.getOrNull() else null,
                    errorMessage = result.exceptionOrNull()?.let { err -> routeErrorMessage(err) }
                )
            }
        }
    }

    private var defaultOriginTried = false

    // Al abrir la pantalla (con permiso) intenta poner la ubicacion actual como origen.
    // Si no hay señal o no esta en Argentina, queda el origen de siempre.
    fun loadDefaultOrigin() {
        if (!locationClient.hasPermission() || defaultOriginTried) return
        defaultOriginTried = true
        val untouchedText = _uiState.value.originInputText
        viewModelScope.launch {
            val loc = locationClient.getCurrentLocation() ?: return@launch
            if (!isInArgentina(loc.latitude, loc.longitude)) return@launch
            // Si mientras tanto el usuario escribio otro origen, no se lo pisa
            if (_uiState.value.originInputText != untouchedText) return@launch
            val point = gpsPoint(loc.latitude, loc.longitude)
            _uiState.update {
                it.copy(origin = point, originInputText = point.name, calculatedPlan = null)
            }
            previewRoute()
        }
    }

    fun initParams(vehicle: Vehicle, batteryPercent: Int, acActive: Boolean) {
        _uiState.update {
            // Si cambio el auto o la bateria, las paradas de la vista previa ya no sirven
            val changed = it.vehicle != vehicle || it.initialBatteryPercent != batteryPercent || it.acActive != acActive
            it.copy(
                vehicle = vehicle,
                initialBatteryPercent = batteryPercent,
                acActive = acActive,
                calculatedPlan = if (changed) null else it.calculatedPlan
            )
        }
    }

    fun updateOriginInput(text: String) {
        _uiState.update {
            it.copy(
                originInputText = text,
                origin = pointForText(text, it.origin),
                calculatedPlan = null,
                addressOptions = emptyList()
            )
        }
    }

    fun updateDestinationInput(text: String) {
        _uiState.update {
            it.copy(
                destinationInputText = text,
                destination = pointForText(text, it.destination),
                calculatedPlan = null,
                addressOptions = emptyList()
            )
        }
    }

    // Busca entre las ciudades conocidas; si no esta, queda como direccion a buscar
    private fun pointForText(text: String, current: LocationPoint): LocationPoint =
        KnownCities.findByName(text) ?: current.copy(name = text, address = CUSTOM_ADDRESS)

    private fun isInArgentina(latitude: Double, longitude: Double): Boolean =
        latitude in ARGENTINA_LATITUDES && longitude in ARGENTINA_LONGITUDES

    private fun gpsPoint(latitude: Double, longitude: Double) = LocationPoint(
        name = GPS_POINT_NAME,
        address = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", latitude, longitude),
        latitude = latitude,
        longitude = longitude
    )

    fun swapOriginDestination() {
        _uiState.update {
            it.copy(
                origin = it.destination,
                destination = it.origin,
                originInputText = it.destinationInputText,
                destinationInputText = it.originInputText,
                calculatedPlan = null
            )
        }
    }

    // Sin permiso de ubicacion se avisa y el origen se puede escribir a mano
    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                infoNotice = "No diste permiso de ubicación. Escribí el origen a mano o activalo desde Ajustes."
            )
        }
    }

    /**
     * Obtiene la ubicación por GPS con protección ante emuladores en EE.UU. (Mountain View)
     */
    fun useCurrentLocationAsOrigin() {
        viewModelScope.launch {
            val loc = locationClient.getCurrentLocation()
            if (loc != null) {
                if (isInArgentina(loc.latitude, loc.longitude)) {
                    val point = gpsPoint(loc.latitude, loc.longitude)
                    _uiState.update {
                        it.copy(
                            origin = point,
                            originInputText = point.name,
                            calculatedPlan = null,
                            infoNotice = "Ubicación GPS obtenida con éxito."
                        )
                    }
                } else {
                    // Detección de emulador con coordenadas default (California, EE.UU.)
                    // Para evitar el error de cálculo de 13.000 km, fijamos Buenos Aires Obelisco
                    _uiState.update {
                        it.copy(
                            origin = KnownCities.OBELISCO,
                            originInputText = KnownCities.OBELISCO.name,
                            calculatedPlan = null,
                            infoNotice = "Emulador detectado con GPS de EE.UU. Se fijó el origen en Buenos Aires (Obelisco) para calcular distancias viales reales en Argentina."
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(infoNotice = "No se pudo obtener señal GPS. Usando ubicación predeterminada en Buenos Aires.")
                }
            }
        }
    }

    fun calculateRoute() {
        val currentState = _uiState.value
        val vehicle = currentState.vehicle ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            // Primero se buscan las direcciones que escribio el usuario
            val points = resolveOriginAndDestination() ?: return@launch
            val origin = points.first
            val destination = points.second

            val result = calculateRouteUseCase(
                origin = origin,
                destination = destination,
                vehicle = vehicle,
                initialBatteryPercent = currentState.initialBatteryPercent,
                acActive = currentState.acActive
            )

            result.onSuccess { plan ->
                val tripId = "trip_${System.currentTimeMillis()}"
                val trip = Trip(
                    id = tripId,
                    origin = origin,
                    destination = destination,
                    vehicle = vehicle,
                    initialBatteryPercent = currentState.initialBatteryPercent,
                    currentBatteryPercent = currentState.initialBatteryPercent,
                    acActive = currentState.acActive,
                    routePlan = plan
                )
                // Persistencia inmediata en Room (Offline-First)
                saveTripUseCase(trip)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        calculatedPlan = plan,
                        createdTripId = tripId,
                        errorMessage = null
                    )
                }
                loadRecentTrips()
                // El viaje ya quedo guardado en Room y en createdTripId: el evento solo avisa para navegar
                _events.emit(RoutePlanningEvent.TripCreated(tripId))
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = routeErrorMessage(err)
                    )
                }
            }
        }
    }

    // RNF06: mensaje claro de que paso y que puede hacer el usuario
    private fun routeErrorMessage(error: Throwable): String {
        val reason = when (error) {
            is NoConnectionException, is RouteServiceException -> error.message
            else -> "No se pudo calcular la ruta."
        }
        return "$reason Podés reintentar o abrir una ruta guardada."
    }

    companion object {
        private const val CUSTOM_ADDRESS = "Ubicación personalizada"
        private const val GPS_POINT_NAME = "Mi ubicación GPS"

        // Rectangulo aproximado que contiene a la Argentina
        private val ARGENTINA_LATITUDES = -56.0..-21.0
        private val ARGENTINA_LONGITUDES = -74.0..-53.0
    }
}
