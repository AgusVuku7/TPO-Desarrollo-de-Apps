package com.chargeroute.app.presentation.components

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chargeroute.app.R
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.domain.model.RouteGeometry
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RouteMapView(
    routePlan: RoutePlan?,
    modifier: Modifier = Modifier,
    activeCarProgress: Float? = null, // de 0.0f a 1.0f a lo largo de la ruta
    onStationClick: ((Station) -> Unit)? = null
) {
    // En el preview de Android Studio no se puede crear el MapView de osmdroid
    if (LocalInspectionMode.current) {
        Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.component_map_preview_placeholder))
        }
        return
    }

    val context = LocalContext.current

    // Para no redibujar todo en cada movimiento del auto
    val drawnPlan = remember { arrayOfNulls<RoutePlan>(1) }
    val carMarkerHolder = remember { arrayOfNulls<Marker>(1) }

    val mapView = remember {
        // La configuracion tiene que estar lista antes de crear el mapa (si no, no bajan bien los tiles)
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().osmdroidBasePath = File(context.cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")

        MapView(context).apply {
            // Para poder mover el mapa con el dedo aunque este dentro de una pantalla con scroll
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(7.5)
            // Centra el mapa mas o menos en la provincia de Buenos Aires
            controller.setCenter(GeoPoint(-36.0, -58.0))
        }
    }

    // El mapa solo trabaja (tiles, redibujo) mientras la pantalla esta en primer plano
    LifecycleResumeEffect(mapView) {
        mapView.onResume()
        onPauseOrDispose { mapView.onPause() }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                // Solo se redibuja si cambio la ruta. Si no, el mapa volvia al zoom inicial a cada rato.
                if (drawnPlan[0] != routePlan) {
                drawnPlan[0] = routePlan
                carMarkerHolder[0] = null
                map.overlays.clear()

                if (routePlan != null && routePlan.polylinePoints.isNotEmpty()) {
                    val geoPoints = routePlan.polylinePoints.map { GeoPoint(it.first, it.second) }

                    // 1. Linea de la ruta
                    val polyline = Polyline(map).apply {
                        setPoints(geoPoints)
                        outlinePaint.color = BrandPrimary.toArgb()
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    map.overlays.add(polyline)

                    // 2. Marcador de origen (punto verde)
                    val originGeo = GeoPoint(routePlan.origin.latitude, routePlan.origin.longitude)
                    val originMarker = Marker(map).apply {
                        position = originGeo
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_map_origin)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = context.getString(R.string.component_map_origin_title, routePlan.origin.name)
                        snippet = routePlan.origin.address
                    }
                    map.overlays.add(originMarker)

                    // 3. Marcador de destino (banderita)
                    val destGeo = GeoPoint(routePlan.destination.latitude, routePlan.destination.longitude)
                    val destMarker = Marker(map).apply {
                        position = destGeo
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_map_flag)
                        setAnchor(0.22f, 0.94f) // la base del mastil queda justo en el destino
                        title = context.getString(R.string.component_map_destination_title, routePlan.destination.name)
                        snippet = routePlan.destination.address
                    }
                    map.overlays.add(destMarker)

                    // 4. RF02: estaciones compatibles cerca de la ruta (mas transparentes que la parada).
                    // Se dibujan antes que las paradas para que la parada quede arriba.
                    val stopStationIds = routePlan.stops.map { it.station.id }.toSet()
                    routePlan.nearbyStations
                        .filter { it.id !in stopStationIds }
                        .forEach { station ->
                            val nearbyMarker = Marker(map).apply {
                                position = GeoPoint(station.latitude, station.longitude)
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_bolt)
                                alpha = 0.55f
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = station.name
                                snippet = stationSnippet(context, station)
                            }
                            map.overlays.add(nearbyMarker)
                        }

                    // 5. Paradas de carga planificadas
                    routePlan.stops.forEach { stop ->
                        val stationGeo = GeoPoint(stop.station.latitude, stop.station.longitude)
                        val stationMarker = Marker(map).apply {
                            position = stationGeo
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_bolt) // rayo = aca se carga
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = stop.station.name
                            snippet = context.getString(
                                R.string.component_map_stop_snippet,
                                stationSnippet(context, stop.station),
                                stop.stopDurationMinutes
                            )
                            setOnMarkerClickListener { _, _ ->
                                onStationClick?.invoke(stop.station)
                                true
                            }
                        }
                        map.overlays.add(stationMarker)
                    }

                    // 6. Marcador del auto sobre la ruta
                    if (activeCarProgress != null) {
                        val carMarker = Marker(map).apply {
                            position = originGeo
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_nav)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = context.getString(R.string.component_map_vehicle_title)
                        }
                        map.overlays.add(carMarker)
                        carMarkerHolder[0] = carMarker

                        // Zoom de navegacion: se ven las calles y que el auto va por la ruta
                        map.controller.setZoom(16.0)
                    } else {
                        // Ajusta el zoom para que se vean origen, destino y todas las paradas
                        try {
                            val allPoints = mutableListOf<GeoPoint>()
                            allPoints.add(originGeo)
                            allPoints.add(destGeo)
                            routePlan.stops.forEach { allPoints.add(GeoPoint(it.station.latitude, it.station.longitude)) }
                            val box = BoundingBox.fromGeoPoints(allPoints)
                            map.post {
                                map.zoomToBoundingBox(box, true, 110)
                            }
                        } catch (_: Exception) {
                            map.controller.setCenter(originGeo)
                        }
                    }
                }
                }

                // El auto se mueve siempre sobre la linea de la ruta
                val carMarker = carMarkerHolder[0]
                if (carMarker != null && routePlan != null && activeCarProgress != null) {
                    val progressClamped = activeCarProgress.coerceIn(0f, 1f)
                    val carPoint = RouteGeometry.pointAtProgress(routePlan.polylinePoints, progressClamped)
                    val carGeo = GeoPoint(carPoint.first, carPoint.second)
                    carMarker.position = carGeo
                    carMarker.snippet = context.getString(R.string.component_map_progress, (progressClamped * 100).toInt())

                    // Modo conductor: el mapa gira segun hacia donde va el auto y el auto queda abajo
                    map.mapOrientation = -RouteGeometry.bearingAtProgress(routePlan.polylinePoints, progressClamped)
                    map.setMapCenterOffset(0, map.height / 4)
                    map.controller.setCenter(carGeo)
                }

                map.invalidate()
            }
        )

        // Overlay Map Controls: Zoom in / Zoom out / Recenter (en modo conductor no se muestran)
        if (activeCarProgress == null) Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomIn() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.component_map_zoom_in))
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.component_map_zoom_out))
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmallFloatingActionButton(
                onClick = {
                    if (routePlan != null && routePlan.polylinePoints.isNotEmpty()) {
                        val points = routePlan.polylinePoints.map { GeoPoint(it.first, it.second) }
                        val box = BoundingBox.fromGeoPoints(points)
                        mapView.zoomToBoundingBox(box, true, 110)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandPrimary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = stringResource(R.string.component_map_center_route))
            }
        }
    }
}

// Texto corto del cartel de una estacion: operador, conectores y potencia maxima
private fun stationSnippet(context: Context, station: Station): String {
    val connectors = station.connectors.map { it.type.displayName }.distinct().joinToString(", ")
    val maxKw = station.connectors.maxOfOrNull { it.powerKw } ?: 0.0
    val power = if (maxKw > 0) context.getString(R.string.component_map_up_to_kw, maxKw.toInt()) else ""
    return "${station.operator} • $connectors$power"
}
