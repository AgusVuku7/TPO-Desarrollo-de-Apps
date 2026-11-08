package com.chargeroute.app

import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.Trip
import com.chargeroute.app.domain.model.TripStop
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.usecase.UpdateBatteryUseCase
import com.chargeroute.app.presentation.screens.navigation.ActiveNavigationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake implementation of [TripRepository] in-memory for testing.
 */
class FakeTripRepository(
    initialTrips: List<Trip> = emptyList()
) : TripRepository {
    private val tripsFlow = MutableStateFlow<Map<String, Trip>>(initialTrips.associateBy { it.id })

    override suspend fun saveTrip(trip: Trip) {
        tripsFlow.update { it + (trip.id to trip) }
    }

    override suspend fun updateTripBattery(tripId: String, newBatteryPercent: Int) {
        tripsFlow.update { map ->
            val trip = map[tripId] ?: return@update map
            map + (tripId to trip.copy(currentBatteryPercent = newBatteryPercent))
        }
    }

    override suspend fun markTripCompleted(tripId: String) {
        tripsFlow.update { map ->
            val trip = map[tripId] ?: return@update map
            map + (tripId to trip.copy(isCompleted = true))
        }
    }

    override fun getTripById(tripId: String): Flow<Trip?> {
        return tripsFlow.map { it[tripId] }
    }

    override fun getSavedTrips(): Flow<List<Trip>> {
        return tripsFlow.map { it.values.toList() }
    }

    override fun getLatestTrip(): Flow<Trip?> {
        return tripsFlow.map { it.values.lastOrNull() }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveNavigationBatteryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTripRepository
    private lateinit var updateBatteryUseCase: UpdateBatteryUseCase
    private lateinit var viewModel: ActiveNavigationViewModel

    private val sampleVehicle = Vehicle(
        id = "hyundai_kona_2023",
        brand = "Hyundai",
        model = "Kona Electric",
        year = 2023,
        version = "Long Range - 64 kWh",
        usableBatteryKwh = 64.0,
        wltpRangeKm = 484.0,
        supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
        maxDcKw = 77.0,
        baseConsumptionKwhPer100Km = 15.5
    )

    private val sampleOrigin = LocationPoint(
        name = "CABA, Buenos Aires",
        address = "Av. 9 de Julio, CABA",
        latitude = -34.6037,
        longitude = -58.3816
    )
    private val sampleDestination = LocationPoint(
        name = "Mar del Plata",
        address = "Plaza Colón, Mar del Plata",
        latitude = -38.0055,
        longitude = -57.5562
    )

    private val sampleStation = Station(
        id = "station_ypf_chascomus",
        remoteId = "ypf_chascomus",
        name = "YPF Chascomús - Punto Eléctrico",
        address = "Ruta 2 Km 120",
        latitude = -35.5786,
        longitude = -58.0125,
        connectors = listOf(
            com.chargeroute.app.domain.model.Connector(
                type = ConnectorType.CCS2,
                powerKw = 50.0,
                isCompatible = true
            )
        ),
        availableCount = 2,
        totalCount = 4,
        amenities = listOf("24 h", "Café", "Baños", "Wi-Fi"),
        operator = "YPF",
        notes = "Sin reserva previa"
    )

    private val sampleRoutePlan = RoutePlan(
        origin = sampleOrigin,
        destination = sampleDestination,
        distanceKm = 404.0,
        totalDurationMinutes = 310,
        driveDurationMinutes = 275,
        chargeDurationMinutes = 35,
        stops = listOf(
            TripStop(
                id = "stop_1",
                station = sampleStation,
                orderIndex = 0,
                stopDurationMinutes = 35,
                arrivalBatteryPercent = 28,
                departureBatteryPercent = 80,
                chargedEnergyKwh = 33.2
            )
        ),
        polylinePoints = listOf(
            Pair(-34.6037, -58.3816),
            Pair(-35.5786, -58.0125),
            Pair(-38.0055, -57.5562)
        ),
        encodedPolyline = "",
        arrivalBatteryPercent = 35,
        consumptionKwhPer100Km = 17.0
    )

    private fun createSampleTrip(
        id: String = "trip_test_1",
        initialBatteryPercent: Int = 70,
        currentBatteryPercent: Int = 70
    ): Trip {
        return Trip(
            id = id,
            origin = sampleOrigin,
            destination = sampleDestination,
            vehicle = sampleVehicle,
            initialBatteryPercent = initialBatteryPercent,
            currentBatteryPercent = currentBatteryPercent,
            acActive = false,
            routePlan = sampleRoutePlan
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTripRepository()
        updateBatteryUseCase = UpdateBatteryUseCase(fakeRepository)
        viewModel = ActiveNavigationViewModel(fakeRepository, updateBatteryUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTrip_usesPersistedCurrentBatteryPercent_insteadOfInitialBattery() = runTest(testDispatcher) {
        // Arrange: Viaje iniciado con 70%, pero recargado / actualizado a 90% en base de datos
        val trip = createSampleTrip(
            id = "trip_rf03",
            initialBatteryPercent = 70,
            currentBatteryPercent = 90
        )
        fakeRepository.saveTrip(trip)

        // Act: Carga el viaje en navegación activa
        viewModel.loadTrip("trip_rf03")
        advanceUntilIdle()

        // Assert: loadTrip DEBE usar currentBatteryPercent (90) y no el initialBatteryPercent (70)
        val state = viewModel.uiState.value
        assertNotNull(state.trip)
        assertEquals("Debe coincidir con currentBatteryPercent persistido", 90, state.currentBatteryPercent)
        assertEquals("El objeto Trip debe reflejar la batería actual persistida", 90, state.trip?.currentBatteryPercent)
        assertEquals("La batería inicial original no debe pisarse", 70, state.trip?.initialBatteryPercent)
        assertFalse("Con 90% no debe disparar alerta de batería baja", state.isLowBatteryWarning)
    }

    @Test
    fun updateBattery_persistsWithoutTripLoadedInUiState() = runTest(testDispatcher) {
        // Arrange: Viaje en repositorio, pero el ViewModel de navegación todavía NO lo tiene cargado
        val trip = createSampleTrip(
            id = "trip_external_charge",
            initialBatteryPercent = 70,
            currentBatteryPercent = 70
        )
        fakeRepository.saveTrip(trip)

        assertEquals("El ViewModel no debe tener trip cargado todavía", null, viewModel.uiState.value.trip)

        // Act: El usuario ajusta batería desde StationDetail (donde se provee tripId explícito)
        viewModel.updateBattery(newPercent = 85, tripId = "trip_external_charge")
        advanceUntilIdle()

        // Assert: Se debió persistir exitosamente en el repositorio a pesar de que _uiState.trip era null
        val persistedTrip = fakeRepository.getTripById("trip_external_charge").firstOrNull()
        assertNotNull(persistedTrip)
        assertEquals("La batería debió persistir en Room a 85%", 85, persistedTrip?.currentBatteryPercent)
    }

    @Test
    fun updateBattery_thenLoadTrip_noRaceCondition_updatesUiStateToNewBattery() = runTest(testDispatcher) {
        // Arrange
        val trip = createSampleTrip(
            id = "trip_station_flow",
            initialBatteryPercent = 70,
            currentBatteryPercent = 70
        )
        fakeRepository.saveTrip(trip)

        // Act: Flujo real de StationDetailScreen -> Ir al cargador:
        // 1. updateBattery se despacha
        // 2. loadTrip se invoca inmediatamente después
        viewModel.updateBattery(newPercent = 92, tripId = "trip_station_flow")
        viewModel.loadTrip("trip_station_flow")
        advanceUntilIdle()

        // Assert: No hay race condition, loadTrip espera la persistencia y refleja el valor ajustado
        val state = viewModel.uiState.value
        assertEquals(92, state.currentBatteryPercent)
        assertEquals(92, state.trip?.currentBatteryPercent)

        val persisted = fakeRepository.getTripById("trip_station_flow").firstOrNull()
        assertEquals(92, persisted?.currentBatteryPercent)
    }

    @Test
    fun lowBatteryWarning_updatesDynamically_onBatteryChange() = runTest(testDispatcher) {
        // Arrange
        val trip = createSampleTrip(
            id = "trip_battery_warning",
            initialBatteryPercent = 80,
            currentBatteryPercent = 80
        )
        fakeRepository.saveTrip(trip)
        viewModel.loadTrip("trip_battery_warning")
        advanceUntilIdle()

        assertFalse("Con 80% no debe haber alerta de batería baja", viewModel.uiState.value.isLowBatteryWarning)

        // Act 1: Batería baja a 18% (menor a 20%)
        viewModel.updateBattery(18)
        advanceUntilIdle()

        // Assert 1: Alerta activa
        assertEquals(18, viewModel.uiState.value.currentBatteryPercent)
        assertTrue("Con 18% debe activarse la alerta de batería baja (< 20%)", viewModel.uiState.value.isLowBatteryWarning)

        // Act 2: Carga en estación a 80%
        viewModel.updateBattery(80)
        advanceUntilIdle()

        // Assert 2: Alerta desactivada
        assertEquals(80, viewModel.uiState.value.currentBatteryPercent)
        assertFalse("Al subir a 80% debe desactivarse la alerta de batería baja", viewModel.uiState.value.isLowBatteryWarning)
    }

    @Test
    fun newTrip_startsWithInitialBattery() = runTest(testDispatcher) {
        // Arrange: Un viaje nuevo configurado con 100% de batería inicial
        val freshTrip = createSampleTrip(
            id = "trip_fresh_100",
            initialBatteryPercent = 100,
            currentBatteryPercent = 100
        )
        fakeRepository.saveTrip(freshTrip)

        // Act
        viewModel.loadTrip("trip_fresh_100")
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(100, state.currentBatteryPercent)
        assertEquals(100, state.trip?.initialBatteryPercent)
        assertEquals(100, state.trip?.currentBatteryPercent)
    }

    @Test
    fun simulation_stopsAtCharger_andContinuesOnlyWhenPressedAgain() = runTest(testDispatcher) {
        fakeRepository.saveTrip(createSampleTrip(id = "trip_stop"))
        viewModel.loadTrip("trip_stop")
        advanceUntilIdle()

        // Primer "Simular viaje": tiene que frenar en el cargador y quedarse ahi
        viewModel.toggleSimulation()
        advanceUntilIdle()

        val atCharger = viewModel.uiState.value
        assertFalse("La simulación debe frenar en el cargador", atCharger.isSimulationPlaying)
        assertTrue(atCharger.isCharging)
        assertEquals(atCharger.stopProgress!!, atCharger.progress, 0.0001f)
        assertEquals("Batería planificada al salir del cargador", 80, atCharger.currentBatteryPercent)
        assertEquals(0, atCharger.currentSpeedKmH)
        assertFalse(atCharger.isArrived)

        // Segundo "Simular viaje": sigue hasta el destino
        viewModel.toggleSimulation()
        advanceUntilIdle()

        val atDestination = viewModel.uiState.value
        assertTrue(atDestination.isArrived)
        assertFalse(atDestination.isCharging)
    }

    @Test
    fun updateBattery_recalculatesIfTheNextStopIsReachable() = runTest(testDispatcher) {
        // RF03: hasta Chascomús hay ~118 km y el Kona gasta ~31 % en ese tramo
        fakeRepository.saveTrip(createSampleTrip(id = "trip_rf03", currentBatteryPercent = 70))
        viewModel.loadTrip("trip_rf03")
        advanceUntilIdle()

        val withEnough = viewModel.uiState.value.reachEstimate!!
        assertEquals(sampleStation.name, withEnough.targetName)
        assertTrue(withEnough.reachesWithReserve)

        // El conductor ingresa 20 %: ya no alcanza para llegar al cargador
        viewModel.updateBattery(20)
        advanceUntilIdle()

        val withLow = viewModel.uiState.value.reachEstimate!!
        assertFalse(withLow.reaches)
        assertTrue(withLow.arrivalBatteryPercent < 0)
    }

    @Test
    fun simulation_stopsAtEveryCharger_inOrder() = runTest(testDispatcher) {
        // Viaje con dos paradas: Chascomús y Dolores
        val secondStation = sampleStation.copy(
            id = "station_dolores",
            name = "Dolores",
            latitude = -36.3132,
            longitude = -57.6792
        )
        val twoStops = sampleRoutePlan.copy(
            stops = listOf(
                sampleRoutePlan.stops[0],
                TripStop(
                    id = "stop_2",
                    station = secondStation,
                    orderIndex = 2,
                    stopDurationMinutes = 20,
                    arrivalBatteryPercent = 60,
                    departureBatteryPercent = 90,
                    chargedEnergyKwh = 19.2
                )
            ),
            polylinePoints = listOf(
                Pair(-34.6037, -58.3816),
                Pair(-35.5786, -58.0125),
                Pair(-36.3132, -57.6792),
                Pair(-38.0055, -57.5562)
            )
        )
        fakeRepository.saveTrip(createSampleTrip(id = "trip_two_stops").copy(routePlan = twoStops))
        viewModel.loadTrip("trip_two_stops")
        advanceUntilIdle()

        // Primera parada
        viewModel.toggleSimulation()
        advanceUntilIdle()
        val first = viewModel.uiState.value
        assertTrue(first.isCharging)
        assertEquals("stop_1", first.nextStop?.id)
        assertEquals(80, first.currentBatteryPercent)

        // Segunda parada
        viewModel.toggleSimulation()
        advanceUntilIdle()
        val second = viewModel.uiState.value
        assertTrue(second.isCharging)
        assertEquals("stop_2", second.nextStop?.id)
        assertEquals(90, second.currentBatteryPercent)
        assertTrue(second.progress > first.progress)

        // Destino
        viewModel.toggleSimulation()
        advanceUntilIdle()
        val end = viewModel.uiState.value
        assertTrue(end.isArrived)
        assertEquals(null, end.nextStop)
    }
}
