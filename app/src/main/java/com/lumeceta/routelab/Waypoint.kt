package com.lumeceta.routelab

import kotlin.math.*

data class Waypoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

data class RoutePosition(
    val waypoint: Waypoint,
    val bearingDegrees: Float,
    val distanceMeters: Double
)

object RouteMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(a: Waypoint, b: Waypoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val hav = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(hav.coerceIn(0.0, 1.0)))
    }

    fun bearingDegrees(a: Waypoint, b: Waypoint): Float {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    fun interpolate(a: Waypoint, b: Waypoint, fraction: Double): Waypoint {
        val f = fraction.coerceIn(0.0, 1.0)
        return Waypoint(
            latitude = a.latitude + (b.latitude - a.latitude) * f,
            longitude = a.longitude + (b.longitude - a.longitude) * f
        )
    }

    fun pointAtDistance(route: List<Waypoint>, distanceMeters: Double): RoutePosition {
        require(route.isNotEmpty()) { "Route must contain at least one waypoint" }
        if (route.size == 1) return RoutePosition(route.first(), 0f, 0.0)

        var remaining = distanceMeters.coerceAtLeast(0.0)
        var travelled = 0.0
        for (i in 0 until route.lastIndex) {
            val a = route[i]
            val b = route[i + 1]
            val segment = distanceMeters(a, b)
            if (segment == 0.0) continue
            if (remaining <= segment) {
                val fraction = remaining / segment
                return RoutePosition(interpolate(a, b, fraction), bearingDegrees(a, b), travelled + remaining)
            }
            remaining -= segment
            travelled += segment
        }
        val a = route[route.lastIndex - 1]
        val b = route.last()
        return RoutePosition(b, bearingDegrees(a, b), travelled)
    }

    fun totalDistanceMeters(route: List<Waypoint>): Double =
        route.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) }
}
