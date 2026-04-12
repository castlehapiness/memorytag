package com.memorytag.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorytag.app.data.model.CreateMemoryUiState
import com.memorytag.app.data.model.Memory
import com.memorytag.app.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

class CreateMemoryViewModel : ViewModel() {

    companion object {
        private const val TAG = "CreateMemoryVM"
    }

    private val repository = MemoryRepository()

    private val _uiState = MutableStateFlow<CreateMemoryUiState>(CreateMemoryUiState.Idle)
    val uiState: StateFlow<CreateMemoryUiState> = _uiState.asStateFlow()

    private val _coords = MutableStateFlow<Pair<Double, Double>?>(null)
    val coords: StateFlow<Pair<Double, Double>?> = _coords.asStateFlow()

    fun createMemory(
        title: String,
        location: String,
        description: String = "",
        date: String = "",
        photosRaw: String = ""
    ) {
        if (title.isBlank()) {
            _uiState.value = CreateMemoryUiState.Error("Le titre est obligatoire")
            return
        }
        if (location.isBlank()) {
            _uiState.value = CreateMemoryUiState.Error("La localisation est obligatoire")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateMemoryUiState.Loading

            try {
                // Géocodage sur IO thread — IMPORTANT
                val (lat, lon) = withContext(Dispatchers.IO) {
                    geocode(location)
                }
                Log.d(TAG, "Coordonnées : $lat / $lon")

                val photos = photosRaw
                    .split(",", "\n")
                    .map { it.trim() }
                    .filter { it.startsWith("http") }

                val memory = Memory(
                    id          = UUID.randomUUID().toString(),
                    title       = title.trim(),
                    location    = location.trim(),
                    description = description.trim(),
                    date        = date.trim(),
                    latitude    = lat,
                    longitude   = lon,
                    photos      = photos
                )

                repository.createMemory(memory)
                _uiState.value = CreateMemoryUiState.Success(memory.id)

            } catch (e: Exception) {
                Log.e(TAG, "Erreur création : ${e.message}", e)
                _uiState.value = CreateMemoryUiState.Error(
                    e.message ?: "Erreur lors de la création"
                )
            }
        }
    }

    private fun geocode(location: String): Pair<Double, Double> {
        return try {
            val query = URLEncoder.encode(location.trim(), "UTF-8")
            val urlStr = "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1"
            Log.d(TAG, "Geocoding : $urlStr")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "MemoryTag-Android/1.0 contact@memorytag.app")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout    = 8000
            connection.connect()

            val responseCode = connection.responseCode
            Log.d(TAG, "Nominatim response code : $responseCode")

            if (responseCode != 200) {
                Log.w(TAG, "Nominatim erreur HTTP $responseCode")
                connection.disconnect()
                return Pair(0.0, 0.0)
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            Log.d(TAG, "Nominatim response : $response")

            val json = JSONArray(response)
            if (json.length() == 0) {
                Log.w(TAG, "Nominatim : aucun résultat pour '$location'")
                return Pair(0.0, 0.0)
            }

            val first = json.getJSONObject(0)
            val lat = first.getString("lat").toDouble()
            val lon = first.getString("lon").toDouble()
            Log.d(TAG, "Geocodé : $lat / $lon")

            _coords.value = Pair(lat, lon)
            Pair(lat, lon)

        } catch (e: Exception) {
            Log.e(TAG, "Geocoding échoué : ${e.message}", e)
            Pair(0.0, 0.0)
        }
    }

    fun resetState() {
        _uiState.value = CreateMemoryUiState.Idle
        _coords.value = null
    }
}