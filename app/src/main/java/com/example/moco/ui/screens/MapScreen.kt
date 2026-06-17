package com.example.moco.ui.screens

import android.Manifest
import android.util.Log
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
import com.example.moco.data.GeocodingHelper
import com.example.moco.model.ParkingSpot
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch

/**
 * MapScreen: Das Herzstück der Mieter-Interaktion.
 * Zeigt alle Parkplätze in Echtzeit an und regelt den Buchungsprozess via UI-Overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onSearchClick: () -> Unit
) {
    // --- 1. VIEW-MODEL ERSATZ (State Management) ---
    val context = LocalContext.current
    val scope = rememberCoroutineScope() 
    val firebaseHelper = remember { FirebaseHelper() }
    val geocodingHelper = remember { GeocodingHelper(context) }
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Aktuelle Benutzerinfos
    val userId = sharedPrefs.getString("user_id", "user_number_one") ?: "user_number_one"
    val userName = sharedPrefs.getString("real_name", "Daniela Kucharczyk") ?: "Daniela Kucharczyk"
    val userLicensePlate = sharedPrefs.getString("license_plate", "K-XY 123") ?: "K-XY 123"

    // NEBENLÄUFIGKEIT: Beobachtet den Echtzeit-Datenstrom von Firebase
    val parkingSpots by firebaseHelper.observeAllSpots().collectAsState(initial = emptyList())
    
    // UI-States für Selektion und Overlay
    var selectedSpotId by remember { mutableStateOf<String?>(null) }
    val selectedSpot = remember(selectedSpotId, parkingSpots) { 
        parkingSpots.find { it.id == selectedSpotId } 
    }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // State für die integrierte Suche und den Suchpunkt
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchedLocation by remember { mutableStateOf<Point?>(null) }

    val mapViewportState = rememberMapViewportState()

    // --- 2. BERECHTIGUNGEN & STANDORT ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            mapViewportState.transitionToFollowPuckState()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // --- 3. UI: KARTEN-KOMPONENTE (MAPBOX) ---
    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            scaleBar = {},
        ) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    locationPuck = createDefault2DPuck(withBearing = true)
                    puckBearingEnabled = true
                    puckBearing = PuckBearing.HEADING
                }
                mapViewportState.transitionToFollowPuckState(
                    followPuckViewportStateOptions = FollowPuckViewportStateOptions.Builder().zoom(12.0).build()
                )
            }

            // RENDERING: Zeichnet die Pins basierend auf der aktuellen Liste (Reaktiv)
            parkingSpots.forEach { spot ->
                val point = Point.fromLngLat(spot.longitude, spot.latitude)
                
                // AUTOMATIK: Farbe ändert sich basierend auf Zeitplan und Belegung
                val pinColor = when {
                    !spot.isCurrentlyInSchedule() -> Color.Gray // Geschlossen
                    spot.isAvailable -> Color(0xFF4CAF50) // Frei
                    else -> Color(0xFFF44336) // Belegt
                }
                
                key(spot.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(point)
                            allowOverlap(true)
                            allowOverlapWithPuck(true)
                            visible(true)
                            variableAnchors(listOf(
                                ViewAnnotationAnchorConfig.Builder().anchor(ViewAnnotationAnchor.BOTTOM).build()
                            ))
                        }
                    ) {
                        // PIN INTERAKTION: Öffnet das Detail-Overlay
                        Box(modifier = Modifier.wrapContentSize().clickable {
                            selectedSpotId = spot.id
                            showBottomSheet = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Pin",
                                tint = pinColor,
                                modifier = Modifier.size(42.dp).shadow(2.dp, shape = RoundedCornerShape(21.dp))
                            )
                        }
                    }
                }
            }

            // RENDERING: Gezeichneter Suchpunkt (blauer Punkt bei Adresssuche)
            searchedLocation?.let { point ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(point)
                        allowOverlap(true)
                        visible(true)
                        variableAnchors(listOf(
                            ViewAnnotationAnchorConfig.Builder().anchor(ViewAnnotationAnchor.CENTER).build()
                        ))
                    }
                ) {
                    Surface(
                        modifier = Modifier.size(14.dp),
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFF2196F3), // Blau
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                        shadowElevation = 4.dp
                    ) {}
                }
            }
        }

        // --- 4. UI-OVERLAY: SUCHE ---
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Suche", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Parkplatz suchen...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty() || searchedLocation != null) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    searchedLocation = null
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Löschen")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    isSearching = true
                                    scope.launch {
                                        val coords = geocodingHelper.getCoordinatesFromAddress(searchQuery)
                                        if (coords != null) {
                                            val targetPoint = Point.fromLngLat(coords.second, coords.first)
                                            searchedLocation = targetPoint
                                            mapViewportState.setCameraOptions(
                                                CameraOptions.Builder()
                                                    .center(targetPoint)
                                                    .zoom(14.0).build()
                                            )
                                        } else {
                                            Toast.makeText(context, "Ort nicht gefunden", Toast.LENGTH_SHORT).show()
                                        }
                                        isSearching = false
                                    }
                                }
                            }
                        )
                    )
                    
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }

        // --- 5. UI-OVERLAY: DETAIL-MODAL (Aufgabe: Status-Management) ---
        if (showBottomSheet && selectedSpot != null) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
                selectedSpot?.let { spot ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = spot.title, style = MaterialTheme.typography.headlineMedium)
                        Text(text = spot.address, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Anbieter: ${spot.ownerName}", style = MaterialTheme.typography.bodyMedium)
                        
                        HorizontalDivider()
                        
                        // LIVE-STATUS ANZEIGE
                        val (statusText, statusColor) = when {
                            !spot.isCurrentlyInSchedule() -> "Derzeit geschlossen" to Color.Gray
                            spot.isAvailable -> "Verfügbar" to Color(0xFF4CAF50)
                            else -> "Belegt" to Color(0xFFF44336)
                        }
                        Text(text = "Status: $statusText", color = statusColor, style = MaterialTheme.typography.titleMedium)

                        // NEBENLÄUFIGKEIT: Transaktions-Buttons (Check-In / Check-Out)
                        when {
                            !spot.isCurrentlyInSchedule() -> {
                                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                    Text("Außerhalb der Öffnungszeiten")
                                }
                            }
                            spot.isAvailable -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            firebaseHelper.checkIn(spot, userId, userName, userLicensePlate)
                                            showBottomSheet = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Jetzt Einchecken") }
                            }
                            spot.currentTenantId == userId -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            spot.activeBookingId?.let { firebaseHelper.checkOut(spot.id, it) }
                                            showBottomSheet = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) { Text("Auschecken") }
                            }
                            else -> {
                                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                    Text("Besetzt durch ${spot.currentTenantName}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 6. UI-ELEMENT: STANDORT-BUTTON (Zurück zur eigenen Position) ---
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
