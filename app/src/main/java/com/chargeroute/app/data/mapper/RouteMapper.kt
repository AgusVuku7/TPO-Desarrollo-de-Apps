package com.chargeroute.app.data.mapper

import com.chargeroute.app.data.local.entity.RouteEntity
import com.chargeroute.app.data.local.entity.TripStopEntity
import com.chargeroute.app.domain.model.LocationPoint
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.TripClock
import com.chargeroute.app.domain.model.TripStop

object RouteMapper {

    fun toEntity(tripId: String, plan: RoutePlan): RouteEntity {
        return RouteEntity(
            id = "route_$tripId",
            tripId = tripId,
            encodedPolyline = plan.encodedPolyline,
            distanceKm = plan.distanceKm,
            totalDurationMinutes = plan.totalDurationMinutes,
            driveDurationMinutes = plan.driveDurationMinutes,
            chargeDurationMinutes = plan.chargeDurationMinutes,
            arrivalBatteryPercent = plan.arrivalBatteryPercent,
            consumptionKwhPer100Km = plan.consumptionKwhPer100Km,
            calculatedAtMillis = plan.calculatedAtMillis,
            directDistanceKm = plan.directDistanceKm
        )
    }

    fun toStopEntities(routeId: String, stops: List<TripStop>): List<TripStopEntity> {
        return stops.map { stop ->
            TripStopEntity(
                id = stop.id,
                routeId = routeId,
                stationId = stop.station.id,
                orderIndex = stop.orderIndex,
                stopDurationMinutes = stop.stopDurationMinutes,
                arrivalBatteryPercent = stop.arrivalBatteryPercent,
                departureBatteryPercent = stop.departureBatteryPercent,
                chargedEnergyKwh = stop.chargedEnergyKwh,
                arrivalTime = stop.arrivalTime,
                departureTime = stop.departureTime
            )
        }
    }

    fun fromEntities(
        origin: LocationPoint,
        destination: LocationPoint,
        routeEntity: RouteEntity,
        stopsEntities: List<TripStopEntity>,
        stationsMap: Map<String, Station>
    ): RoutePlan {
        val stops = stopsEntities.mapNotNull { stopEntity ->
            val station = stationsMap[stopEntity.stationId] ?: return@mapNotNull null
            TripStop(
                id = stopEntity.id,
                station = station,
                orderIndex = stopEntity.orderIndex,
                stopDurationMinutes = stopEntity.stopDurationMinutes,
                arrivalBatteryPercent = stopEntity.arrivalBatteryPercent,
                departureBatteryPercent = stopEntity.departureBatteryPercent,
                chargedEnergyKwh = stopEntity.chargedEnergyKwh,
                arrivalTime = stopEntity.arrivalTime,
                departureTime = stopEntity.departureTime
            )
        }

        val polylinePoints = decodePolyline(routeEntity.encodedPolyline)

        return RoutePlan(
            origin = origin,
            destination = destination,
            distanceKm = routeEntity.distanceKm,
            totalDurationMinutes = routeEntity.totalDurationMinutes,
            driveDurationMinutes = routeEntity.driveDurationMinutes,
            chargeDurationMinutes = routeEntity.chargeDurationMinutes,
            stops = stops,
            polylinePoints = polylinePoints,
            encodedPolyline = routeEntity.encodedPolyline,
            arrivalBatteryPercent = routeEntity.arrivalBatteryPercent,
            consumptionKwhPer100Km = routeEntity.consumptionKwhPer100Km,
            // Los horarios se recalculan a partir del momento en que se calculo la ruta
            calculatedAtMillis = routeEntity.calculatedAtMillis,
            directDistanceKm = routeEntity.directDistanceKm,
            departureTime = TripClock.at(routeEntity.calculatedAtMillis, 0),
            arrivalTime = TripClock.at(routeEntity.calculatedAtMillis, routeEntity.totalDurationMinutes),
            isLowBatteryWarning = routeEntity.arrivalBatteryPercent < 20,
            warningMessage = if (routeEntity.arrivalBatteryPercent < 20) {
                "Batería baja al llegar: ${routeEntity.arrivalBatteryPercent}%. Mantené la parada en ${stops.firstOrNull()?.station?.name ?: "estación de recarga"}."
            } else null
        )
    }

    // Hace el camino inverso a decodePolyline, para guardar en Room las rutas calculadas sin internet
    fun encodePolyline(points: List<Pair<Double, Double>>): String {
        val result = StringBuilder()
        var lastLat = 0
        var lastLng = 0
        for (point in points) {
            val lat = Math.round(point.first * 1E5).toInt()
            val lng = Math.round(point.second * 1E5).toInt()
            encodeValue(lat - lastLat, result)
            encodeValue(lng - lastLng, result)
            lastLat = lat
            lastLng = lng
        }
        return result.toString()
    }

    private fun encodeValue(value: Int, result: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else (value shl 1)
        while (v >= 0x20) {
            result.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        result.append((v + 63).toChar())
    }

    /**
     * Standard Polyline decoder for Google/OSRM 5-decimal polylines.
     */
    fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val poly = ArrayList<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val pLat = lat.toDouble() / 1E5
            val pLng = lng.toDouble() / 1E5
            poly.add(Pair(pLat, pLng))
        }
        return poly
    }
}
