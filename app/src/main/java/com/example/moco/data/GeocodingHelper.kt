package com.example.moco.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * GeocodingHelper: Wandelt Adressen in Koordinaten um.
 * Nutzt die moderne Android Geocoder API mit Coroutine-Unterstützung.
 */
class GeocodingHelper(private val context: Context) {
    suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Moderner asynchroner Ansatz für Android 13+
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (addresses.isNotEmpty()) {
                            val loc = addresses[0]
                            continuation.resume(Pair(loc.latitude, loc.longitude))
                        } else {
                            continuation.resume(null)
                        }
                    }
                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                })
            }
        } else {
            // Fallback für ältere Versionen (blockierend, daher im IO-Thread)
            try {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                if (!results.isNullOrEmpty()) {
                    val loc = results[0]
                    Pair(loc.latitude, loc.longitude)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
