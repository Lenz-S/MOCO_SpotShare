package com.example.moco.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(
    onNavigateBack: () -> Unit,
    onSaveSpot: (title: String, description: String, address: String, pricePerHour: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pricePerHour by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuen Parkplatz hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Adresse") },
                placeholder = { Text("Straße, Hausnummer, PLZ, Ort") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = pricePerHour,
                onValueChange = { input ->
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

            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val price = pricePerHour.replace(',', '.').toDoubleOrNull() ?: 0.0
                        onSaveSpot(title, description, address, price)
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

