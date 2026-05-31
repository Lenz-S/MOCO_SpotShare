package com.example.moco.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.moco.viewmodel.ParkingViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.Marker
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@Composable
fun MapScreen(viewModel: ParkingViewModel) {
    val mapViewportState = rememberMapViewportState()
    val spots by viewModel.parkingSpots.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
        ) {
            spots.forEach { spot ->
                Marker(
                    point = Point.fromLngLat(spot.longitude, spot.latitude),
                )
            }
        }
        // Hier kommen später Suchfeld und Buttons drüber
    }
}




