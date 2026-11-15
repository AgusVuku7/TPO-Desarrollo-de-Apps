package com.chargeroute.app.domain.model

object Consumption {
    // Con el aire acondicionado prendido el auto gasta un 10 % mas
    const val AC_MULTIPLIER = 1.10

    fun effectiveKwhPer100Km(baseKwhPer100Km: Double, acActive: Boolean): Double {
        return if (acActive) baseKwhPer100Km * AC_MULTIPLIER else baseKwhPer100Km
    }
}
