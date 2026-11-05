package com.chargeroute.app

import android.app.Application
import com.chargeroute.app.core.di.AppContainer
import com.chargeroute.app.core.di.DefaultAppContainer

class ChargeRouteApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = DefaultAppContainer(this)
    }

    companion object {
        lateinit var instance: ChargeRouteApp
            private set
    }
}
