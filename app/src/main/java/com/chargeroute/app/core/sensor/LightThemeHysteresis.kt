package com.chargeroute.app.core.sensor

/**
 * Decide si el tema oscuro tiene que estar prendido segun la luz ambiente.
 * Usa dos umbrales distintos para que no parpadee cuando la luz anda cerca del limite.
 */
object LightThemeHysteresis {
    const val DARK_BELOW_LUX = 30f
    const val LIGHT_ABOVE_LUX = 60f

    fun isDark(currentlyDark: Boolean, lux: Float): Boolean = when {
        lux < DARK_BELOW_LUX -> true
        lux > LIGHT_ABOVE_LUX -> false
        else -> currentlyDark
    }
}
