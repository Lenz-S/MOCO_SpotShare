package com.example.moco.model

import java.util.UUID

/**
 * Datenmodell für einen Parkplatz.
 * Dies ist die zentrale Datenstruktur für die Zusammenarbeit im Team.
 */
data class ParkingSpot(
    // Eindeutige ID für die Datenbank
    val id: String = UUID.randomUUID().toString(),
    // Titel des Parkplatzes (z.B. "Garage am Hauptbahnhof")
    val title: String,
    // Nähere Beschreibung
    val description: String = "",
    // Geografische Koordinaten für Mapbox
    val latitude: Double,
    val longitude: Double,
    // Menschenlesbare Adresse
    val address: String = "",
    // Für "Spot-Foto": Pfad oder URL zum aufgenommenen Bild
    val imageUrl: String? = null,
    // Für "QR-Check-In": Ein Code, der im QR-Code enthalten ist
    val qrCodeData: String? = null,
    // Für "Vermieter-Benachrichtigung": Wer mietet den Platz gerade?
    val currentTenantId: String? = null,
    // Verfügbarkeitsstatus
    val isAvailable: Boolean = true,
    // ID des Erstellers/Besitzers
    val ownerId: String = "default_user"
)
