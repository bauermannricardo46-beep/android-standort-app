package com.lumeceta.routelab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun RouteLabScreen() {
    val context = LocalContext.current
    var startLat by remember { mutableStateOf("52.5200") }
    var startLon by remember { mutableStateOf("13.4050") }
    var endLat by remember { mutableStateOf("52.5300") }
    var endLon by remember { mutableStateOf("13.4200") }
    var speedKmh by remember { mutableFloatStateOf(5f) }
    var status by remember { mutableStateOf("Bereit für Standorttests") }

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        status = if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) "Standortberechtigung erteilt"
        else "Standortberechtigung nicht erteilt"
    }

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun serviceAction(action: String) {
        ContextCompat.startForegroundService(context, Intent(context, SimulationService::class.java).setAction(action))
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("RouteLab", style = MaterialTheme.typography.headlineLarge)
            Text("GPS Simulator · Android Mock Location", style = MaterialTheme.typography.titleMedium)
            Text("Für Entwicklung und legitime Standorttests. Simulierte Positionen können von Android oder Apps erkannt werden.")
            HorizontalDivider()
            Text("Startpunkt", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(startLat, { startLat = it }, label = { Text("Breitengrad") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(startLon, { startLon = it }, label = { Text("Längengrad") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Text("Zielpunkt", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(endLat, { endLat = it }, label = { Text("Breitengrad") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(endLon, { endLon = it }, label = { Text("Längengrad") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Text("Geschwindigkeit: ${speedKmh.toInt()} km/h")
            Slider(speedKmh, { speedKmh = it }, valueRange = 1f..120f)

            OutlinedButton(
                onClick = {
                    if (!hasPermission()) permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    else {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        status = "Entwickleroptionen öffnen und RouteLab als App für simulierte Standorte auswählen"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Mock-Standort einrichten") }

            Button(
                onClick = {
                    val aLat = startLat.toDoubleOrNull()
                    val aLon = startLon.toDoubleOrNull()
                    val bLat = endLat.toDoubleOrNull()
                    val bLon = endLon.toDoubleOrNull()
                    if (listOf(aLat, aLon, bLat, bLon).any { it == null }) status = "Bitte gültige Koordinaten eingeben"
                    else if (!hasPermission()) permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    else {
                        runCatching { listOf(Waypoint(aLat!!, aLon!!), Waypoint(bLat!!, bLon!!)) }
                            .onSuccess {
                                val intent = Intent(context, SimulationService::class.java).apply {
                                    action = SimulationService.ACTION_START
                                    putExtra(SimulationService.EXTRA_LATS, doubleArrayOf(aLat!!, bLat!!))
                                    putExtra(SimulationService.EXTRA_LONS, doubleArrayOf(aLon!!, bLon!!))
                                    putExtra(SimulationService.EXTRA_SPEED, speedKmh / 3.6)
                                }
                                ContextCompat.startForegroundService(context, intent)
                                status = "Simulation gestartet"
                            }
                            .onFailure { status = it.message ?: "Ungültige Koordinaten" }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("START") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ serviceAction(SimulationService.ACTION_PAUSE) }, Modifier.weight(1f)) { Text("Pause") }
                OutlinedButton({ serviceAction(SimulationService.ACTION_RESUME) }, Modifier.weight(1f)) { Text("Fortsetzen") }
                OutlinedButton({ serviceAction(SimulationService.ACTION_STOP) }, Modifier.weight(1f)) { Text("Stopp") }
            }
            HorizontalDivider()
            Text("Status", style = MaterialTheme.typography.titleMedium)
            Text(status)
            Text("Die App sendet simulierte Koordinaten über Android-Testprovider; sie verändert keine Satellitensignale und kann keine Annahme durch andere Apps garantieren.")
        }
    }
}
