package com.chargeroute.app.presentation.screens.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargeroute.app.core.location.LocationClient
import com.chargeroute.app.core.network.NetworkConnectivityObserver
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.usecase.CalculateRouteUseCase
import com.chargeroute.app.domain.usecase.GetOfflineRouteUseCase
import com.chargeroute.app.domain.usecase.SaveTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArgentineCity(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

data class RoutePlanningUiState(
    val origin: LocationPoint = LocationPoint(
        name = "Buenos Aires (Obelisco)",
        address = "Av. 9 de Julio y Corrientes, CABA",
        latitude = -34.6037,
        longitude = -58.3816
    ),
    val destination: LocationPoint = LocationPoint(
        name = "Mar del Plata (Plaza Colón)",
        address = "Av. Patricio Peralta Ramos 2200, Mar del Plata",
        latitude = -38.0055,
        longitude = -57.5432
    ),
    val originInputText: String = "Buenos Aires (Obelisco)",
    val destinationInputText: String = "Mar del Plata (Plaza Colón)",
    val vehicle: Vehicle? = null,
    val initialBatteryPercent: Int = 70,
    val acActive: Boolean = true,
    val isLoading: Boolean = false,
    val calculatedPlan: RoutePlan? = null,
    val errorMessage: String? = null,
    val infoNotice: String? = null,
    val isOffline: Boolean = false,
    val createdTripId: String? = null
)

class RoutePlanningViewModel(
    private val calculateRouteUseCase: CalculateRouteUseCase,
    private val saveTripUseCase: SaveTripUseCase,
    private val getOfflineRouteUseCase: GetOfflineRouteUseCase,
    private val locationClient: LocationClient,
    private val networkObserver: NetworkConnectivityObserver
) : ViewModel() {

    // Diccionario de ciudades argentinas para autocompletado y geocodificación precisa
    val argentineCities = listOf(
        ArgentineCity("Buenos Aires (Obelisco)", "Av. 9 de Julio y Corrientes, CABA", -34.6037, -58.3816),
        ArgentineCity("Mar del Plata", "Av. Patricio Peralta Ramos 2200, Mar del Plata", -38.0055, -57.5432),
        ArgentineCity("Dolores", "Autovía 2 km 210, Dolores, Buenos Aires", -36.3132, -57.6792),
        ArgentineCity("Chascomús", "Costanera España y Av. Lastra, Chascomús", -35.5786, -58.0125),
        ArgentineCity("Pinamar", "Av. Bunge y Av. Shaw, Pinamar", -37.1084, -56.8587),
        ArgentineCity("Villa Gesell", "Av. Buenos Aires y Av. 3, Villa Gesell", -37.2642, -56.9731),
        ArgentineCity("Tandil", "Plaza Independencia, Tandil", -37.3217, -59.1332),
        ArgentineCity("La Plata", "Plaza Moreno, Calle 12 y 51, La Plata", -34.9214, -57.9545),
        ArgentineCity("Rosario", "Monumento a la Bandera, Rosario, Santa Fe", -32.9468, -60.6393),
        ArgentineCity("Córdoba Capital", "Plaza San Martín, Córdoba", -31.4201, -64.1888)
    )

    private val _uiState = MutableStateFlow(RoutePlanningUiState())
    val uiState: StateFlow<RoutePlanningUiState> = _uiState.asStateFlow()

    init {
        checkNetwork()
    }

    private fun checkNetwork() {
        val connected = networkObserver.isCurrentlyConnected
        _uiState.update { it.copy(isOffline = !connected) }
    }

    fun initParams(vehicle: Vehicle, batteryPercent: Int, acActive: Boolean) {
        _uiState.update {
            it.copy(
                vehicle = vehicle,
                initialBatteryPercent = batteryPercent,
                acActive = acActive
            )
        }
    }

    fun setOriginCity(city: ArgentineCity) {
        _uiState.update {
            it.copy(
                origin = LocationPoint(city.name, city.address, city.latitude, city.longitude),
                originInputText = city.name,
                infoNotice = null
            )
        }
    }

    fun setDestinationCity(city: ArgentineCity) {
        _uiState.update {
            it.copy(
                destination = LocationPoint(city.name, city.address, city.latitude, city.longitude),
                destinationInputText = city.name,
                infoNotice = null
            )
        }
    }

    fun updateOriginInput(text: String) {
        _uiState.update { it.copy(originInputText = text) }
        // Match against known cities or custom geocode
        val matched = argentineCities.firstOrNull { it.name.contains(text, ignoreCase = true) }
        if (matched != null) {
            _uiState.update {
                it.copy(origin = LocationPoint(matched.name, matched.address, matched.latitude, matched.longitude))
            }
        } else {
            _uiState.update {
                it.copy(origin = it.origin.copy(name = text, address = "Ubicación personalizada"))
            }
        }
    }

    fun updateDestinationInput(text: String) {
        _uiState.update { it.copy(destinationInputText = text) }
        val matched = argentineCities.firstOrNull { it.name.contains(text, ignoreCase = true) }
        if (matched != null) {
            _uiState.update {
                it.copy(destination = LocationPoint(matched.name, matched.address, matched.latitude, matched.longitude))
            }
        } else {
            _uiState.update {
                it.copy(destination = it.destination.copy(name = text, address = "Ubicación personalizada"))
            }
        }
    }

    fun swapOriginDestination() {
        _uiState.update {
            it.copy(
                origin = it.destination,
                destination = it.origin,
                originInputText = it.destinationInputText,
                destinationInputText = it.originInputText
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
                val lat = loc.latitude
                val lon = loc.longitude

                // Verificamos si las coordenadas están en territorio argentino
                val isInsideArgentina = lat in -56.0..-21.0 && lon in -74.0..-53.0

                if (isInsideArgentina) {
                    val point = LocationPoint(
                        name = "Mi ubicación GPS",
                        address = String.format("Lat: %.4f, Lon: %.4f", lat, lon),
                        latitude = lat,
                        longitude = lon
                    )
                    _uiState.update {
                        it.copy(
                            origin = point,
                            originInputText = point.name,
                            infoNotice = "Ubicación GPS obtenida con éxito."
                        )
                    }
                } else {
                    // Detección de emulador con coordenadas default (California, EE.UU.)
                    // Para evitar el error de cálculo de 13.000 km, fijamos Buenos Aires Obelisco
                    val obelisco = argentineCities.first()
                    _uiState.update {
                        it.copy(
                            origin = LocationPoint(obelisco.name, obelisco.address, obelisco.latitude, obelisco.longitude),
                            originInputText = obelisco.name,
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

    fun calculateRoute(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        val vehicle = currentState.vehicle ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = calculateRouteUseCase(
                origin = currentState.origin,
                destination = currentState.destination,
                vehicle = vehicle,
                initialBatteryPercent = currentState.initialBatteryPercent,
                acActive = currentState.acActive
            )

            result.onSuccess { plan ->
                val tripId = "trip_${System.currentTimeMillis()}"
                val trip = Trip(
                    id = tripId,
                    origin = currentState.origin,
                    destination = currentState.destination,
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
                onSuccess(tripId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al calcular ruta: ${err.localizedMessage ?: "Verificá la conexión o los parámetros"}"
                    )
                }
            }
        }
    }
}
