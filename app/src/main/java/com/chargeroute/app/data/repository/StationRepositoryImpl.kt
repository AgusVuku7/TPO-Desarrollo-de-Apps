package com.chargeroute.app.data.repository

import com.chargeroute.app.core.network.NetworkConnectivityObserver
import com.chargeroute.app.data.local.dao.StationDao
import com.chargeroute.app.data.mapper.StationMapper
import com.chargeroute.app.data.remote.api.OpenChargeMapApi
import com.chargeroute.app.domain.model.ConnectorType
import com.chargeroute.app.domain.model.Station
import com.chargeroute.app.domain.model.StationFilter
import com.chargeroute.app.domain.repository.StationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StationRepositoryImpl(
    private val api: OpenChargeMapApi,
    private val stationDao: StationDao,
    private val networkObserver: NetworkConnectivityObserver,
    private val apiKey: String
) : StationRepository {

    override suspend fun getStationsAlongRoute(
        route: List<Pair<Double, Double>>,
        vehicleConnectors: List<ConnectorType>
    ): List<Station> = withContext(Dispatchers.IO) {
        val fromApi = if (networkObserver.isCurrentlyConnected && apiKey.isNotBlank()) {
            fetchFromApi(route)
        } else {
            null
        }

        if (fromApi != null) {
            // Offline First: lo que llega de la API se guarda para poder usarlo sin conexion
            stationDao.insertStations(fromApi.map { StationMapper.toEntity(it) })
            StationFilter.alongRoute(fromApi, route, vehicleConnectors)
        } else {
            getCachedStationsAlongRoute(route, vehicleConnectors)
        }
    }

    override suspend fun getCachedStationsAlongRoute(
        route: List<Pair<Double, Double>>,
        vehicleConnectors: List<ConnectorType>
    ): List<Station> = withContext(Dispatchers.IO) {
        val cached = stationDao.getAllStations().map { StationMapper.fromEntity(it) }
        StationFilter.alongRoute(cached, route, vehicleConnectors)
    }

    // Una sola consulta: todas las estaciones del rectangulo que rodea la ruta.
    // Devuelve null si la API fallo, para que se use lo guardado en Room.
    private suspend fun fetchFromApi(route: List<Pair<Double, Double>>): List<Station>? {
        if (route.isEmpty()) return emptyList()
        return try {
            val response = api.getStationsInArea(boundingBox = boundingBox(route), apiKey = apiKey)
            if (response.isSuccessful) {
                response.body().orEmpty().mapNotNull { StationMapper.fromOcmDto(it) }
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    // Rectangulo que contiene toda la ruta, con un margen para no perder estaciones del borde
    private fun boundingBox(route: List<Pair<Double, Double>>): String {
        val margin = 0.15 // grados, unos 15 km
        val minLat = route.minOf { it.first } - margin
        val maxLat = route.maxOf { it.first } + margin
        val minLon = route.minOf { it.second } - margin
        val maxLon = route.maxOf { it.second } + margin
        return "($maxLat,$minLon),($minLat,$maxLon)"
    }
}
