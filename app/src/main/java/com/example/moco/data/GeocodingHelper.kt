package com.example.moco.data

import android.content.Context
import com.example.moco.R
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helper-Klasse für das Geocoding (Adresse -> Koordinaten).
 * Nutzt das Mapbox Java SDK (Geocoding API v5), wie in der offiziellen Dokumentation empfohlen.
 */
class GeocodingHelper(private val context: Context) {

    /**
     * Wandelt eine Adresse in Koordinaten um.
     * Nutzt Coroutines (suspend), um das Ergebnis asynchron zurückzugeben.
     * 
     * @param address Die vom Nutzer eingegebene Adresse.
     * @return Ein Pair aus Latitude und Longitude oder null bei Fehler.
     */
    suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        val mapboxGeocoding = MapboxGeocoding.builder()
            .accessToken(context.getString(R.string.mapbox_access_token))
            .query(address)
            .limit(1)
            .build()

        mapboxGeocoding.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                val results = response.body()?.features()
                if (!results.isNullOrEmpty()) {
                    val point = results[0].center()
                    if (point != null) {
                        // Mapbox liefert [longitude, latitude]
                        continuation.resume(Pair(point.latitude(), point.longitude()))
                    } else {
                        continuation.resume(null)
                    }
                } else {
                    continuation.resume(null)
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        })

        continuation.invokeOnCancellation {
            // Falls die Coroutine abgebrochen wird, brechen wir auch den Netzwerk-Call ab
            mapboxGeocoding.cancelCall()
        }

    }
}

