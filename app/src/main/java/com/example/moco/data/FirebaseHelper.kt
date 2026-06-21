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
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * FirebaseHelper: Zentrale Daten-Schnittstelle der App (Repository-Ersatz).
 * Kapselt alle asynchronen Operationen für Firestore und Storage.
 */
class FirebaseHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val spotsCollection = firestore.collection("parking_spots")
    private val bookingsCollection = firestore.collection("bookings")
    private val notificationsCollection = firestore.collection("notifications")

    /**
     * Sendet eine Benachrichtigungs-Anfrage an Firestore.
     * Ein Listener in der App des Empfängers wird diese bemerken und lokal anzeigen.
     */
    suspend fun sendNotificationRequest(targetUserId: String, title: String, message: String) {
        val notificationData = mapOf(
            "id" to UUID.randomUUID().toString(),
            "targetUserId" to targetUserId,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        notificationsCollection.document(notificationData["id"] as String).set(notificationData).await()
    }

    /**
     * Beobachtet eingehende Benachrichtigungen für einen spezifischen Nutzer.
     */
    fun observeNotifications(userId: String): Flow<List<Map<String, Any>>> {
        return notificationsCollection
            .whereEqualTo("targetUserId", userId)
            .whereEqualTo("isRead", false)
            .snapshots()
            .map { it.documents.map { doc -> doc.data ?: emptyMap() } }
    }

    /**
     * Markiert eine Benachrichtigung als gelesen, damit sie nicht doppelt angezeigt wird.
     */
    suspend fun markNotificationAsRead(notificationId: String) {
        notificationsCollection.document(notificationId).update("isRead", true).await()
    }

    /**
     * Speichert das Push-Token eines Nutzers in Firestore.
     * Dies ermöglicht es dem Server (oder anderen Clients), gezielt Nachrichten an diesen Nutzer zu senden.
     */
    suspend fun updatePushToken(userId: String, token: String) {
        firestore.collection("users").document(userId).set(
            mapOf("fcmToken" to token),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun uploadSpotImageCompressed(context: Context, imageUri: Uri, spotId: String): String {
        val storageRef = storage.reference.child("spots/$spotId.jpg")

        // 1. Bild lokal komprimieren, um Datenvolumen und Speicherplatz zu sparen
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Skalieren (z.B. maximale Breite/Höhe von 1024 Pixeln beibehalten)
        val maxSize = 1024
        val width = originalBitmap.width
        val height = originalBitmap.height
        val bitmap = if (width > maxSize || height > maxSize) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
            val newHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        // In ein Byte-Array mit 80% JPEG-Qualität schreiben
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageData = baos.toByteArray()

        // 2. Upload der komprimierten Bytes statt der riesigen Originaldatei
        storageRef.putBytes(imageData).await()

        // 3. Download-URL für Firestore zurückgeben
        return storageRef.downloadUrl.await().toString()
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
     * Beobachtet Parkplätze eines spezifischen Besitzers in Echtzeit.
     */
    fun observeSpotsByOwner(ownerId: String): Flow<List<ParkingSpot>> {
        return spotsCollection.whereEqualTo("ownerId", ownerId).snapshots().map { querySnapshot ->
            querySnapshot.toObjects(ParkingSpot::class.java)
        }
    }

    /**
     * Löscht einen Parkplatz aus der Datenbank.
     */
    suspend fun deleteParkingSpot(spotId: String) {
        spotsCollection.document(spotId).delete().await()
    }

    /**
     * Holt Parkplätze für die Übersicht der eigenen Spots.
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