package com.chargeroute.app

import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.presentation.screens.carsetup.CarSetupUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class CarSetupUiStateTest {

    // Auto de 50 kWh que gasta 20 kWh/100 km
    private val vehicle = Vehicle(
        id = "test_car",
        brand = "Marca",
        model = "Modelo",
        year = 2023,
        version = "Test",
        usableBatteryKwh = 50.0,
        wltpRangeKm = 250.0,
        supportedConnectors = listOf(ConnectorType.CCS2),
        maxDcKw = 100.0,
        baseConsumptionKwhPer100Km = 20.0
    )

    @Test
    fun estimatedRangeWithoutAc() {
        // 80 % de 50 kWh = 40 kWh; 40 / 20 * 100 = 200 km
        val state = CarSetupUiState(selectedVehicle = vehicle, initialBatteryPercent = 80, acActive = false)

        assertEquals(200.0, state.estimatedRangeKm, 0.01)
    }

    @Test
    fun acRaisesConsumptionAndLowersRange() {
        val state = CarSetupUiState(selectedVehicle = vehicle, initialBatteryPercent = 80, acActive = true)

        // 20 * 1,10 = 22 kWh/100 km; 40 / 22 * 100 = 181,8 km
        assertEquals(22.0, state.effectiveConsumptionKwhPer100Km, 0.001)
        assertEquals(181.82, state.estimatedRangeKm, 0.01)
    }

    @Test
    fun withoutVehicleEverythingIsZero() {
        val state = CarSetupUiState()

        assertEquals(0.0, state.usableBatteryKwh, 0.0)
        assertEquals(0.0, state.effectiveConsumptionKwhPer100Km, 0.0)
        assertEquals(0.0, state.estimatedRangeKm, 0.0)
    }
}
