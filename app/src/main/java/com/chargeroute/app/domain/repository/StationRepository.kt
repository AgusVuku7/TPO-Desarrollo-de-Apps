package com.chargeroute.app.domain.repository

import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Station

interface StationRepository {
    /**
     * Estaciones compatibles cerca de la ruta.
     * Con conexion las pide a OpenChargeMap y las guarda en Room.
     * Sin conexion (o si la API falla) usa las que ya estaban guardadas.
     */
    suspend fun getStationsAlongRoute(
        route: List<Pair<Double, Double>>,
        vehicleConnectors: List<ConnectorType>
    ): List<Station>

    // Solo lee lo guardado en Room (para abrir viajes guardados sin conexion)
    suspend fun getCachedStationsAlongRoute(
        route: List<Pair<Double, Double>>,
        vehicleConnectors: List<ConnectorType>
    ): List<Station>
}
