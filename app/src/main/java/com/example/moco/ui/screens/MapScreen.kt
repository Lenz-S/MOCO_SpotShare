package com.example.moco.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions

/**
 * Haupt-Kartenbildschirm der App (MapScreen).
 * 
 * Diese Komponente ist für die Anzeige der Mapbox-Weltkarte zuständig.
 * Sie kümmert sich um:
 * 1. Die Abfrage der Standortberechtigungen beim Nutzer.
 * 2. Die Anzeige des aktuellen Standorts (blauer Punkt/Puck).
 * 3. Das automatische Zoomen auf die Umgebung des Nutzers beim Start.
 * 4. Interaktive Elemente über der Karte (z.B. Standort-Button).
 */
@Composable
fun MapScreen(onSearchClick: () -> Unit) {
    // mapViewportState verwaltet den sichtbaren Bereich der Karte (Kamera).
    // rememberMapViewportState stellt sicher, dass der Zustand bei UI-Updates erhalten bleibt.
    val mapViewportState = rememberMapViewportState()

    // permissionLauncher definiert den Dialog zur Abfrage von System-Berechtigungen (GPS).
    // ActivityResultContracts.RequestMultiplePermissions erlaubt das gleichzeitige Anfragen von Fine & Coarse Location.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Callback nach der Nutzerentscheidung
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Falls Berechtigung erteilt wurde: Kamera sofort auf den Nutzer ausrichten.
            mapViewportState.transitionToFollowPuckState()
        }
    }

    // LaunchedEffect(Unit) wird genau einmal ausgeführt, wenn dieser Screen zum ersten Mal erscheint.
    // Hier triggern wir die Standortabfrage.
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Box dient als Container, um UI-Elemente (Buttons, Suche) über der Karte zu platzieren.
    Box(modifier = Modifier.fillMaxSize()) {
        // Die eigentliche Mapbox-Kartenkomponente.
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
        ) {
            // MapEffect bietet einen "Escape Hatch", um direkt auf die zugrunde liegende MapView zuzugreifen.
            // Dies ist notwendig für Features, die in der Compose-API noch nicht direkt als Parameter existieren.
            MapEffect(Unit) { mapView ->
                // Zugriff auf das Location-Plugin von Mapbox, um den "Puck" zu konfigurieren.
                mapView.location.updateSettings {
                    enabled = true // Aktiviert die Standordanzeige
                    // Erstellt einen 2D-Puck (blauer Kreis) mit einer Richtungsanzeige (Bearing).
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING // Richtet den Pfeil nach der Blickrichtung aus
                }
                
                // Kamera-Einstellung: "transitionToFollowPuckState" sorgt dafür, dass die Karte dem Nutzer folgt.
                // Wir setzen hier einen festen Zoom-Wert von 15.0 für eine gute Übersicht der Umgebung.
                mapViewportState.transitionToFollowPuckState(
                    followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder()
                        .zoom(12.0)
                        .build()
                )
            }
        }

        // --- UI Elemente über der Karte ---

        // FloatingActionButton zum Zentrieren der Karte auf den aktuellen Standort.
        // Erscheint unten rechts über der BottomBar.
        FloatingActionButton(
            onClick = {
                // Kamera wird wieder auf den Standort-Puck fokussiert und folgt ihm.
                mapViewportState.transitionToFollowPuckState()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd) // Positioniert den Button unten rechts im Box-Container
                .padding(16.dp), // Abstand vom Rand (und damit von der BottomBar)
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Meinen Standort fokussieren"
            )
        }
    }
}
