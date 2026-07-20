package com.example.moco.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.moco.data.FirebaseHelper
import com.example.moco.data.GeocodingHelper
import com.example.moco.data.ImageHelper
import com.example.moco.model.ParkingSpot
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(spotId: String? = null, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val firebaseHelper = remember { FirebaseHelper() }
    val geocodingHelper = remember { GeocodingHelper(context) }
    val imageHelper = remember { ImageHelper(context) }
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf(1) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }

    // Zeit-Zustände
    val daysOfWeek = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val daysMapping = listOf(2, 3, 4, 5, 6, 7, 1)
    var selectedDays by remember { mutableStateOf(daysMapping.toSet()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("20:00") }

    var isLoadingExisting by remember { mutableStateOf(spotId != null) }
    var titleError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Laden der bestehenden Daten, falls im Edit-Modus
    LaunchedEffect(spotId) {
        if (spotId != null) {
            val spot = firebaseHelper.getParkingSpot(spotId)
            if (spot != null) {
                title = spot.title
                description = spot.description
                address = spot.address
                capacity = spot.capacity
                selectedDays = spot.availableDays.toSet()
                startTime = String.format(Locale.getDefault(), "%02d:%02d", spot.startMinute / 60, spot.startMinute % 60)
                endTime = String.format(Locale.getDefault(), "%02d:%02d", spot.endMinute / 60, spot.endMinute % 60)
                existingImageUrl = spot.imageUrl
            }
            isLoadingExisting = false
        }
    }

    // Vorbereitung der Datei für das Kamerabild
    val tempImageUri = remember {
        val directory = File(context.cacheDir, "images")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "temp_image.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // Launcher für Kamera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = tempImageUri
        }
    }

    // Hilfsfunktion zum Starten der Kamera
    val launchCamera = {
        try {
            val file = File(context.cacheDir, "images/temp_image.jpg")
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            cameraLauncher.launch(tempImageUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Kamera-Fehler: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher für Galerie
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera()
        else Toast.makeText(context, "Kamera-Berechtigung wird benötigt", Toast.LENGTH_SHORT).show()
    }

    val currentUserId = sharedPrefs.getString("user_id", "user_default") ?: "user_default"
    val currentUserName = sharedPrefs.getString("real_name", "Benutzer") ?: "Benutzer"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (spotId == null) "Parkplatz anbieten" else "Parkplatz bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoadingExisting) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "Foto hinzufügen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (existingImageUrl != null) {
                             Image(
                                painter = rememberAsyncImagePainter(existingImageUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) launchCamera()
                            else permissionLauncher.launch(Manifest.permission.CAMERA)
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Kamera")
                        }
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Galerie")
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = false },
                        label = { Text("Titel") },
                        isError = titleError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; addressError = false },
                        label = { Text("Adresse") },
                        isError = addressError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Anzahl der Plätze: $capacity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = capacity.toFloat(),
                        onValueChange = { capacity = it.toInt() },
                        valueRange = 1f..99f,
                        steps = 98,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Beschreibung") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Verfügbarkeit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            daysOfWeek.forEachIndexed { index, day ->
                                val dayValue = daysMapping[index]
                                val isSelected = selectedDays.contains(dayValue)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDays = if (isSelected) selectedDays - dayValue else selectedDays + dayValue },
                                    label = { Text(day, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Von") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("Bis") }, modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            val startMin = parseTimeToMinutes(startTime)
                            val endMin = parseTimeToMinutes(endTime)
                            if (title.isBlank()) titleError = true
                            else if (address.isBlank()) addressError = true
                            else {
                                isSaving = true
                                scope.launch {
                                    try {
                                        val encodedImage = if (selectedImageUri != null) imageHelper.convertUriToBase64(selectedImageUri!!) else existingImageUrl
                                        val coords = geocodingHelper.getCoordinatesFromAddress(address)
                                        if (coords != null) {
                                            val newSpot = ParkingSpot(
                                                id = spotId ?: java.util.UUID.randomUUID().toString(),
                                                title = title,
                                                description = description,
                                                address = address,
                                                latitude = coords.first,
                                                longitude = coords.second,
                                                capacity = capacity,
                                                ownerId = currentUserId,
                                                ownerName = currentUserName,
                                                availableDays = selectedDays.toList(),
                                                startMinute = startMin ?: 0,
                                                endMinute = endMin ?: 1439,
                                                imageUrl = encodedImage
                                            )
                                            firebaseHelper.saveParkingSpot(newSpot)
                                            onBackClick()
                                        } else {
                                            Toast.makeText(context, "Adresse nicht gefunden", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Fehler: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text(if (spotId == null) "Parkplatz erstellen" else "Änderungen speichern")
                    }
                }
            }
        }
    }
}

private fun parseTimeToMinutes(time: String): Int? {
    return try {
        val parts = time.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) { null }
}
