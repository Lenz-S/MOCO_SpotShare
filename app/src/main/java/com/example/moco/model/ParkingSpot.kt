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
    // Preis pro Stunde (später für Routing/Buchung wichtig)
    val pricePerHour: Double = 0.0,
    // Menschenlesbare Adresse
    val address: String = "",
    // Verfügbarkeitsstatus
    val isAvailable: Boolean = true,
    // ID des Erstellers/Besitzers
    val ownerId: String = "default_user"
)
