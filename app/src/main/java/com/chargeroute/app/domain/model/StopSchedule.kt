package com.chargeroute.app.domain.model

object StopSchedule {
    // Minutos desde la salida hasta llegar a cada parada (manejo hasta ahi + cargas anteriores)
    fun minutesToEachStop(
        stopKms: List<Double>,
        chargeMinutes: List<Int>,
        routeKm: Double,
        driveMinutes: Int
    ): List<Int> {
        var previousChargeMinutes = 0
        return stopKms.mapIndexed { index, km ->
            val driveToStop = if (routeKm > 0) ((km / routeKm) * driveMinutes).toInt() else 0
            val minutesToStop = driveToStop + previousChargeMinutes
            previousChargeMinutes += chargeMinutes[index]
            minutesToStop
        }
    }
}
