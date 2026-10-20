package com.lumeceta.routelab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMathTest {
    @Test fun interpolationAtHalfway() {
        val a = Waypoint(10.0, 20.0)
        val b = Waypoint(12.0, 24.0)
        val p = RouteMath.interpolate(a, b, 0.5)
        assertEquals(11.0, p.latitude, 0.000001)
        assertEquals(22.0, p.longitude, 0.000001)
    }

    @Test fun distanceIsPositive() {
        val d = RouteMath.distanceMeters(Waypoint(52.52, 13.405), Waypoint(52.53, 13.42))
        assertTrue(d > 0)
    }

    @Test fun routeLengthSumsSegments() {
        val a = Waypoint(0.0, 0.0)
        val b = Waypoint(0.0, 0.01)
        val c = Waypoint(0.0, 0.02)
        val total = RouteMath.totalDistanceMeters(listOf(a, b, c))
        assertEquals(RouteMath.distanceMeters(a, b) + RouteMath.distanceMeters(b, c), total, 0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLatitudeRejected() {
        Waypoint(91.0, 0.0)
    }
}
