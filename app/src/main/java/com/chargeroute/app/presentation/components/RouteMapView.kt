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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    activeCarProgress: Float? = null, // 0.0f to 1.0f along route
    onStationClick: ((Station) -> Unit)? = null
) {
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
            // Center roughly around Buenos Aires province
            controller.setCenter(GeoPoint(-36.0, -58.0))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
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

                    // 1. Draw Route Polyline
                    val polyline = Polyline(map).apply {
                        setPoints(geoPoints)
                        outlinePaint.color = android.graphics.Color.parseColor("#00C88C")
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    map.overlays.add(polyline)

                    // 2. Origin Marker (punto verde)
                    val originGeo = GeoPoint(routePlan.origin.latitude, routePlan.origin.longitude)
                    val originMarker = Marker(map).apply {
                        position = originGeo
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_map_origin)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Origen: ${routePlan.origin.name}"
                        snippet = routePlan.origin.address
                    }
                    map.overlays.add(originMarker)

                    // 3. Destination Marker (banderita)
                    val destGeo = GeoPoint(routePlan.destination.latitude, routePlan.destination.longitude)
                    val destMarker = Marker(map).apply {
                        position = destGeo
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_map_flag)
                        setAnchor(0.22f, 0.94f) // la base del mastil queda justo en el destino
                        title = "Destino: ${routePlan.destination.name}"
                        snippet = routePlan.destination.address
                    }
                    map.overlays.add(destMarker)

                    // 4. Charging Station Markers (Dolores, etc.)
                    routePlan.stops.forEach { stop ->
                        val stationGeo = GeoPoint(stop.station.latitude, stop.station.longitude)
                        val stationMarker = Marker(map).apply {
                            position = stationGeo
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_bolt) // rayo = aca se carga
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = stop.station.name
                            val kw = stop.station.connectors.firstOrNull()?.powerKw?.toInt() ?: 120
                            snippet = "${stop.station.operator} • DC $kw kW • Parada ${stop.stopDurationMinutes} min"
                            setOnMarkerClickListener { _, _ ->
                                onStationClick?.invoke(stop.station)
                                true
                            }
                        }
                        map.overlays.add(stationMarker)
                    }

                    // 5. Active Car Marker along the polyline
                    if (activeCarProgress != null) {
                        val carMarker = Marker(map).apply {
                            position = originGeo
                            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_nav)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Tu Vehículo en Ruta"
                        }
                        map.overlays.add(carMarker)
                        carMarkerHolder[0] = carMarker

                        // Zoom de navegacion: se ven las calles y que el auto va por la ruta
                        map.controller.setZoom(16.0)
                    } else {
                        // Fit bounding box showing origin, destination, and all stops
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
                    carMarker.snippet = "Progreso: ${(progressClamped * 100).toInt()}%"

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
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
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
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centrar Ruta")
            }
        }
    }
}
