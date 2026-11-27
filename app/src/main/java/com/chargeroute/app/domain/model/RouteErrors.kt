package com.chargeroute.app.domain.model

// Errores al calcular una ruta nueva. No llevan texto: la pantalla elige el mensaje (RNF06).

class NoConnectionException : Exception()

class RouteServiceException : Exception()

// Con las estaciones encontradas sobre la ruta no se llega a destino: no se arma un viaje que no se puede hacer
class UnreachableDestinationException : Exception()
