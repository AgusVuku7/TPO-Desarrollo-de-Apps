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
 * Lee el sensor de luz ambiente (TYPE_LIGHT) para cambiar solo entre modo claro y oscuro.
 * Cubre RNF04 (adaptacion a la luz) y RNF05 (uso responsable de sensores).
 */
class AmbientLightSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    /**
     * Flow con las lecturas de luz en lux.
     * Registra el listener al empezar a escucharlo y lo libera cuando se deja de escuchar.
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
                // No hace falta para la luz ambiente
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
