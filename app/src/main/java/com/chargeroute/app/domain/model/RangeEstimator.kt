package com.chargeroute.app.domain.model

// Resultado de RF03: si con la bateria actual se llega al proximo objetivo
data class ReachEstimate(
    val targetName: String,        // proxima parada o destino
    val kmToTarget: Int,
    val arrivalBatteryPercent: Int, // puede ser negativo: indica cuanto falta
    val rangeKm: Int                // autonomia con la bateria actual
) {
    // Se llega (aunque sea justo)
    val reaches: Boolean
        get() = arrivalBatteryPercent >= 0

    // Se llega con la reserva de seguridad recomendada
    val reachesWithReserve: Boolean
        get() = arrivalBatteryPercent >= ChargingPlanner.SAFETY_RESERVE_PERCENT
}

/**
 * RF03: con el porcentaje de bateria que ingresa el conductor, calcula
 * hasta donde llega el auto y con cuanta bateria llega al proximo objetivo.
 * Kotlin puro, sin Android.
 */
object RangeEstimator {

    // Km que se pueden recorrer con ese porcentaje de bateria
    fun rangeKm(batteryPercent: Int, usableBatteryKwh: Double, consumptionKwhPer100Km: Double): Double {
        if (consumptionKwhPer100Km <= 0) return 0.0
        val energyKwh = batteryPercent / 100.0 * usableBatteryKwh
        return energyKwh / consumptionKwhPer100Km * 100.0
    }

    fun estimate(
        targetName: String,
        kmToTarget: Double,
        batteryPercent: Int,
        usableBatteryKwh: Double,
        consumptionKwhPer100Km: Double
    ): ReachEstimate {
        val percentNeeded = kmToTarget * consumptionKwhPer100Km / 100.0 / usableBatteryKwh * 100.0
        return ReachEstimate(
            targetName = targetName,
            kmToTarget = kmToTarget.toInt(),
            arrivalBatteryPercent = (batteryPercent - percentNeeded).toInt(),
            rangeKm = rangeKm(batteryPercent, usableBatteryKwh, consumptionKwhPer100Km).toInt()
        )
    }
}
