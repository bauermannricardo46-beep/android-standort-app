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
                    secondary = Color(0xFF63F2D2),
                    background = Color(0xFF070A10),
                    surface = Color(0xFF111722)
                )
            ) { RouteLabScreen() }
        }
    }
}
