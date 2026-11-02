package com.chargeroute.app.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manages the TYPE_LIGHT ambient sensor to adapt UI luminance automatically.
 * Implements RNF04 (Light/Dark adaptation) and RNF05 (Battery responsible use).
 */
class AmbientLightSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    val isSensorAvailable: Boolean
        get() = lightSensor != null

    /**
     * Flow of ambient lux readings.
     * Automatically registers listener upon subscription and unregisters when closed.
     */
    fun getAmbientLuxFlow(): Flow<Float> = callbackFlow {
        if (lightSensor == null || sensorManager == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { lux ->
                    trySend(lux)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not critical for ambient light
            }
        }

        sensorManager.registerListener(
            listener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
