package com.example.moco.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // Falls du Coil für die Bildvorschau nutzt (empfohlen)
import com.example.moco.data.FirebaseHelper
import com.example.moco.data.GeocodingHelper
import com.example.moco.model.ParkingSpot
import kotlinx.coroutines.launch


/**
 * AddSpotScreen: Ermöglicht das Anlegen eines neuen Parkplatzes inklusive Galerie-Upload.
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

    // NEU: Zustand für das ausgewählte Bild aus der Galerie
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Zustände für Validierung und Ladevorgang
    var titleError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Abruf der im Profil hinterlegten Benutzer-ID
    val currentUserId = sharedPrefs.getString("user_id", "user_number_one") ?: "user_number_one"

    // NEU: Photo Picker Launcher für die Galerie
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

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

                // Eingabefeld für die physische Adresse
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
                    maxLines = 5,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                // --- NEU: BILD-SEKTION ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedImageUri != null) {
                            // Vorschau des ausgewählten Bildes (benötigt Coil-Bibliothek)
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Ausgewähltes Parkplatzfoto",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedImageUri == null) "Foto aus Galerie wählen" else "Foto ändern")
                        }
                    }
                }
                // -------------------------

                Spacer(modifier = Modifier.weight(1f))

                // Speicher-Button mit integrierter Geocoding-, Firebase- und Upload-Logik
                Button(
                    onClick = {
                        when {
                            title.isBlank() -> titleError = true
                            address.isBlank() -> addressError = true
                            else -> {
                                isSaving = true
                                scope.launch {
                                    try {
                                        // 1. Schritt: Adresse in Koordinaten umwandeln
                                        val coords = geocodingHelper.getCoordinatesFromAddress(address)

                                        if (coords == null) {
                                            isSaving = false
                                            Toast.makeText(context, "Adresse konnte nicht gefunden werden", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }

                                        // NEU -> 1.5 Schritt: Bild auf Firebase Storage hochladen, falls eines ausgewählt wurde
                                        var uploadedImageUrl: String? = null
                                        selectedImageUri?.let { uri ->
                                            // Hier rufen wir deine Upload-Funktion auf
                                            uploadedImageUrl = firebaseHelper.uploadImage(uri, context)
                                        }

                                        // 2. Schritt: Datenmodell befüllen (inklusive der neuen imageUrl)
                                        val newSpot = ParkingSpot(
                                            title = title,
                                            description = description,
                                            address = address,
                                            latitude = coords.first,
                                            longitude = coords.second,
                                            ownerId = currentUserId,
                                            imageUrl = uploadedImageUrl // Übergabe der URL an das Modell
                                        )

                                        // 3. Schritt: In der Cloud-Datenbank (Firestore) speichern
                                        firebaseHelper.saveParkingSpot(newSpot)

                                        // Erfolgreich gespeichert -> Zurück zur Karte
                                        isSaving = false
                                        onBackClick()
                                    } catch (e: Exception) {
                                        isSaving = false
                                        Toast.makeText(
                                            context,
                                            "Fehler: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
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

            // Graues Overlay während des Speichervorgangs
            if (isSaving) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ) {}
            }
        }
    }
}