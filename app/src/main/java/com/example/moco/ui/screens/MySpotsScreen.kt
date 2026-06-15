package com.example.moco.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moco.data.FirebaseHelper
import com.example.moco.model.ParkingSpot

/**
 * MySpotsScreen: Zeigt eine Liste aller Parkplätze an, die der aktuelle Nutzer erstellt hat.
 * Ermöglicht die Übersicht über den Status (verfügbar/belegt) der eigenen Angebote.
 * 
 * @param onBackClick Navigation zurück zur Karte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySpotsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val firebaseHelper = remember { FirebaseHelper() }
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }
    
    // Holt die gespeicherte Benutzer-ID (Standard: user_number_one)
    val userId = sharedPrefs.getString("user_id", "user_number_one") ?: "user_number_one"

    // Zustände für die Parkplatz-Liste und den Ladevorgang
    var mySpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ruft die Daten ab, sobald der Screen geladen wird oder sich die userId ändert
    LaunchedEffect(userId) {
        isLoading = true
        mySpots = firebaseHelper.getSpotsByOwner(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Parkplätze") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück zur Karte"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                // Zeigt einen Ladekreis an, während die Daten von Firebase geladen werden
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (mySpots.isEmpty()) {
                // Hinweistext, falls noch keine eigenen Parkplätze existieren
                Text(
                    text = "Du hast noch keine Parkplätze angelegt.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Liste der eigenen Parkplätze
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mySpots) { spot ->
                        MySpotItem(spot)
                    }
                }
            }
        }
    }
}

/**
 * Einzelnes Listenelement für einen Parkplatz.
 * Zeigt Titel, Adresse und den aktuellen Verfügbarkeitsstatus an.
 */
@Composable
fun MySpotItem(spot: ParkingSpot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Titel des Parkplatzes
            Text(
                text = spot.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            // Adresse
            Text(
                text = spot.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status-Anzeige (Grün/Rot mit Text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusText = if (spot.isAvailable) "Verfügbar" else "Aktuell belegt"
                val statusColor = if (spot.isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = statusColor
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
