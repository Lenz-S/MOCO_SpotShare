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
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun uploadSpotImageCompressed(context: Context, imageUri: Uri, spotId: String): String = withContext(Dispatchers.IO) {
        val storageRef = storage.reference.child("spots/$spotId.jpg")

        // 1. Bild lokal komprimieren
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("Bild konnte nicht geladen werden")
        
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("Bildformat wird nicht unterstützt")
        
        inputStream.close()

        // Skalieren (maximale Breite/Höhe von 1024 Pixeln)
        val maxSize = 1024
        val width = originalBitmap.width
        val height = originalBitmap.height
        val bitmap = if (width > maxSize || height > maxSize) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
            val newHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
            originalBitmap.scale(newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        // In ein Byte-Array mit 80% JPEG-Qualität schreiben
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageData = baos.toByteArray()
        
        // Speicher freigeben
        if (bitmap != originalBitmap) bitmap.recycle()
        originalBitmap.recycle()

        // 2. Upload der komprimierten Bytes
        val uploadTask = storageRef.putBytes(imageData)
        
        // Warte bis der Upload fertig ist
        uploadTask.await()
        
        // Hole die URL ab
        storageRef.downloadUrl.await().toString()
    }

    // --- NEBENLÄUFIGKEIT: ASYNCHRONE SCHREIBZUGRIFFE (Deine Aufgabe) ---

    /**
     * Erstellt oder aktualisiert einen Parkplatz in der Datenbank.
     */
    suspend fun saveParkingSpot(spot: ParkingSpot) {
        spotsCollection.document(spot.id).set(spot).await()
    }

    /**
     * Convenience: Erstellt ein ParkingSpot-Objekt, lädt optional ein Bild hoch und speichert alles in Firestore.
     */
    suspend fun createAndSaveParkingSpot(
        context: Context,
        imageUri: Uri?,
        title: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        ownerId: String,
        ownerName: String,
        availableDays: List<Int>,
        startMinute: Int,
        endMinute: Int
    ) {
        val spotId = UUID.randomUUID().toString()
        var uploadedUrl: String? = null
        imageUri?.let { uri ->
            uploadedUrl = uploadSpotImageCompressed(context, uri, spotId)
        }

        val spot = ParkingSpot(
            id = spotId,
            title = title,
            description = description,
            address = address,
            latitude = latitude,
            longitude = longitude,
            imageUrl = uploadedUrl,
            availableDays = availableDays,
            startMinute = startMinute,
            endMinute = endMinute,
            ownerId = ownerId,
            ownerName = ownerName
        )

        saveParkingSpot(spot)
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