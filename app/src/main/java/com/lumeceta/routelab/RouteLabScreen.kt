package com.lumeceta.routelab

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val Night = Color(0xFF050A16)
private val Panel = Color(0xFF101827)
private val Cyan = Color(0xFF00D9FF)
private val Violet = Color(0xFF8B6CFF)
private val Muted = Color(0xFF91A4BD)
private val Mint = Color(0xFF63F2D2)

private enum class PointSelection { START, DESTINATION }

@Composable
fun RouteLabScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    var startPoint by remember { mutableStateOf(GeoPoint(52.5200, 13.4050)) }
    var endPoint by remember { mutableStateOf(GeoPoint(52.5300, 13.4200)) }
    var selection by remember { mutableStateOf(PointSelection.START) }
    var speedKmh by remember { mutableFloatStateOf(24f) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Bereit für die Routenvorschau") }

    val onMapTap = rememberUpdatedState(newValue = { point: GeoPoint ->
        if (selection == PointSelection.START) {
            startPoint = point
            status = "Startpunkt auf der Karte gesetzt"
        } else {
            endPoint = point
            status = "Zielpunkt auf der Karte gesetzt"
        }
        progress = 0f
        isPlaying = false
    })

    val mapView = remember(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.5)
            controller.setCenter(GeoPoint(52.5250, 13.4120))
            overlays.add(RouteTapOverlay { point -> onMapTap.value(point) })
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(startPoint, endPoint) {
        delay(250)
        runCatching {
            mapView.zoomToBoundingBox(
                BoundingBox.fromGeoPoints(listOf(startPoint, endPoint)),
                true,
                100
            )
        }
    }

    LaunchedEffect(isPlaying, startPoint, endPoint, speedKmh) {
        if (isPlaying) {
            if (progress >= 1f) progress = 0f
            val initialProgress = progress
            // Keep playback duration consistent with the selected km/h speed.
            // This is an in-app straight-line preview, not a system location feed.
            val durationMs = (
                distanceKm(startPoint, endPoint) / speedKmh.toDouble().coerceAtLeast(1.0) * 3_600_000.0
            ).coerceAtLeast(1.0).toLong()
            val beganAt = SystemClock.elapsedRealtime()
            while (isPlaying && progress < 1f) {
                val portion = ((SystemClock.elapsedRealtime() - beganAt).toDouble() / durationMs)
                    .toFloat().coerceIn(0f, 1f)
                progress = initialProgress + (1f - initialProgress) * portion
                if (progress >= 1f) {
                    isPlaying = false
                    status = "Vorschau abgeschlossen · Ziel erreicht"
                    break
                }
                delay(80)
            }
        }
    }

    val progressDouble = progress.toDouble()
    val movingPoint = GeoPoint(
        startPoint.latitude + (endPoint.latitude - startPoint.latitude) * progressDouble,
        startPoint.longitude + (endPoint.longitude - startPoint.longitude) * progressDouble
    )
    val distance = distanceKm(startPoint, endPoint)

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF10263B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, tint = Cyan, modifier = Modifier.size(31.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("NaviFlux", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("ROUTE STUDIO  ·  VERSION 2.0", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp)
                }
                Surface(shape = CircleShape, color = Color(0xFF102D30)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(Mint, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE MAP", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ROUTENKARTE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                            Text("Antippen, um Start oder Ziel festzulegen", color = Muted, fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFF202A3B)) {
                            Text("OSM", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                    }
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        update = { map ->
                            map.overlays.removeAll { it is Polyline || it is Marker }

                            val routeLine = Polyline(map).apply {
                                setPoints(listOf(startPoint, endPoint))
                                outlinePaint.color = android.graphics.Color.parseColor("#00D9FF")
                                outlinePaint.strokeWidth = 7f
                                outlinePaint.isAntiAlias = true
                            }
                            map.overlays.add(routeLine)

                            map.overlays.add(Marker(map).apply {
                                position = startPoint
                                title = "Startpunkt"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            })
                            map.overlays.add(Marker(map).apply {
                                position = endPoint
                                title = "Zielpunkt"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            })
                            map.overlays.add(Marker(map).apply {
                                position = movingPoint
                                title = "Vorschau-Marker"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            })
                            map.invalidate()
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("© OpenStreetMap-Mitwirkende", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text("LUFTLINIE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ROUTE EINRICHTEN", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selection == PointSelection.START) {
                        Button(onClick = { selection = PointSelection.START }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start wählen", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        OutlinedButton(onClick = { selection = PointSelection.START }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start wählen", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (selection == PointSelection.DESTINATION) {
                        Button(onClick = { selection = PointSelection.DESTINATION }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ziel wählen", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        OutlinedButton(onClick = { selection = PointSelection.DESTINATION }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ziel wählen", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoordinateCard(title = "START", point = startPoint, accent = Cyan, modifier = Modifier.weight(1f))
                    CoordinateCard(title = "ZIEL", point = endPoint, accent = Violet, modifier = Modifier.weight(1f))
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = Cyan, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ANIMIERTE VORSCHAU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vorschaugeschwindigkeit", color = Muted, fontSize = 12.sp)
                            Text("${speedKmh.toInt()} km/h", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(String.format(java.util.Locale.US, "%.2f km", distance), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            val etaSeconds = (distance / speedKmh.toDouble().coerceAtLeast(1.0) * 3600.0).toLong()
                            val etaMinutes = etaSeconds / 60
                            val etaRemainderSeconds = etaSeconds % 60
                            Text(
                                "Dauer %.2d:%02d".format(etaMinutes, etaRemainderSeconds),
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Slider(
                        value = speedKmh,
                        onValueChange = { speedKmh = it },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan,
                            activeTrackColor = Cyan,
                            inactiveTrackColor = Color(0xFF344154)
                        )
                    )
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = Violet,
                        trackColor = Color(0xFF273247)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    isPlaying = false
                                    status = "Vorschau pausiert"
                                } else {
                                    if (progress >= 1f) progress = 0f
                                    isPlaying = true
                                    status = "Marker bewegt sich in der Kartenansicht"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPlaying) "Pause" else "Route abspielen")
                        }
                        OutlinedButton(
                            onClick = {
                                isPlaying = false
                                progress = 0f
                                status = "Vorschau zurückgesetzt"
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Reset")
                        }
                    }
                    Text(status, color = Mint, fontSize = 12.sp)
                }
            }

            Text(
                "NaviFlux zeigt eine Routenvorschau direkt auf der Karte. Der Marker bewegt sich nur innerhalb dieser App; es werden keine simulierten Standortdaten an andere Apps gesendet.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CoordinateCard(title: String, point: GeoPoint, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1422))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.3.sp)
            Text(String.format(java.util.Locale.US, "%.5f", point.latitude), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(String.format(java.util.Locale.US, "%.5f", point.longitude), color = Muted, fontSize = 12.sp)
        }
    }
}

private class RouteTapOverlay(private val onTap: (GeoPoint) -> Unit) : Overlay() {
    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val tapped = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
        onTap(GeoPoint(tapped.latitude, tapped.longitude))
        return true
    }
}

private fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
    val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val latitude1 = Math.toRadians(from.latitude)
    val latitude2 = Math.toRadians(to.latitude)
    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(latitude1) * cos(latitude2) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return 6_371.0 * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceAtLeast(0.0)))
}
