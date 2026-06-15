package com.example.moco.model

import java.util.UUID

/**
 * Datenmodell für eine Buchung (Transaktions-Historie).
 */
data class Booking(
    val id: String = UUID.randomUUID().toString(),
    // Verknüpfung zum Parkplatz
    val spotId: String = "",
    val spotTitle: String = "",
    // Informationen zum Mieter
    val tenantId: String = "",
    val tenantName: String = "",
    val tenantLicensePlate: String = "",
    // Zeitliche Informationen
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null, // null, solange die Buchung aktiv ist
    // Status der Buchung
    val isActive: Boolean = true
)
