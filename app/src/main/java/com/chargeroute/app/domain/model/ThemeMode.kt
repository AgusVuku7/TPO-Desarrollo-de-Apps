package com.chargeroute.app.domain.model

// Son las tres opciones del menu. Si no hay nada guardado se usa LIGHT.
enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO_SENSOR // Usa el sensor de luz ambiente (TYPE_LIGHT) (RNF04, RNF05)
}
