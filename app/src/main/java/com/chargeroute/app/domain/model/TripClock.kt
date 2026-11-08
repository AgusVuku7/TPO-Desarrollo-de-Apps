package com.chargeroute.app.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Horarios del viaje: hora de salida (cuando se calculo la ruta) + minutos transcurridos
object TripClock {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    // Ej: at(salida, 90) devuelve la hora 1 h 30 despues de la salida, como "14:05"
    fun at(startMillis: Long, plusMinutes: Int, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = Instant.ofEpochMilli(startMillis).plusSeconds(plusMinutes * 60L).atZone(zone)
        return formatter.format(time)
    }
}
