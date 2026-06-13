package com.example.moco.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moco.data.FirebaseHelper
import com.example.moco.model.ParkingSpot
import com.mapbox.geojson.Point
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationAnchorConfig
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Haupt-Kartenbildschirm der App.
 * Nutzt Mapbox für die Darstellung und Firebase Firestore für die Daten.
 */
@Composable
fun MapScreen(
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val mapViewportState = rememberMapViewportState()
    val scope = rememberCoroutineScope()
    val firebaseHelper = remember { FirebaseHelper() }
    
    // Wir nutzen eine Liste als State. Wenn sich diese ändert, triggert das den MapEffect.
    var parkingSpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }

    // Berechtigungs-Management (Standort)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            mapViewportState.transitionToFollowPuckState()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            scaleBar = {},
        ) {
            // Standort-Konfiguration
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                }
                
                mapViewportState.transitionToFollowPuckState(
                    followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                        .zoom(12.0)
                        .build()
                )
            }

            // NEU: Stabile Darstellung der Parkplätze via ViewAnnotation
            // Wir nutzen Compose-Elemente direkt als Marker
            parkingSpots.forEach { spot ->
                val point = Point.fromLngLat(spot.longitude, spot.latitude)
                val pinColor = if (spot.isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                
                key(spot.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(point)
                            allowOverlap(true)
                            allowOverlapWithPuck(true)
                            visible(true)
                            variableAnchors(listOf(
                                ViewAnnotationAnchorConfig.Builder()
                                    .anchor(ViewAnnotationAnchor.BOTTOM)
                                    .build()
                            ))
                        }
                    ) {
                        // Ein Container für den Pin, um das Rendering zu stabilisieren
                        Box(modifier = Modifier.wrapContentSize()) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Pin",
                                tint = pinColor,
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(2.dp, shape = RoundedCornerShape(21.dp))
                            )
                        }
                    }
                }
            }
        }

        // --- UI-Elemente über der Karte ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Such-Leiste
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp))
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Suche", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Adresse suchen...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Such-Button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Daten vom Server abrufen
                            val spots = firebaseHelper.getAllSpotsOnce()
                            // Den State aktualisieren - erzwungenes Neuzeichnen durch kurzen Reset
                            parkingSpots = emptyList() 
                            delay(50) // Kurze Pause, damit Compose den leeren Zustand bemerkt
                            parkingSpots = spots
                            Toast.makeText(context, "${spots.size} Parkplätze geladen", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Fehler: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(text = "In diesem Bereich suchen", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Standort-Button
        FloatingActionButton(
            onClick = { mapViewportState.transitionToFollowPuckState() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Fokus")
        }
    }
}
