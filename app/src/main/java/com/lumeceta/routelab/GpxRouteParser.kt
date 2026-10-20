package com.lumeceta.routelab

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object GpxRouteParser {
    fun parse(input: InputStream): List<Waypoint> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(input, "UTF-8")
        val points = mutableListOf<Waypoint>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "trkpt" || event == XmlPullParser.START_TAG && parser.name == "rtept" || event == XmlPullParser.START_TAG && parser.name == "wpt") {
                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                    ?: throw IllegalArgumentException("GPX point missing valid latitude")
                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                    ?: throw IllegalArgumentException("GPX point missing valid longitude")
                points += Waypoint(lat, lon)
            }
            event = parser.next()
        }
        require(points.isNotEmpty()) { "No route points found in GPX file" }
        return points
    }
}
