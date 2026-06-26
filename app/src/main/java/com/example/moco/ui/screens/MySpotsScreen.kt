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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

/**
 * MySpotsScreen: Das Dashboard für Vermieter.
 * Zeigt eigene Parkplätze in Echtzeit an, inklusive Mieter-Details und Verwaltungs-Optionen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySpotsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseHelper = remember { FirebaseHelper() }
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }
    
    val userId = sharedPrefs.getString("user_id", "user_number_one") ?: "user_number_one"

    // NEBENLÄUFIGKEIT: Echtzeit-Beobachtung der eigenen Parkplätze
    val mySpots by firebaseHelper.observeSpotsByOwner(userId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vermieter-Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                mySpots == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                mySpots!!.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Keine Parkplätze gefunden.", style = MaterialTheme.typography.bodyLarge)
                        Text("Biete deinen ersten Parkplatz an!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(mySpots!!) { spot ->
                            MySpotDashboardItem(
                                spot = spot,
                                onDeleteClick = {
                                    scope.launch {
                                        firebaseHelper.deleteParkingSpot(spot.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MySpotDashboardItem(
    spot: ParkingSpot,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Parkplatz löschen?") },
            text = { Text("Möchtest du '${spot.title}' wirklich dauerhaft entfernen?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDeleteClick() 
                    showDeleteDialog = false 
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Titel und Lösch-Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = spot.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = spot.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status-Badge
            val (statusText, statusColor) = if (spot.isAvailable) {
                "FREI" to Color(0xFF4CAF50)
            } else {
                "BELEGT" to Color(0xFFF44336)
            }

            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }

            // Mieter-Details (nur wenn belegt)
            if (!spot.isAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(text = "Aktueller Mieter:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = spot.currentTenantName ?: "Unbekannt", style = MaterialTheme.typography.bodyLarge)
                }

                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = spot.currentTenantLicensePlate ?: "Kein Kennzeichen", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
