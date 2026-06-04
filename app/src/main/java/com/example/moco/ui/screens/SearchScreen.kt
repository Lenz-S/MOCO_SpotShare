package com.example.moco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Screen für die Adresssuche.
 * 
 * Aktueller Fokus: Nur die reine Eingabe der Adresse ermöglichen.
 * Die Sprung-Logik zur Karte wurde vorerst entfernt.
 * 
 * @param onBackClick Funktion zum Zurückkehren zur Karte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit
) {
    // searchQuery speichert den aktuell eingegebenen Text
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adresse suchen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Textfeld für die Adresseingabe.
            // KeyboardType.Text stellt sicher, dass alle Zeichen (inkl. ä, ö, ü) eingegeben werden können.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Straße, Stadt oder PLZ") },
                placeholder = { Text("z.B. Alexanderplatz, Berlin") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { 
                        // Hier wird später die Suche gestartet.
                        // Für den Moment wird nur der Fokus entfernt oder die Tastatur geschlossen.
                    }
                )
            )

            // Button zum Starten der Suche (Logik folgt in einem späteren Schritt)
            Button(
                onClick = { 
                    /* Logik für die Suche wird später implementiert */ 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.isNotBlank()
            ) {
                Text("Suchen")
            }

            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}
