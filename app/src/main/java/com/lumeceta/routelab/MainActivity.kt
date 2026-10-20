package com.lumeceta.routelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00D9FF),
                    onPrimary = Color(0xFF00131B),
                    secondary = Color(0xFF8B6CFF),
                    onSecondary = Color.White,
                    background = Color(0xFF050A16),
                    onBackground = Color.White,
                    surface = Color(0xFF101827),
                    onSurface = Color.White
                )
            ) {
                RouteLabScreen()
            }
        }
    }
}
