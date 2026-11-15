package com.chargeroute.app

// Codifica una lista de puntos como polilinea (formato Google/OSRM, 5 decimales).
// Solo se usa en los tests para armar datos; la app solo decodifica.
fun encodePolyline(points: List<Pair<Double, Double>>): String {
    val result = StringBuilder()
    var lastLat = 0
    var lastLng = 0
    for (point in points) {
        val lat = Math.round(point.first * 1E5).toInt()
        val lng = Math.round(point.second * 1E5).toInt()
        encodeValue(lat - lastLat, result)
        encodeValue(lng - lastLng, result)
        lastLat = lat
        lastLng = lng
    }
    return result.toString()
}

private fun encodeValue(value: Int, result: StringBuilder) {
    var v = if (value < 0) (value shl 1).inv() else (value shl 1)
    while (v >= 0x20) {
        result.append(((0x20 or (v and 0x1f)) + 63).toChar())
        v = v shr 5
    }
    result.append((v + 63).toChar())
}
