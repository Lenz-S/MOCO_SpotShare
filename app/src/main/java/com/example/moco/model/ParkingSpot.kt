package com.example.moco.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Calendar
import java.util.UUID

/**
 * ParkingSpot: Zentrales Datenmodell für einen Stellplatz.
 * 
 * ARCHITEKTUR-HINWEIS:
 * Dieses Modell dient als Basis für Firestore. Die Verfügbarkeit wird hier 
 * sowohl durch den Status (isAvailable) als auch durch den Zeitplan (availableDays, etc.) bestimmt.
 */
data class ParkingSpot(
    // Eindeutige Kennung (Wird automatisch generiert)
    val id: String = UUID.randomUUID().toString(),
    
    // Basis-Informationen (Eingabe durch Nils im AddSpotScreen)
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null,
    val qrCodeData: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 
     * STRUKTURIERTE VERFÜGBARKEIT (Deine Aufgabe):
     * Ermöglicht automatische Status-Updates basierend auf der Uhrzeit.
     */
    // Liste der Wochentage (1=So, 2=Mo, ..., 7=Sa)
    val availableDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), 
    // Start/Ende in Minuten ab Mitternacht (z.B. 480 = 08:00 Uhr)
    val startMinute: Int = 0,
    val endMinute: Int = 1439, 
    
    /** 
     * STATUS-MANAGEMENT:
     * isAvailable markiert die manuelle Sperrung/Freigabe durch Buchungen.
     */
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    
    // Mieter-Informationen (Wird beim Check-In durch dich befüllt)
    val currentTenantId: String? = null,
    val currentTenantName: String? = null,
    val currentTenantLicensePlate: String? = null,
    val activeBookingId: String? = null, // Verknüpfung zur aktuellen Transaktion

    // Vermieter-Informationen
    val ownerId: String = "default_user",
    val ownerName: String = ""
) {
    /**
     * NEBENLÄUFIGKEIT/LOGIK:
     * Berechnet basierend auf der aktuellen Systemzeit, ob der Platz geöffnet ist.
     * @Exclude sorgt dafür, dass diese berechnete Funktion nicht in Firebase gespeichert wird.
     */
    @Exclude
    fun isCurrentlyInSchedule(): Boolean {
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        
        val dayMatch = availableDays.contains(dayOfWeek)
        val timeMatch = currentMinute in startMinute..endMinute
        
        return dayMatch && timeMatch
    }
}
