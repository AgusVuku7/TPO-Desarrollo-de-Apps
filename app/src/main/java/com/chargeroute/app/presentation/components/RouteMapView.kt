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
import com.chargeroute.app.core.theme.BrandPrimary
import com.chargeroute.app.domain.model.RoutePlan
import com.chargeroute.app.domain.model.Station
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

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
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

                    // 2. Origin Marker (Green Pin)
                    val originGeo = GeoPoint(routePlan.origin.latitude, routePlan.origin.longitude)
                    val originMarker = Marker(map).apply {
                        position = originGeo
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Origen: ${routePlan.origin.name}"
                        snippet = routePlan.origin.address
                    }
                    map.overlays.add(originMarker)

                    // 3. Destination Marker (Red Pin)
                    val destGeo = GeoPoint(routePlan.destination.latitude, routePlan.destination.longitude)
                    val destMarker = Marker(map).apply {
                        position = destGeo
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Destino: ${routePlan.destination.name}"
                        snippet = routePlan.destination.address
                    }
                    map.overlays.add(destMarker)

                    // 4. Charging Station Markers (Dolores, etc.)
                    routePlan.stops.forEach { stop ->
                        val stationGeo = GeoPoint(stop.station.latitude, stop.station.longitude)
                        val stationMarker = Marker(map).apply {
                            position = stationGeo
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
                        val progressClamped = activeCarProgress.coerceIn(0f, 1f)
                        val carPoint = interpolatePolyline(geoPoints, progressClamped)
                        val carMarker = Marker(map).apply {
                            position = carPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Tu Vehículo en Ruta"
                            snippet = "Progreso: ${(progressClamped * 100).toInt()}%"
                        }
                        map.overlays.add(carMarker)

                        // Center map on moving vehicle in active navigation mode
                        map.controller.animateTo(carPoint)
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

                map.invalidate()
            }
        )

        // Overlay Map Controls: Zoom in / Zoom out / Recenter
        Column(
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

/**
 * Interpola un GeoPoint a lo largo de los puntos de la polilínea según un porcentaje (0.0 a 1.0)
 */
private fun interpolatePolyline(points: List<GeoPoint>, progress: Float): GeoPoint {
    if (points.isEmpty()) return GeoPoint(-34.6037, -58.3816)
    if (points.size == 1 || progress <= 0f) return points.first()
    if (progress >= 1f) return points.last()

    val totalSegments = points.size - 1
    val scaledProgress = progress * totalSegments
    val index = scaledProgress.toInt().coerceIn(0, totalSegments - 1)
    val fraction = scaledProgress - index

    val p1 = points[index]
    val p2 = points[index + 1]

    val lat = p1.latitude + (p2.latitude - p1.latitude) * fraction
    val lon = p1.longitude + (p2.longitude - p1.longitude) * fraction

    return GeoPoint(lat, lon)
}
