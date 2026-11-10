package com.chargeroute.app.domain.model

/**
 * Niveles de bateria que se usan en varias pantallas.
 */
object BatteryLevels {
    // Por debajo de este porcentaje se avisa que la bateria esta baja (RF03)
    const val LOW_PERCENT = 20

    // Bateria con la que arranca el formulario si el usuario no la cambia
    const val DEFAULT_INITIAL_PERCENT = 70
}
