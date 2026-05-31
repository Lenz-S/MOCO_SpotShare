package com.example.moco.viewmodel

import androidx.lifecycle.ViewModel
import com.example.moco.model.ParkingSpot
import com.mapbox.common.MapboxOptions
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.ResponseInfo
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ParkingViewModel : ViewModel() {
    private val _parkingSpots = MutableStateFlow<List<ParkingSpot>>(emptyList())
    val parkingSpots: StateFlow<List<ParkingSpot>> = _parkingSpots.asStateFlow()

    private var searchEngine: SearchEngine? = null

    fun initSearchEngine(accessToken: String) {
        if (searchEngine == null) {
            MapboxOptions.accessToken = accessToken
            searchEngine = SearchEngine.createSearchEngine(SearchEngineSettings())
        }
    }

    fun addSpotWithAddress(title: String, description: String, address: String, pricePerHour: Double) {
        val engine = searchEngine
        if (engine == null) {
            addSpot(title, description, address, 52.5200, 13.4050, pricePerHour)
            return
        }

        engine.search(address, SearchOptions(), object : SearchSuggestionsCallback {
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                if (suggestions.isNotEmpty()) {
                    engine.select(
                        suggestions.first(),
                        object : SearchSelectionCallback {
                            override fun onResult(
                                suggestion: SearchSuggestion,
                                result: SearchResult,
                                responseInfo: ResponseInfo
                            ) {
                                val coord = result.coordinate
                                addSpot(title, description, address, coord.latitude(), coord.longitude(), pricePerHour)
                            }

                            override fun onResults(
                                suggestion: SearchSuggestion,
                                results: List<SearchResult>,
                                responseInfo: ResponseInfo
                            ) {
                                // Für Kategorien (nicht benötigt)
                            }

                            override fun onError(e: Exception) {
                                addSpot(title, description, address, 52.5200, 13.4050, pricePerHour)
                            }

                            override fun onSuggestions(
                                suggestions: List<SearchSuggestion>,
                                responseInfo: ResponseInfo
                            ) {
                                // Bereits oben behandelt
                            }
                        }
                    )
                } else {
                    addSpot(title, description, address, 52.5200, 13.4050, pricePerHour)
                }
            }

            override fun onError(e: Exception) {
                addSpot(title, description, address, 52.5200, 13.4050, pricePerHour)
            }
        })
    }

    private fun addSpot(title: String, description: String, address: String, lat: Double, lon: Double, price: Double) {
        val newSpot = ParkingSpot(
            title = title,
            description = description,
            address = address,
            pricePerHour = price,
            latitude = lat,
            longitude = lon
        )
        _parkingSpots.update { it + newSpot }
    }
}


