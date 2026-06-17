package com.example.moco.data

import android.net.Uri
import com.example.moco.model.Booking
import com.example.moco.model.ParkingSpot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * FirebaseHelper: Zentrale Daten-Schnittstelle der App (Repository-Ersatz).
 * Kapselt alle asynchronen Operationen für Firestore und Storage.
 */
class FirebaseHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val spotsCollection = firestore.collection("parking_spots")
    private val bookingsCollection = firestore.collection("bookings")

    // --- NEBENLÄUFIGKEIT: ASYNCHRONE MEDIA-UPLOAD-FUNKTION (Aufgabe für Nils) ---
    suspend fun uploadSpotImage(imageUri: Uri): String {
        val fileName = "spots/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(imageUri).await() // .await() pausiert die Coroutine, bis der Upload fertig ist
        return ref.downloadUrl.await().toString()
    }

    // --- NEBENLÄUFIGKEIT: ASYNCHRONE SCHREIBZUGRIFFE (Deine Aufgabe) ---

    /**
     * Erstellt oder aktualisiert einen Parkplatz in der Datenbank.
     */
    suspend fun saveParkingSpot(spot: ParkingSpot) {
        spotsCollection.document(spot.id).set(spot).await()
    }

    /**
     * Führt einen Check-In durch.
     * Nutzt zwei Collections parallel, um Daten-Konsistenz zu wahren.
     */
    suspend fun checkIn(spot: ParkingSpot, tenantId: String, tenantName: String, licensePlate: String) {
        val booking = Booking(
            spotId = spot.id,
            spotTitle = spot.title,
            tenantId = tenantId,
            tenantName = tenantName,
            tenantLicensePlate = licensePlate,
            startTime = System.currentTimeMillis()
        )
        
        // Speichert die Buchungshistorie
        bookingsCollection.document(booking.id).set(booking).await()
        
        // Aktualisiert den Live-Status des Parkplatzes
        spotsCollection.document(spot.id).update(
            mapOf(
                "isAvailable" to false,
                "currentTenantId" to tenantId,
                "currentTenantName" to tenantName,
                "currentTenantLicensePlate" to licensePlate,
                "activeBookingId" to booking.id
            )
        ).await()
    }

    /**
     * Gibt einen Parkplatz wieder frei.
     */
    suspend fun checkOut(spotId: String, bookingId: String) {
        bookingsCollection.document(bookingId).update(
            mapOf("endTime" to System.currentTimeMillis(), "isActive" to false)
        ).await()

        spotsCollection.document(spotId).update(
            mapOf(
                "isAvailable" to true,
                "currentTenantId" to null,
                "currentTenantName" to null,
                "currentTenantLicensePlate" to null,
                "activeBookingId" to null
            )
        ).await()
    }

    /**
     * Holt die Buchungshistorie eines Mieters.
     */
    suspend fun getBookingHistory(tenantId: String): List<Booking> {
        return try {
            val query = bookingsCollection
                .whereEqualTo("tenantId", tenantId)
                .get(Source.SERVER).await()
            query.toObjects(Booking::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- NEBENLÄUFIGKEIT: ECHTZEIT-DATENSTRÖME (FLOWS) ---

    /**
     * Beobachtet alle Parkplätze. 
     * Jedes Mal, wenn ein anderer Nutzer etwas ändert, "fließt" ein neues Ergebnis in die UI.
     */
    fun observeAllSpots(): Flow<List<ParkingSpot>> {
        return spotsCollection.snapshots().map { querySnapshot ->
            querySnapshot.toObjects(ParkingSpot::class.java)
        }
    }

    /**
     * Holt alle Parkplätze einmalig direkt vom Server.
     * Nutzt Source.SERVER, um den lokalen Cache zu umgehen.
     */
    suspend fun getAllSpotsOnce(): List<ParkingSpot> {
        return try {
            val query = spotsCollection.get(Source.SERVER).await()
            query.toObjects(ParkingSpot::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Holt Parkplätze für den MySpotsScreen (Aufgabe für Lenz).
     */
    suspend fun getSpotsByOwner(ownerId: String): List<ParkingSpot> {
        return try {
            val query = spotsCollection.whereEqualTo("ownerId", ownerId).get(Source.SERVER).await()
            query.toObjects(ParkingSpot::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
