package com.example.moco.model

import com.google.firebase.firestore.PropertyName
import java.util.UUID

/**
 * Datenmodell für einen Parkplatz.
 * Optimiert für Firebase und Team-Features.
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
    
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    
    val currentTenantId: String? = null,
    val ownerId: String = "default_user"
)
