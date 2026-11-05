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

        // ---- Autos agregados (catálogo completo del mercado argentino) ----
        Vehicle(
            id = "renault_kangoo_etech",
            brand = "Renault",
            model = "Kangoo E-Tech",
            year = 2024,
            version = "45 kWh",
            usableBatteryKwh = 45.0,
            wltpRangeKm = 300.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 80.0,
            baseConsumptionKwhPer100Km = 15.0
        ),
        Vehicle(
            id = "peugeot_e208",
            brand = "Peugeot",
            model = "e-208",
            year = 2024,
            version = "GT - 50 kWh",
            usableBatteryKwh = 50.0,
            wltpRangeKm = 362.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 100.0,
            baseConsumptionKwhPer100Km = 14.5
        ),
        Vehicle(
            id = "ford_mustang_mache_gt",
            brand = "Ford",
            model = "Mustang Mach-E",
            year = 2024,
            version = "GT Performance - 91 kWh",
            usableBatteryKwh = 91.0,
            wltpRangeKm = 490.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 20.0
        ),
        Vehicle(
            id = "hyundai_ioniq5",
            brand = "Hyundai",
            model = "Ioniq 5",
            year = 2024,
            version = "Long Range - 77.4 kWh",
            usableBatteryKwh = 77.4,
            wltpRangeKm = 507.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 233.0,
            baseConsumptionKwhPer100Km = 16.8
        ),
        Vehicle(
            id = "audi_etron_sportback",
            brand = "Audi",
            model = "e-tron Sportback",
            year = 2023,
            version = "55 quattro - 95 kWh",
            usableBatteryKwh = 95.0,
            wltpRangeKm = 446.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 21.8
        ),
        Vehicle(
            id = "audi_rs_etron_gt",
            brand = "Audi",
            model = "RS e-tron GT",
            year = 2024,
            version = "93.4 kWh",
            usableBatteryKwh = 83.7,
            wltpRangeKm = 472.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 270.0,
            baseConsumptionKwhPer100Km = 20.2
        ),
        Vehicle(
            id = "mercedes_eqb_350",
            brand = "Mercedes-Benz",
            model = "EQB",
            year = 2024,
            version = "350 4MATIC - 66.5 kWh",
            usableBatteryKwh = 66.5,
            wltpRangeKm = 423.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 100.0,
            baseConsumptionKwhPer100Km = 18.1
        ),
        Vehicle(
            id = "mercedes_eqe_350",
            brand = "Mercedes-Benz",
            model = "EQE",
            year = 2024,
            version = "350+ - 90.6 kWh",
            usableBatteryKwh = 90.6,
            wltpRangeKm = 645.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 170.0,
            baseConsumptionKwhPer100Km = 16.5
        ),
        Vehicle(
            id = "mercedes_eqs_450",
            brand = "Mercedes-Benz",
            model = "EQS",
            year = 2024,
            version = "450+ - 107.8 kWh",
            usableBatteryKwh = 107.8,
            wltpRangeKm = 770.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 200.0,
            baseConsumptionKwhPer100Km = 16.0
        ),
        Vehicle(
            id = "bmw_i4_edrive40",
            brand = "BMW",
            model = "i4",
            year = 2024,
            version = "eDrive40 - 83.9 kWh",
            usableBatteryKwh = 80.7,
            wltpRangeKm = 590.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 205.0,
            baseConsumptionKwhPer100Km = 16.1
        ),
        Vehicle(
            id = "bmw_ix3",
            brand = "BMW",
            model = "iX3",
            year = 2024,
            version = "M Sport - 80 kWh",
            usableBatteryKwh = 74.0,
            wltpRangeKm = 460.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 18.6
        ),
        Vehicle(
            id = "bmw_ix_xdrive50",
            brand = "BMW",
            model = "iX",
            year = 2024,
            version = "xDrive50 - 111.5 kWh",
            usableBatteryKwh = 105.2,
            wltpRangeKm = 630.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 200.0,
            baseConsumptionKwhPer100Km = 19.8
        ),
        Vehicle(
            id = "volvo_xc40_recharge",
            brand = "Volvo",
            model = "EX40 / XC40 Recharge",
            year = 2024,
            version = "Twin Motor - 78 kWh",
            usableBatteryKwh = 75.0,
            wltpRangeKm = 537.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 200.0,
            baseConsumptionKwhPer100Km = 17.5
        ),
        Vehicle(
            id = "coradir_tito_ext",
            brand = "Coradir",
            model = "Tito Extended Range",
            year = 2024,
            version = "21 kWh",
            usableBatteryKwh = 21.0,
            wltpRangeKm = 300.0,
            supportedConnectors = listOf(ConnectorType.TYPE_2, ConnectorType.SCHUKO),
            maxDcKw = 3.3,
            baseConsumptionKwhPer100Km = 7.5
        ),
        Vehicle(
            id = "coradir_tita",
            brand = "Coradir",
            model = "Tita Pick-Up",
            year = 2024,
            version = "Utilitario - 8 kWh",
            usableBatteryKwh = 8.0,
            wltpRangeKm = 100.0,
            supportedConnectors = listOf(ConnectorType.TYPE_2, ConnectorType.SCHUKO),
            maxDcKw = 2.5,
            baseConsumptionKwhPer100Km = 9.0
        ),
        Vehicle(
            id = "sero_electric_sedan",
            brand = "Sero Electric",
            model = "Sedan",
            year = 2024,
            version = "Urbano - 6 kWh",
            usableBatteryKwh = 6.0,
            wltpRangeKm = 100.0,
            supportedConnectors = listOf(ConnectorType.SCHUKO),
            maxDcKw = 2.0,
            baseConsumptionKwhPer100Km = 6.0
        ),
        Vehicle(
            id = "jmev_easy3",
            brand = "JMEV",
            model = "Easy 3",
            year = 2026,
            version = "30.2 kWh",
            usableBatteryKwh = 30.24,
            wltpRangeKm = 330.0,
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
            wltpRangeKm = 280.0,
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
            wltpRangeKm = 380.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 85.0,
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "byd_dolphin_gs",
            brand = "BYD",
            model = "Dolphin",
            year = 2026,
            version = "GS - 60.4 kWh",
            usableBatteryKwh = 60.48,
            wltpRangeKm = 427.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 88.0,
            baseConsumptionKwhPer100Km = 15.2
        ),
        Vehicle(
            id = "byd_seal_awd",
            brand = "BYD",
            model = "Seal",
            year = 2026,
            version = "Excellence AWD - 82.5 kWh",
            usableBatteryKwh = 82.5,
            wltpRangeKm = 520.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 18.2
        ),
        Vehicle(
            id = "byd_yuan_pro_gs",
            brand = "BYD",
            model = "Yuan Pro",
            year = 2026,
            version = "GS - 45.1 kWh",
            usableBatteryKwh = 45.12,
            wltpRangeKm = 380.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 85.0,
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "gwm_ora_03",
            brand = "GWM",
            model = "Ora 03",
            year = 2026,
            version = "48 kWh",
            usableBatteryKwh = 45.4,
            wltpRangeKm = 310.0,
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
            wltpRangeKm = 442.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 80.0,
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "gac_aion_ut",
            brand = "GAC",
            model = "Aion UT",
            year = 2026,
            version = "Premium - 60 kWh",
            usableBatteryKwh = 60.0,
            wltpRangeKm = 500.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 80.0,
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "geely_ex5_pro",
            brand = "Geely",
            model = "EX5",
            year = 2026,
            version = "Pro - 60.2 kWh",
            usableBatteryKwh = 60.22,
            wltpRangeKm = 430.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 100.0,
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "chevrolet_bolt_euv",
            brand = "Chevrolet",
            model = "Bolt EUV",
            year = 2025,
            version = "Premier - 65 kWh",
            usableBatteryKwh = 65.0,
            wltpRangeKm = 397.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 55.0,
            baseConsumptionKwhPer100Km = 16.2
        ),
        Vehicle(
            id = "chevrolet_spark_euv",
            brand = "Chevrolet",
            model = "Spark EUV",
            year = 2026,
            version = "42 kWh",
            usableBatteryKwh = 42.0,
            wltpRangeKm = 360.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 50.0,
            baseConsumptionKwhPer100Km = 14.0
        ),
        Vehicle(
            id = "volvo_ex30_core",
            brand = "Volvo",
            model = "EX30",
            year = 2026,
            version = "Core - 51 kWh",
            usableBatteryKwh = 51.0,
            wltpRangeKm = 405.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 134.0,
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "toyota_bz4x",
            brand = "Toyota",
            model = "bZ4X",
            year = 2026,
            version = "73 kWh",
            usableBatteryKwh = 73.0,
            wltpRangeKm = 515.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 150.0,
            baseConsumptionKwhPer100Km = 18.0
        ),
        Vehicle(
            id = "porsche_taycan_4s",
            brand = "Porsche",
            model = "Taycan",
            year = 2024,
            version = "4S Performance Plus - 93.4 kWh",
            usableBatteryKwh = 83.7,
            wltpRangeKm = 512.0,
            supportedConnectors = listOf(ConnectorType.CCS2, ConnectorType.TYPE_2),
            maxDcKw = 270.0,
            baseConsumptionKwhPer100Km = 20.4
        ),
        Vehicle(
            id = "audi_q6_etron",
            brand = "Audi",
            model = "Q6 e-tron",
            year = 2026,
            version = "50 quattro - 100 kWh",
            usableBatteryKwh = 94.9,
            wltpRangeKm = 625.0,
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
