package com.example.moco.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moco.data.FirebaseHelper
import com.example.moco.data.GeocodingHelper
import com.example.moco.model.ParkingSpot
import kotlinx.coroutines.launch

/**
 * AddSpotScreen: Ermöglicht das Anlegen eines neuen Parkplatzes.
 * Beinhaltet nun auch die Konfiguration der zeitlichen Verfügbarkeit.
 *
 * @param onBackClick Navigation zurück zur Kartenansicht.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialisierung der Daten-Helfer
    val firebaseHelper = remember { FirebaseHelper() }
    val geocodingHelper = remember { GeocodingHelper(context) }
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }

    // Zustandsvariablen für die Formulareingaben
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    // ZUSTAND: Zeitliche Verfügbarkeit
    val daysOfWeek = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    // Mapping auf Calendar-Konstanten: Mo=2, Di=3, Mi=4, Do=5, Fr=6, Sa=7, So=1
    val daysMapping = listOf(2, 3, 4, 5, 6, 7, 1)
    var selectedDays by remember { mutableStateOf(daysMapping.toSet()) }
    
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("20:00") }

    // Zustände für Validierung und Ladevorgang
    var titleError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Abruf der im Profil hinterlegten Benutzer-ID und des Klarnamens
    val currentUserId = sharedPrefs.getString("user_id", "user_default") ?: "user_default"
    val currentUserName = sharedPrefs.getString("real_name", "Benutzer") ?: "Benutzer"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuen Parkplatz hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Eingabefeld für den Parkplatz-Namen
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Titel / Name") },
                    placeholder = { Text("z.B. Garage am Park") },
                    isError = titleError,
                    supportingText = {
                        if (titleError) Text("Bitte gib einen Titel an", color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // Eingabefeld für die physische Adresse (wird für Geocoding genutzt)
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        if (it.isNotBlank()) addressError = false
                    },
                    label = { Text("Adresse") },
                    placeholder = { Text("Straße, PLZ, Ort") },
                    isError = addressError,
                    supportingText = {
                        if (addressError) Text("Die Adresse ist erforderlich", color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // Eingabefeld für zusätzliche Details
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung") },
                    placeholder = { Text("Besonderheiten zur Zufahrt etc.") },
                    minLines = 3,
                    maxLines = 3,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                // --- NEU: Zeitliche Verfügbarkeit ---
                Text(
                    text = "Verfügbarkeit festlegen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Wochentage Auswahl
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wochentage", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysOfWeek.forEachIndexed { index, day ->
                                val dayValue = daysMapping[index]
                                val isSelected = selectedDays.contains(dayValue)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDays = if (isSelected) {
                                            selectedDays - dayValue
                                        } else {
                                            selectedDays + dayValue
                                        }
                                    },
                                    label = { Text(day, style = MaterialTheme.typography.bodySmall) },
                                    enabled = !isSaving
                                )
                            }
                        }
                    }
                }

                // Uhrzeiten Auswahl
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { if (it.length <= 5) startTime = it },
                        label = { Text("Von (HH:mm)") },
                        placeholder = { Text("08:00") },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { if (it.length <= 5) endTime = it },
                        label = { Text("Bis (HH:mm)") },
                        placeholder = { Text("20:00") },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speicher-Button mit integrierter Geocoding- und Firebase-Logik
                Button(
                    onClick = {
                        val startMin = parseTimeToMinutes(startTime)
                        val endMin = parseTimeToMinutes(endTime)

                        when {
                            title.isBlank() -> titleError = true
                            address.isBlank() -> addressError = true
                            selectedDays.isEmpty() -> Toast.makeText(context, "Bitte mindestens einen Tag wählen", Toast.LENGTH_SHORT).show()
                            startMin == null || endMin == null -> Toast.makeText(context, "Ungültiges Zeitformat (HH:mm)", Toast.LENGTH_SHORT).show()
                            startMin >= endMin -> Toast.makeText(context, "Startzeit muss vor Endzeit liegen", Toast.LENGTH_SHORT).show()
                            else -> {
                                isSaving = true
                                scope.launch {
                                    try {
                                        // 1. Schritt: Adresse in Koordinaten umwandeln (Geocoding)
                                        val coords = geocodingHelper.getCoordinatesFromAddress(address)

                                        if (coords == null) {
                                            isSaving = false
                                            Toast.makeText(context, "Adresse konnte nicht gefunden werden", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }

                                        // 2. Schritt: Datenmodell befüllen mit zeitlicher Verfügbarkeit
                                        val newSpot = ParkingSpot(
                                            title = title,
                                            description = description,
                                            address = address,
                                            latitude = coords.first,
                                            longitude = coords.second,
                                            ownerId = currentUserId,
                                            ownerName = currentUserName,
                                            availableDays = selectedDays.toList().sorted(),
                                            startMinute = startMin,
                                            endMinute = endMin
                                        )

                                        // 3. Schritt: In der Cloud-Datenbank speichern
                                        firebaseHelper.saveParkingSpot(newSpot)

                                        // Erfolgreich gespeichert -> Zurück zur Karte
                                        isSaving = false
                                        onBackClick()
                                    } catch (e: Exception) {
                                        // Fehlerbehandlung
                                        isSaving = false
                                        Toast.makeText(context, "Fehler: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Parkplatz erstellen", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            
            // Graues Overlay während des Speichervorgangs zur visuellen Sperrung
            if (isSaving) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {}
            }
        }
    }
}

/**
 * Hilfsfunktion zur Umrechnung von HH:mm in Minuten ab Mitternacht.
 */
private fun parseTimeToMinutes(time: String): Int? {
    return try {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        if (hours in 0..23 && minutes in 0..59) {
            hours * 60 + minutes
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
