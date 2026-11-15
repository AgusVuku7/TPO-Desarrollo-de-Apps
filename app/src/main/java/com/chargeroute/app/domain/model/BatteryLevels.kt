package com.chargeroute.app.domain.model

/**
 * Niveles de bateria que se usan en varias pantallas.
 */
object BatteryLevels {
    // Por debajo de este porcentaje se avisa que la bateria esta baja (RF03)
    const val LOW_PERCENT = 20

    // Bateria con la que arranca el formulario si el usuario no la cambia
    const val DEFAULT_INITIAL_PERCENT = 70

    // Bateria minima que se puede cargar o simular
    const val MIN_PERCENT = 5

    // Bateria maxima
    const val MAX_PERCENT = 100

    // Cuanto suman o restan los botones - y +
    const val STEP_PERCENT = 5

    // Por encima de este porcentaje el indicador se muestra en verde
    const val GOOD_ABOVE_PERCENT = 40
}
