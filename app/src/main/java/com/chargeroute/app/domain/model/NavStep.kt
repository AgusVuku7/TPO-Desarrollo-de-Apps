package com.chargeroute.app.domain.model

// Una indicacion del viaje: "Girá a la derecha por Av. Rivadavia"
data class NavStep(
    val progress: Float, // en que parte de la ruta hay que hacer la maniobra
    val instruction: String,
    val maneuver: String, // left, right, straight, roundabout, arrive...
    val roadName: String
)
