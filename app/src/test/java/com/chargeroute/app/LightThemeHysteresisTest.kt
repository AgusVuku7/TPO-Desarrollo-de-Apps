package com.chargeroute.app

import com.chargeroute.app.core.sensor.LightThemeHysteresis
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LightThemeHysteresisTest {

    @Test
    fun poca_luz_prende_el_modo_oscuro() {
        assertTrue(LightThemeHysteresis.isDark(currentlyDark = false, lux = 10f))
    }

    @Test
    fun mucha_luz_apaga_el_modo_oscuro() {
        assertFalse(LightThemeHysteresis.isDark(currentlyDark = true, lux = 200f))
    }

    @Test
    fun en_la_zona_del_medio_mantiene_el_estado() {
        assertTrue(LightThemeHysteresis.isDark(currentlyDark = true, lux = 45f))
        assertFalse(LightThemeHysteresis.isDark(currentlyDark = false, lux = 45f))
    }
}
