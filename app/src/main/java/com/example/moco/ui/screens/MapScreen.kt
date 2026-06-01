package com.example.moco.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@Composable
fun MapScreen() {
    val mapViewportState = rememberMapViewportState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
        )
        // Hier kommen später Suchfeld und Buttons drüber
    }
}
