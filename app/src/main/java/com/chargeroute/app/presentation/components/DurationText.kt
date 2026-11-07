package com.chargeroute.app.presentation.components

// Convierte minutos en un texto corto: "45 min", "3 h" o "11 h 05"
fun formatDuration(totalMinutes: Int): String {
    val minutes = totalMinutes.coerceAtLeast(0)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h ${rest.toString().padStart(2, '0')}"
    }
}
