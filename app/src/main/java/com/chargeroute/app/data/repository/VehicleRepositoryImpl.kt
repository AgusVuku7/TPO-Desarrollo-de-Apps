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
        ),

        // ---- Autos agregados (lista del mercado argentino, consultada en 11/2026) ----
        // La autonomia es la declarada por cada marca y puede ser NEDC, CLTC o WLTP (se aclara en cada auto).
        // Los ciclos NEDC y CLTC dan numeros mas altos que el uso real en ruta.
        // El consumo sale del catalogo de mielectrico.ar, salvo donde dice "estimado".
        Vehicle(
            id = "jmev_easy3",
            brand = "JMEV",
            model = "Easy 3",
            year = 2026,
            version = "30.2 kWh",
            usableBatteryKwh = 30.24,
            wltpRangeKm = 330.0, // CLTC
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 40.0,
            baseConsumptionKwhPer100Km = 10.8
        ),
        Vehicle(
            id = "byd_dolphin_mini_gl",
            brand = "BYD",
            model = "Dolphin Mini",
            year = 2026,
            version = "GL - 30.1 kWh",
            usableBatteryKwh = 30.08,
            wltpRangeKm = 280.0, // NEDC
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 65.0,
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "byd_dolphin_mini_gs",
            brand = "BYD",
            model = "Dolphin Mini",
            year = 2026,
            version = "GS - 43.2 kWh",
            usableBatteryKwh = 43.2,
            wltpRangeKm = 380.0, // NEDC
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 85.0,
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "byd_yuan_pro_gs",
            brand = "BYD",
            model = "Yuan Pro",
            year = 2026,
            version = "GS - 45.1 kWh",
            usableBatteryKwh = 45.12,
            wltpRangeKm = 380.0, // NEDC
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2), // conector estimado
            maxDcKw = 85.0, // estimado
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "gwm_ora_03",
            brand = "GWM",
            model = "Ora 03",
            year = 2026,
            version = "48 kWh",
            usableBatteryKwh = 45.4, // 48 kWh totales, 45.4 utiles
            wltpRangeKm = 310.0, // WLTP
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 60.0,
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "gac_aion_es",
            brand = "GAC",
            model = "Aion ES",
            year = 2026,
            version = "55.2 kWh",
            usableBatteryKwh = 55.2,
            wltpRangeKm = 442.0, // ciclo no informado
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2), // conector estimado
            maxDcKw = 80.0, // estimado
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "gac_aion_ut",
            brand = "GAC",
            model = "Aion UT",
            year = 2026,
            version = "Premium - 60 kWh",
            usableBatteryKwh = 60.0,
            wltpRangeKm = 500.0, // ciclo no informado
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2), // conector estimado
            maxDcKw = 80.0, // estimado
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "geely_ex5_pro",
            brand = "Geely",
            model = "EX5",
            year = 2026,
            version = "Pro - 60.2 kWh",
            usableBatteryKwh = 60.22,
            wltpRangeKm = 430.0, // ciclo no informado
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2), // conector estimado
            maxDcKw = 100.0, // estimado
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "chevrolet_spark_euv",
            brand = "Chevrolet",
            model = "Spark EUV",
            year = 2026,
            version = "42 kWh",
            usableBatteryKwh = 42.0,
            wltpRangeKm = 360.0, // NEDC
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2), // conector estimado
            maxDcKw = 50.0, // estimado
            baseConsumptionKwhPer100Km = 14.0 // estimado
        ),
        Vehicle(
            id = "volvo_ex30_core",
            brand = "Volvo",
            model = "EX30",
            year = 2026,
            version = "Core - 51 kWh",
            usableBatteryKwh = 51.0,
            wltpRangeKm = 405.0, // ciclo no informado
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 134.0, // estimado
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "toyota_bz4x",
            brand = "Toyota",
            model = "bZ4X",
            year = 2026,
            version = "73 kWh",
            usableBatteryKwh = 73.0,
            wltpRangeKm = 515.0, // WLTP Europa
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "audi_q6_etron",
            brand = "Audi",
            model = "Q6 e-tron",
            year = 2026,
            version = "50 quattro - 100 kWh",
            usableBatteryKwh = 94.9, // 100 kWh totales, 94.9 utiles
            wltpRangeKm = 625.0, // WLTP Europa
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 270.0,
            baseConsumptionKwhPer100Km = 20.0
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
