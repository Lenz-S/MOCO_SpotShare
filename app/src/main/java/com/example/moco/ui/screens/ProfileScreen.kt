package com.example.moco.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Screen zur Profilverwaltung.
 * Hier kann der Nutzer seinen Anzeigenamen und seine User ID festlegen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }
    
    // Lädt die gespeicherten Daten
    var realName by remember { 
        mutableStateOf(sharedPrefs.getString("real_name", "Benutzer") ?: "Benutzer") 
    }
    var userId by remember { 
        mutableStateOf(sharedPrefs.getString("user_id", "user_default") ?: "user_default") 
    }
    var licensePlate by remember {
        mutableStateOf(sharedPrefs.getString("license_plate", "K-XY 123") ?: "K-XY 123")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Profil bearbeiten",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = realName,
                onValueChange = { realName = it },
                label = { Text("Vollständiger Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("Benutzer-ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = licensePlate,
                onValueChange = { licensePlate = it },
                label = { Text("KFZ-Kennzeichen") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    sharedPrefs.edit()
                        .putString("user_id", userId)
                        .putString("real_name", realName)
                        .putString("license_plate", licensePlate)
                        .apply()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Profil speichern")
            }
        }
    }
}
