package com.chargeroute.app.domain.model

/**
 * Energia de un viaje para la pantalla de llegada.
 * Kotlin puro, sin Android.
 */
object TripEnergy {

    // kWh gastados en el viaje: distancia x consumo cada 100 km
    fun usedKwh(distanceKm: Double, consumptionKwhPer100Km: Double): Double {
        return distanceKm * consumptionKwhPer100Km / 100.0
    }

    // kWh que quedan en la bateria con ese porcentaje
    fun remainingKwh(batteryPercent: Int, usableBatteryKwh: Double): Double {
        return batteryPercent / 100.0 * usableBatteryKwh
    }
}
