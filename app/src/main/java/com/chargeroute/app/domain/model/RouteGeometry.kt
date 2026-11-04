package com.chargeroute.app.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Resultado de buscar el punto de la ruta mas cercano a una posicion
data class RouteMatch(
    val progress: Float, // 0.0 = origen, 1.0 = destino
    val distanceToRouteKm: Double
)

// Cuentas para que el puntero siempre quede arriba de la linea de la ruta
object RouteGeometry {

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun totalKm(points: List<Pair<Double, Double>>): Double {
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += distanceKm(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second)
        }
        return total
    }

    // Devuelve el punto de la ruta que esta a ese porcentaje del recorrido real en km
    fun pointAtProgress(points: List<Pair<Double, Double>>, progress: Float): Pair<Double, Double> {
        if (points.isEmpty()) return Pair(-34.6037, -58.3816)
        if (points.size == 1 || progress <= 0f) return points.first()
        if (progress >= 1f) return points.last()

        val targetKm = totalKm(points) * progress
        var walkedKm = 0.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val segmentKm = distanceKm(a.first, a.second, b.first, b.second)
            if (walkedKm + segmentKm >= targetKm) {
                val fraction = if (segmentKm > 0) (targetKm - walkedKm) / segmentKm else 0.0
                return Pair(
                    a.first + (b.first - a.first) * fraction,
                    a.second + (b.second - a.second) * fraction
                )
            }
            walkedKm += segmentKm
        }
        return points.last()
    }

    // Hacia donde apunta la ruta en ese punto, en grados (0 = norte, 90 = este)
    fun bearingAtProgress(points: List<Pair<Double, Double>>, progress: Float): Float {
        if (points.size < 2) return 0f
        val total = totalKm(points)
        // Se mira 100 metros mas adelante para que no tiemble en cada puntito de la linea
        val lookAhead = if (total > 0) (0.1 / total).toFloat() else 0f
        val from = pointAtProgress(points, if (progress + lookAhead > 1f) 1f - lookAhead else progress)
        val to = pointAtProgress(points, if (progress + lookAhead > 1f) 1f else progress + lookAhead)

        val lat1 = Math.toRadians(from.first)
        val lat2 = Math.toRadians(to.first)
        val dLon = Math.toRadians(to.second - from.second)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    // Busca el lugar de la ruta mas cercano a una posicion (GPS, cargador, etc)
    fun nearestOnRoute(points: List<Pair<Double, Double>>, lat: Double, lon: Double): RouteMatch {
        if (points.size < 2) return RouteMatch(0f, Double.MAX_VALUE)

        val total = totalKm(points)
        var walkedKm = 0.0
        var bestDistance = Double.MAX_VALUE
        var bestWalkedKm = 0.0

        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val segmentKm = distanceKm(a.first, a.second, b.first, b.second)

            // Proyeccion simple sobre el segmento (para tramos cortos alcanza)
            val scale = cos(Math.toRadians(lat))
            val abX = (b.second - a.second) * scale
            val abY = b.first - a.first
            val apX = (lon - a.second) * scale
            val apY = lat - a.first
            val lengthSquared = abX * abX + abY * abY
            val fraction = if (lengthSquared > 0) {
                ((apX * abX + apY * abY) / lengthSquared).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            val pointLat = a.first + (b.first - a.first) * fraction
            val pointLon = a.second + (b.second - a.second) * fraction
            val distance = distanceKm(lat, lon, pointLat, pointLon)
            if (distance < bestDistance) {
                bestDistance = distance
                bestWalkedKm = walkedKm + segmentKm * fraction
            }
            walkedKm += segmentKm
        }

        val progress = if (total > 0) (bestWalkedKm / total).toFloat() else 0f
        return RouteMatch(progress.coerceIn(0f, 1f), bestDistance)
    }
}
