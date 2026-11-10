package com.chargeroute.app.domain.model

/**
 * Ciudades conocidas con coordenadas exactas, para autocompletar sin consultar la API.
 */
object KnownCities {

    val OBELISCO = LocationPoint("Buenos Aires (Obelisco)", "Av. 9 de Julio y Corrientes, CABA", -34.6037, -58.3816)

    // Destino que aparece cargado al abrir la pantalla de ruta
    val DEFAULT_DESTINATION = LocationPoint(
        "Mar del Plata (Plaza Colón)", "Av. Patricio Peralta Ramos 2200, Mar del Plata", -38.0055, -57.5432
    )

    val all = listOf(
        OBELISCO,
        LocationPoint("Mar del Plata", "Av. Patricio Peralta Ramos 2200, Mar del Plata", -38.0055, -57.5432),
        LocationPoint("Dolores", "Autovía 2 km 210, Dolores, Buenos Aires", -36.3132, -57.6792),
        LocationPoint("Chascomús", "Costanera España y Av. Lastra, Chascomús", -35.5786, -58.0125),
        LocationPoint("Pinamar", "Av. Bunge y Av. Shaw, Pinamar", -37.1084, -56.8587),
        LocationPoint("Villa Gesell", "Av. Buenos Aires y Av. 3, Villa Gesell", -37.2642, -56.9731),
        LocationPoint("Tandil", "Plaza Independencia, Tandil", -37.3217, -59.1332),
        LocationPoint("La Plata", "Plaza Moreno, Calle 12 y 51, La Plata", -34.9214, -57.9545),
        LocationPoint("Rosario", "Monumento a la Bandera, Rosario, Santa Fe", -32.9468, -60.6393),
        LocationPoint("Córdoba Capital", "Plaza San Martín, Córdoba", -31.4201, -64.1888)
    )

    // Primera ciudad conocida cuyo nombre contiene lo escrito (o null)
    fun findByName(text: String): LocationPoint? =
        all.firstOrNull { it.name.contains(text, ignoreCase = true) }
}
