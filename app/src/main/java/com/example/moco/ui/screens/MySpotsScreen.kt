package com.example.moco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.moco.model.ParkingSpot
import com.example.moco.viewmodel.ParkingViewModel

/**
 * Screen für die eigenen Parkplätze des Benutzers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySpotsScreen(
    viewModel: ParkingViewModel,
    onBackClick: () -> Unit
) {
    val spots by viewModel.parkingSpots.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eigene Parkplätze") },
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
        if (spots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Du hast noch keine Parkplätze hinzugefügt.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(spots) { spot ->
                    SpotListItem(spot = spot)
                }
            }
        }
    }
}

@Composable
fun SpotListItem(spot: ParkingSpot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = spot.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (spot.address.isNotBlank()) {
                Text(
                    text = spot.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = spot.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.2f € / Std", spot.pricePerHour),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (spot.isAvailable) "Verfügbar" else "Belegt",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (spot.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
