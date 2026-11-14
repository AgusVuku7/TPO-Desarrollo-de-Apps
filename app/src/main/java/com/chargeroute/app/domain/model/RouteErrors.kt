package com.chargeroute.app.domain.model

// Errores al calcular una ruta nueva. No llevan texto: la pantalla elige el mensaje (RNF06).

class NoConnectionException : Exception()

class RouteServiceException : Exception()
