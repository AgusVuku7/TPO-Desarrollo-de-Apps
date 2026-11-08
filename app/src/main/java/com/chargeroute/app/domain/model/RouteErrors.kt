package com.chargeroute.app.domain.model

// Errores al calcular una ruta nueva. El mensaje es el que ve el usuario (RNF06).

class NoConnectionException :
    Exception("Sin conexión: no se puede calcular una ruta nueva.")

class RouteServiceException :
    Exception("No se pudo consultar el servidor de rutas.")
