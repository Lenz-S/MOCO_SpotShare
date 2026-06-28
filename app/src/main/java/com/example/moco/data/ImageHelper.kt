package com.example.moco.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * ImageHelper: Zentrale Klasse für die Verarbeitung von Bildern.
 * Übernimmt das Skalieren, Komprimieren und die Base64-Konvertierung
 * für die serverlose Bildspeicherung in Firestore.
 */
class ImageHelper(private val context: Context) {

    /**
     * Wandelt ein Bild (von Kamera oder Galerie) in einen Base64-String um.
     * Das Bild wird dabei auf max. 400 Pixel skaliert und komprimiert,
     * um unter dem 1MB Firestore-Limit zu bleiben.
     */
    suspend fun convertUriToBase64(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
            
            // 1. Skalierung (Maximale Seitenlänge 400px)
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val (targetWidth, targetHeight) = if (ratio > 1) {
                400 to (400 / ratio).toInt()
            } else {
                (400 * ratio).toInt() to 400
            }
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            
            // 2. Kompression (JPEG, 60% Qualität)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()
            
            // 3. Kodierung
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("ImageHelper", "Fehler bei der Konvertierung: ${e.message}")
            null
        }
    }

    /**
     * Wandelt einen Base64-String oder eine URL in ein Modell um,
     * das von der Image-Komponente (Coil) direkt verarbeitet werden kann.
     */
    fun getImageModel(imageSource: String?): Any? {
        if (imageSource.isNullOrBlank()) return null
        
        return if (imageSource.startsWith("http")) {
            // Es ist eine normale Internet-URL
            imageSource
        } else {
            // Es ist ein Base64-Text -> In Byte-Array umwandeln
            try {
                Base64.decode(imageSource, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        }
    }
}
