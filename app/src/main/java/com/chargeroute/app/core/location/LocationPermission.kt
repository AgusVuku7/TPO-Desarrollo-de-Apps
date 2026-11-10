package com.chargeroute.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Un solo lugar para saber si hay permiso de ubicacion y cuales se piden.
 */
object LocationPermission {

    val REQUESTED = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Alcanza con la ubicacion aproximada o la precisa
    fun isGranted(context: Context): Boolean = REQUESTED.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
