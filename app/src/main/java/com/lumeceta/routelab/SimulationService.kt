package com.lumeceta.routelab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class SimulationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var route: List<Waypoint> = emptyList()
    private var speedMetersPerSecond = 1.4
    private var progressMeters = 0.0
    private var lastTickNanos = 0L
    private var paused = false
    private val activeProviders = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSimulation()
            ACTION_PAUSE -> paused = true
            ACTION_RESUME -> { paused = false; lastTickNanos = System.nanoTime() }
            ACTION_START -> {
                val lats = intent.getDoubleArrayExtra(EXTRA_LATS)
                val lons = intent.getDoubleArrayExtra(EXTRA_LONS)
                route = if (lats != null && lons != null && lats.size == lons.size) {
                    runCatching { lats.indices.map { Waypoint(lats[it], lons[it]) } }.getOrDefault(emptyList())
                } else emptyList()
                speedMetersPerSecond = intent.getDoubleExtra(EXTRA_SPEED, 1.4).coerceIn(0.28, 33.33)
                progressMeters = 0.0
                if (route.size >= 2) startSimulation()
            }
        }
        return START_NOT_STICKY
    }

    private fun startSimulation() {
        stopLoop()
        startForeground(NOTIFICATION_ID, notification("Simulation startet"))
        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            activeProviders.clear()
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                runCatching {
                    manager.addTestProvider(provider, false, false, false, false, true, true, true, android.location.Criteria.POWER_MEDIUM, android.location.Criteria.ACCURACY_FINE)
                    manager.setTestProviderEnabled(provider, true)
                    activeProviders += provider
                }
            }
            if (activeProviders.isEmpty()) {
                updateNotification("Keine Testprovider verfügbar")
                stopSelf()
                return
            }
            lastTickNanos = System.nanoTime()
            paused = false
            job = scope.launch {
                val total = RouteMath.totalDistanceMeters(route)
                while (isActive && progressMeters < total) {
                    val now = System.nanoTime()
                    if (!paused) {
                        val delta = ((now - lastTickNanos) / 1_000_000_000.0).coerceIn(0.0, 2.0)
                        progressMeters = (progressMeters + delta * speedMetersPerSecond).coerceAtMost(total)
                        val point = RouteMath.pointAtDistance(route, progressMeters)
                        val location = Location(LocationManager.GPS_PROVIDER).apply {
                            latitude = point.waypoint.latitude
                            longitude = point.waypoint.longitude
                            accuracy = 3f
                            altitude = 0.0
                            speed = speedMetersPerSecond.toFloat()
                            bearing = point.bearingDegrees
                            time = System.currentTimeMillis()
                            elapsedRealtimeNanos = System.nanoTime()
                        }
                        activeProviders.toList().forEach { provider ->
                            runCatching {
                                val copy = Location(location).apply { this.provider = provider }
                                (getSystemService(LOCATION_SERVICE) as LocationManager).setTestProviderLocation(provider, copy)
                            }
                        }
                        updateNotification("Simulation: %.2f / %.2f km".format(progressMeters / 1000.0, total / 1000.0))
                    }
                    lastTickNanos = now
                    delay(1000L)
                }
                if (isActive && progressMeters >= total) {
                    updateNotification("Ziel erreicht")
                    delay(1500L)
                    stopSimulation()
                }
            }
        } catch (e: Exception) {
            updateNotification("Fehler bei Standortsimulation")
            stopSimulation()
        }
    }

    private fun stopLoop() { job?.cancel(); job = null }

    private fun stopSimulation() {
        stopLoop()
        runCatching {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            activeProviders.toList().forEach { provider ->
                runCatching { manager.setTestProviderEnabled(provider, false) }
                runCatching { manager.removeTestProvider(provider) }
            }
        }
        activeProviders.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "RouteLab Standortsimulation", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(status: String): Notification {
        val stopIntent = PendingIntent.getService(this, 1, Intent(this, SimulationService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RouteLab GPS Simulator")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(0, "STOPP", stopIntent)
            .build()
    }

    private fun updateNotification(status: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(status))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLoop()
        scope.cancel()
        runCatching {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            activeProviders.toList().forEach { provider ->
                runCatching { manager.setTestProviderEnabled(provider, false) }
                runCatching { manager.removeTestProvider(provider) }
            }
        }
        activeProviders.clear()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.lumeceta.routelab.START"
        const val ACTION_PAUSE = "com.lumeceta.routelab.PAUSE"
        const val ACTION_RESUME = "com.lumeceta.routelab.RESUME"
        const val ACTION_STOP = "com.lumeceta.routelab.STOP"
        const val EXTRA_LATS = "route_lats"
        const val EXTRA_LONS = "route_lons"
        const val EXTRA_SPEED = "speed"
        private const val CHANNEL_ID = "routelab_simulation"
        private const val NOTIFICATION_ID = 4041
    }
}