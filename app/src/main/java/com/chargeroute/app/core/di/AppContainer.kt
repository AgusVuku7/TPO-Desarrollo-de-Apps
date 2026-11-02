package com.chargeroute.app.core.di

import android.content.Context
import com.chargeroute.app.core.location.LocationClient
import com.chargeroute.app.core.network.NetworkConnectivityObserver
import com.chargeroute.app.core.sensor.AmbientLightSensorManager
import com.chargeroute.app.data.local.ChargeRouteDatabase
import com.chargeroute.app.data.local.datastore.UserPreferencesDataStore
import com.chargeroute.app.data.repository.RouteRepositoryImpl
import com.chargeroute.app.data.repository.TripRepositoryImpl
import com.chargeroute.app.data.repository.VehicleRepositoryImpl
import com.chargeroute.app.domain.repository.RouteRepository
import com.chargeroute.app.domain.repository.TripRepository
import com.chargeroute.app.domain.repository.VehicleRepository
import com.chargeroute.app.domain.usecase.CalculateRouteUseCase
import com.chargeroute.app.domain.usecase.GetOfflineRouteUseCase
import com.chargeroute.app.domain.usecase.GetVehicleCatalogUseCase
import com.chargeroute.app.domain.usecase.SaveTripUseCase
import com.chargeroute.app.domain.usecase.UpdateBatteryUseCase

interface AppContainer {
    val database: ChargeRouteDatabase
    val userPreferences: UserPreferencesDataStore
    val networkObserver: NetworkConnectivityObserver
    val lightSensorManager: AmbientLightSensorManager
    val locationClient: LocationClient

    val vehicleRepository: VehicleRepository
    val routeRepository: RouteRepository
    val tripRepository: TripRepository

    val calculateRouteUseCase: CalculateRouteUseCase
    val updateBatteryUseCase: UpdateBatteryUseCase
    val getOfflineRouteUseCase: GetOfflineRouteUseCase
    val saveTripUseCase: SaveTripUseCase
    val getVehicleCatalogUseCase: GetVehicleCatalogUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: ChargeRouteDatabase by lazy {
        ChargeRouteDatabase.getInstance(context)
    }

    override val userPreferences: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val networkObserver: NetworkConnectivityObserver by lazy {
        NetworkConnectivityObserver(context)
    }

    override val lightSensorManager: AmbientLightSensorManager by lazy {
        AmbientLightSensorManager(context)
    }

    override val locationClient: LocationClient by lazy {
        LocationClient(context)
    }

    override val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(userPreferences)
    }

    override val routeRepository: RouteRepository by lazy {
        RouteRepositoryImpl(database, networkObserver)
    }

    override val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(database, vehicleRepository)
    }

    override val calculateRouteUseCase: CalculateRouteUseCase by lazy {
        CalculateRouteUseCase(routeRepository)
    }

    override val updateBatteryUseCase: UpdateBatteryUseCase by lazy {
        UpdateBatteryUseCase(tripRepository)
    }

    override val getOfflineRouteUseCase: GetOfflineRouteUseCase by lazy {
        GetOfflineRouteUseCase(routeRepository)
    }

    override val saveTripUseCase: SaveTripUseCase by lazy {
        SaveTripUseCase(tripRepository)
    }

    override val getVehicleCatalogUseCase: GetVehicleCatalogUseCase by lazy {
        GetVehicleCatalogUseCase(vehicleRepository)
    }
}
