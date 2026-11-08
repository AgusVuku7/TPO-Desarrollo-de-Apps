package com.chargeroute.app.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Estrategia Offline First: sin conexion se usan los cargadores guardados en Room.
 * Esto dice desde cuando son esos datos y si conviene avisar que pueden estar viejos.
 */
object DataFreshness {

    // A partir de estos dias los datos de los cargadores se consideran desactualizados
    const val STALE_AFTER_DAYS = 30

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Fecha del dato mas viejo entre las paradas y los cargadores cercanos (null si no hay)
    fun oldestStationUpdate(plan: RoutePlan): Long? =
        (plan.stops.map { it.station } + plan.nearbyStations)
            .map { it.lastUpdatedMillis }
            .filter { it > 0 }
            .minOrNull()

    fun isStale(updatedMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis - updatedMillis > STALE_AFTER_DAYS * 24L * 60 * 60 * 1000

    fun formatDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateFormatter.format(Instant.ofEpochMilli(millis).atZone(zone))
}
