package com.example.moco

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.moco.ui.theme.MOCOTheme
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Der Mapbox Access Token wird automatisch aus der Datei 
        // res/values/mapbox_access_token.xml geladen.
        // Ohne diesen Token kann die Karte keine Daten vom Mapbox-Server abrufen.
        
        // Aktiviert das Edge-to-Edge Design (Statusbar transparent etc.)
        enableEdgeToEdge()
        
        setContent {
            MOCOTheme {
                // Scaffold bietet ein Grundgerüst für die UI (z.B. für TopBars oder Padding)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MocoApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MocoApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // State um zu tracken, ob die Standortberechtigung gewährt wurde
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher um den System-Dialog für Berechtigungen anzuzeigen
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Effekt, der beim ersten Start prüft, ob Berechtigungen fehlen und diese ggf. anfragt
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ViewportState steuert die Kamera der Karte (Zoom, Position)
    val mapViewportState = rememberMapViewportState()

    // Die eigentliche Mapbox Karte als Compose Komponente
    MapboxMap(
        modifier = modifier.fillMaxSize(),
        mapViewportState = mapViewportState
    )
}

// --- Preview Bereich ---
// Diese Funktion ermöglicht die Anzeige der UI direkt im Android Studio Editor (Split-View).
// Hinweis: Mapbox-Karten werden in der Vorschau oft nur als Platzhalter (graue Fläche) 
// dargestellt, da sie eine aktive Grafik-Engine und Internet benötigen.
@Preview(showBackground = true, name = "Standard Vorschau")
@Composable
fun MocoAppPreview() {
    MOCOTheme {
        // Wir nutzen Scaffold auch in der Vorschau, um das reale Layout zu simulieren
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MocoApp(modifier = Modifier.padding(innerPadding))
        }
    }
}
