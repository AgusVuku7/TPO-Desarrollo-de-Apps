package com.chargeroute.app.domain.model

// Regla de negocio de RF02: que estaciones se muestran para una ruta.
// Es codigo Kotlin puro (sin Android), asi se puede probar con tests unitarios.
object StationFilter {

    // Una estacion "esta sobre la ruta" si queda a menos de esta distancia del camino
    const val MAX_DISTANCE_FROM_ROUTE_KM = 15.0

    // Para no recorrer miles de puntos por cada estacion, la ruta se reduce a este maximo
    private const val MAX_ROUTE_POINTS = 300

    /**
     * Devuelve las estaciones compatibles con los conectores del auto que estan
     * cerca de la ruta, ordenadas desde el origen hacia el destino.
     */
    fun alongRoute(
        stations: List<Station>,
        route: List<Pair<Double, Double>>,
        vehicleConnectors: List<ConnectorType>,
        maxDistanceKm: Double = MAX_DISTANCE_FROM_ROUTE_KM
    ): List<Station> {
        if (route.size < 2) return emptyList()
        val shortRoute = simplify(route, MAX_ROUTE_POINTS)

        return stations
            .filter { it.isCompatibleWith(vehicleConnectors) }
            .map { station -> station to RouteGeometry.nearestOnRoute(shortRoute, station.latitude, station.longitude) }
            .filter { (_, match) -> match.distanceToRouteKm <= maxDistanceKm }
            .sortedBy { (_, match) -> match.progress }
            .map { (station, _) -> station }
    }

    // Se queda con uno de cada N puntos (siempre conserva el primero y el ultimo)
    fun simplify(points: List<Pair<Double, Double>>, maxPoints: Int): List<Pair<Double, Double>> {
        if (points.size <= maxPoints) return points
        val step = (points.size + maxPoints - 1) / maxPoints
        val result = points.filterIndexed { index, _ -> index % step == 0 }.toMutableList()
        if (result.last() != points.last()) result.add(points.last())
        return result
    }
}
