package com.example.moco.ui.screens

import android.Manifest
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
import androidx.compose.ui.unit.dp
import com.example.moco.data.FirebaseHelper
import com.example.moco.model.ParkingSpot
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import kotlinx.coroutines.launch

/**
 * Haupt-Kartenbildschirm der App.
 * Zuständig für Kartenanzeige, Standorterkennung und primäre UI-Interaktionen.
 * 
 * @param onSearchClick Navigation zum Suchbildschirm.
 */
@Composable
fun MapScreen(
    onSearchClick: () -> Unit
) {
    val mapViewportState = rememberMapViewportState()
    val scope = rememberCoroutineScope()
    val firebaseHelper = remember { FirebaseHelper() }
    
    // State für die geladenen Parkplätze
    var parkingSpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }

    // Dialog zur Abfrage der GPS-Berechtigungen (Fine & Coarse Location)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            mapViewportState.transitionToFollowPuckState()
        }
    }

    // Triggert die Standortabfrage beim ersten Start des Screens
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
            scaleBar = {}, // Deaktiviert die Anzeige des Maßstabs (ScaleBar)
        ) {
            // MapEffect für den Zugriff auf tieferliegende Mapbox-Konfigurationen
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                }
                
                // Kamera auf den Standort ausrichten (Zoom 12.0 für gute Umgebungsübersicht)
                mapViewportState.transitionToFollowPuckState(
                    followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                        .zoom(12.0)
                        .build()
                )
            }

            // Parkplätze als Pins (rote Kreise) auf der Karte anzeigen
            // MapEffect reagiert auf Änderungen in parkingSpots
            MapEffect(parkingSpots) { mapView ->
                val annotationApi = mapView.annotations
                val circleAnnotationManager = annotationApi.createCircleAnnotationManager()
                
                // Bestehende Pins löschen, bevor neue gezeichnet werden
                circleAnnotationManager.deleteAll()
                
                parkingSpots.forEach { spot ->
                    val circleAnnotationOptions = CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(spot.longitude, spot.latitude))
                        .withCircleRadius(10.0)
                        .withCircleColor("#E91E63") // Ein schönes Pink/Rot
                        .withCircleStrokeWidth(2.0)
                        .withCircleStrokeColor("#FFFFFF")
                    
                    circleAnnotationManager.create(circleAnnotationOptions)
                }
            }
        }

        // --- UI Overlays ---

        // Suchbereich (Suchfeld + "In diesem Bereich suchen" Button)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Schwebendes Suchfeld (fungiert als Button zum SearchScreen)
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
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Suche",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Adresse suchen...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Button "In diesem Bereich suchen"
            // Durchscheinend mit Fokus auf der Schrift
            Button(
                onClick = {
                    scope.launch {
                        // Lädt die Parkplätze einmalig aus Firebase
                        parkingSpots = firebaseHelper.getAllSpotsOnce()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "In diesem Bereich suchen",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Button zum Neuzentrieren auf den aktuellen Standort
        FloatingActionButton(
            onClick = { mapViewportState.transitionToFollowPuckState() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Standort fokussieren"
            )
        }
    }
}
