package com.chargeroute.app

import android.app.Application
import com.chargeroute.app.core.di.AppContainer
import com.chargeroute.app.core.di.DefaultAppContainer

class ChargeRouteApp : Application() {

    // Contenedor con las dependencias de toda la app (se crea una sola vez)
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
