package com.example.moco.model

import com.google.firebase.firestore.PropertyName
import java.util.UUID

/**
 * Datenmodell für einen Parkplatz.
 * Erweitert um Verfügbarkeitszeiten und Buchungsinformationen.
 */
data class ParkingSpot(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null,
    val qrCodeData: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Verfügbarkeits-Zeitfenster (z.B. "Mo-Fr, 08:00-17:00")
    val availabilitySchedule: String = "Immer verfügbar",
    
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    
    // Wer mietet den Platz gerade?
    val currentTenantId: String? = null,
    val currentTenantName: String? = null,
    // Welches Kennzeichen hat das Auto des aktuellen Mieters?
    val currentTenantLicensePlate: String? = null,

    val ownerId: String = "default_user",
    val ownerName: String = ""
)
