package com.example.moco.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
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
 * Hier kann der Nutzer seinen Team-Namen (User ID) festlegen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }
    
    // Konstante für deinen echten Namen
    val realName = "Daniela Kucharczyk"
    
    // Lädt die gespeicherte ID oder nutzt "user_number_one" als Standard
    var userId by remember { 
        mutableStateOf(sharedPrefs.getString("user_id", "user_number_one") ?: "user_number_one") 
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Anzeige deines echten Namens
            Text(
                text = realName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Deine Benutzer-ID",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "Diese ID wird zur Identifizierung deiner Parkplätze in der Datenbank verwendet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("Benutzer-ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    sharedPrefs.edit()
                        .putString("user_id", userId)
                        .putString("real_name", realName)
                        .apply()
                    // Optional: Feedback für den Nutzer
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ID speichern")
            }
        }
    }
}
