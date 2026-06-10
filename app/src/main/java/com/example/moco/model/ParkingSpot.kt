package com.example.moco.model

import java.util.UUID

/**
 * Datenmodell für einen Parkplatz.
 * Dies ist die zentrale Datenstruktur für die Zusammenarbeit im Team.
 * Optimiert für die neuen Features: Foto-Upload, QR-Check-In und Regional-Radar.
 */
data class ParkingSpot(
    // Eindeutige ID für die Datenbank
    val id: String = UUID.randomUUID().toString(),
    
    // Titel des Parkplatzes (z.B. "Garage am Hauptbahnhof")
    val title: String = "",
    
    // Nähere Beschreibung (Zufahrt, Besonderheiten)
    val description: String = "",
    
    // Geografische Koordinaten für Mapbox und den regionalen Radar
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    
    // Menschenlesbare Adresse
    val address: String = "",
    
    // Für "Visuelle Dokumentation": URL zum Foto im Firebase Storage
    val imageUrl: String? = null,
    
    // Für "QR-basierter Echtzeit-Check-In": Der kodierte Inhalt des QR-Codes
    val qrCodeData: String? = null,
    
    // Zeitstempel der Erstellung (wichtig für den "Regionalen Parkplatz-Radar")
    // Speichert die Millisekunden seit 1970
    val createdAt: Long = System.currentTimeMillis(),
    
    // Verfügbarkeitsstatus (wichtig für den Vermieter-Check-In)
    val isAvailable: Boolean = true,
    
    // ID des aktuellen Mieters (null, wenn der Platz frei ist)
    val currentTenantId: String? = null,
    
    // ID des Besitzers (für die "Echtzeit-Vermieter-Info")
    val ownerId: String = "default_user"
)
