package com.example.moco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Screen zum Hinzufügen eines neuen Parkplatzes.
 * Hier werden die Details wie Titel, Beschreibung und Preis abgefragt.
 * 
 * @param onBackClick Funktion zum Zurückkehren auf die Karte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(onBackClick: () -> Unit) {
    // State-Variablen für die Eingabefelder
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pricePerHour by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuen Parkplatz hinzufügen") },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Eingabefeld für den Namen des Parkplatzes
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Titel / Name") },
                placeholder = { Text("z.B. Tiefgarage Innenstadt") },
                isError = titleError,
                supportingText = {
                    if (titleError) {
                        Text("Der Titel darf nicht leer sein", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Eingabefeld für die Beschreibung
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung") },
                placeholder = { Text("Details zum Parkplatz, Zufahrt, Besonderheiten...") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Eingabefeld für den Preis (nur Zahlen erlaubt)
            OutlinedTextField(
                value = pricePerHour,
                onValueChange = { input ->
                    // Erlaubt nur Zahlen und maximal zwei Nachkommastellen
                    if (input.isEmpty() || input.matches(Regex("""^\d*[.,]?\d{0,2}$"""))) {
                        pricePerHour = input
                    }
                },
                label = { Text("Gebühr pro Stunde (€)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Button zum Speichern (Logik wird später implementiert)
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        // Hier käme später die Speicher-Logik hin
                        onBackClick() // Vorläufig kehren wir einfach zurück
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Parkplatz erstellen", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
