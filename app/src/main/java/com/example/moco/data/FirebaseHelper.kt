package com.example.moco.data

import android.net.Uri
import com.example.moco.model.ParkingSpot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
/**
 * Zentraler Helper für Firebase-Operationen.
 * Erleichtert die Zusammenarbeit im Team durch abstrahierte Methoden für Firestore und Storage.
 */
class FirebaseHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val spotsCollection = firestore.collection("parking_spots")

    // --- Speicher-Operationen (Storage) ---

    /**
     * Lädt ein Bild asynchron hoch und gibt die Download-URL zurück.
     * Unterstützt den Anwendungsfall "Visuelle Stellplatz-Dokumentation".
     */
    suspend fun uploadSpotImage(imageUri: Uri): String {
        val fileName = "spots/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }
    suspend fun uploadImage(uri: android.net.Uri, context: android.content.Context): String {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("parking_spots/${java.util.UUID.randomUUID()}.jpg")

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Datei konnte nicht geöffnet werden")

        storageRef.putStream(inputStream).await()
        return storageRef.downloadUrl.await().toString()
    }

    // --- Datenbank-Operationen (Firestore) ---

    /**
     * Erstellt oder aktualisiert einen Parkplatz in der Datenbank.
     */
    suspend fun saveParkingSpot(spot: ParkingSpot) {
        spotsCollection.document(spot.id).set(spot).await()
    }

    /**
     * Validiert einen QR-Code-String gegen die Datenbank.
     * Unterstützt den Anwendungsfall "QR-basierter Echtzeit-Check-In".
     */
    suspend fun findSpotByQrCode(qrData: String): ParkingSpot? {
        val query = spotsCollection.whereEqualTo("qrCodeData", qrData).limit(1).get().await()
        return query.documents.firstOrNull()?.toObject<ParkingSpot>()
    }

    /**
     * Startet einen Parkvorgang (Check-In).
     */
    suspend fun checkIn(spotId: String, tenantId: String) {
        spotsCollection.document(spotId).update(
            mapOf(
                "isAvailable" to false,
                "currentTenantId" to tenantId
            )
        ).await()
    }

    /**
     * Holt alle Parkplätze einmalig direkt vom Server.
     * Nutzt Source.SERVER, um den lokalen Cache zu umgehen.
     */
    suspend fun getAllSpotsOnce(): List<ParkingSpot> {
        return try {
            val query = spotsCollection.get(Source.SERVER).await()
            val list = mutableListOf<ParkingSpot>()
            for (doc in query.documents) {
                val spot = doc.toObject(ParkingSpot::class.java)
                if (spot != null) {
                    list.add(spot.copy(id = doc.id))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Echtzeit-Streaming (Flows) ---

    /**
     * Beobachtet einen spezifischen Parkplatz auf Statusänderungen.
     * Unterstützt den Anwendungsfall "Favoriten-Verfügbarkeits-Wächter".
     */
    fun observeSpotStatus(spotId: String): Flow<ParkingSpot?> {
        return spotsCollection.document(spotId).snapshots().map { snapshot ->
            snapshot.toObject<ParkingSpot>()
        }
    }

    /**
     * Beobachtet alle neuen Parkplätze in Echtzeit.
     * Unterstützt den Anwendungsfall "Regionaler Parkplatz-Radar".
     * (Hinweis: Für echtes Geo-Fencing kann dies später mit GeoFirestore erweitert werden).
     */
    fun observeAllSpots(): Flow<List<ParkingSpot>> {
        return spotsCollection.snapshots().map { querySnapshot ->
            querySnapshot.toObjects(ParkingSpot::class.java)
        }
    }

    /**
     * Beobachtet Buchungen für einen bestimmten Vermieter.
     * Unterstützt den Anwendungsfall "Echtzeit-Vermieter-Info".
     */
    fun observeMySpotsBookings(ownerId: String): Flow<List<ParkingSpot>> {
        return spotsCollection
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("isAvailable", false)
            .snapshots()
            .map { it.toObjects(ParkingSpot::class.java) }
    }
}
