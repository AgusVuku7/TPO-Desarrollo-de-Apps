package com.chargeroute.app.data.repository

import com.chargeroute.app.data.local.datastore.UserPreferencesDataStore
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Vehicle
import com.chargeroute.app.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VehicleRepositoryImpl(
    private val preferencesDataStore: UserPreferencesDataStore
) : VehicleRepository {

    // Catálogo de vehículos eléctricos comercializados y presentes en Argentina
    private val catalog = listOf(
        Vehicle(
            id = "renault_kwid_etech",
            brand = "Renault",
            model = "Kwid E-Tech",
            year = 2024,
            version = "Intens - 26.8 kWh",
            usableBatteryKwh = 26.8,
            wltpRangeKm = 298.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 30.0,
            baseConsumptionKwhPer100Km = 14.2
        ),
        Vehicle(
            id = "renault_megane_etech",
            brand = "Renault",
            model = "Megane E-Tech",
            year = 2024,
            version = "EV60 Optimum Charge - 60 kWh",
            usableBatteryKwh = 60.0,
            wltpRangeKm = 450.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 130.0,
            baseConsumptionKwhPer100Km = 15.5
        ),
        Vehicle(
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
        ),
        Vehicle(
            id = "nissan_leaf_2023",
            brand = "Nissan",
            model = "Leaf",
            year = 2023,
            version = "Tekna - 40 kWh",
            usableBatteryKwh = 40.0,
            wltpRangeKm = 270.0,
            supportedConnectors = listOf(ConnectorType.CHADEMO, ConnectorType.TYPE_2),
            maxDcKw = 50.0,
            baseConsumptionKwhPer100Km = 16.8
        ),
        Vehicle(
            id = "peugeot_e2008",
            brand = "Peugeot",
            model = "e-2008",
            year = 2024,
            version = "GT - 54 kWh",
            usableBatteryKwh = 54.0,
            wltpRangeKm = 406.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 100.0,
            baseConsumptionKwhPer100Km = 15.8
        ),
        Vehicle(
            id = "ford_mustang_mache",
            brand = "Ford",
            model = "Mustang Mach-E",
            year = 2024,
            version = "Premium AWD - 91 kWh",
            usableBatteryKwh = 91.0,
            wltpRangeKm = 540.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 18.2
        ),
        Vehicle(
            id = "audi_etron_55",
            brand = "Audi",
            model = "e-tron",
            year = 2023,
            version = "55 quattro - 95 kWh",
            usableBatteryKwh = 95.0,
            wltpRangeKm = 436.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 22.0
        ),
        Vehicle(
            id = "mercedes_eqa_350",
            brand = "Mercedes-Benz",
            model = "EQA",
            year = 2024,
            version = "350 4MATIC - 66.5 kWh",
            usableBatteryKwh = 66.5,
            wltpRangeKm = 430.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 100.0,
            baseConsumptionKwhPer100Km = 17.5
        ),
        Vehicle(
            id = "volvo_ex30",
            brand = "Volvo",
            model = "EX30",
            year = 2024,
            version = "Extended Range - 69 kWh",
            usableBatteryKwh = 69.0,
            wltpRangeKm = 476.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 153.0,
            baseConsumptionKwhPer100Km = 15.7
        ),
        Vehicle(
            id = "coradir_tito",
            brand = "Coradir",
            model = "Tito",
            year = 2024,
            version = "5P Nacional - 8 kWh",
            usableBatteryKwh = 8.0,
            wltpRangeKm = 100.0,
            supportedConnectors = listOf(ConnectorType.TYPE_2, ConnectorType.SCHUKO),
            maxDcKw = 2.5,
            baseConsumptionKwhPer100Km = 8.0
        )
    )

    override fun getAvailableVehicles(): List<Vehicle> = catalog

    override fun getDefaultVehicle(): Flow<Vehicle> {
        return preferencesDataStore.selectedVehicleId.map { id ->
            catalog.firstOrNull { it.id == id } ?: catalog.first { it.id == "hyundai_kona_2023" }
        }
    }

    override suspend fun saveSelectedVehicle(vehicle: Vehicle) {
        preferencesDataStore.setSelectedVehicleId(vehicle.id)
    }
}
