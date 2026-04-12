package com.memorytag.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorytag.app.data.model.CreateMemoryUiState
import com.memorytag.app.data.model.Memory
import com.memorytag.app.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CreateMemoryViewModel : ViewModel() {

    private val repository = MemoryRepository()

    private val _uiState = MutableStateFlow<CreateMemoryUiState>(CreateMemoryUiState.Idle)
    val uiState: StateFlow<CreateMemoryUiState> = _uiState.asStateFlow()

    /**
     * Crée un souvenir avec les champs obligatoires + optionnels.
     *
     * @param title       Obligatoire
     * @param location    Obligatoire
     * @param description Optionnel
     * @param date        Optionnel  ex: "Juin 2024"
     * @param latitude    Optionnel  ex: "48.8566"
     * @param longitude   Optionnel  ex: "2.3522"
     * @param photosRaw   Optionnel  URLs séparées par des virgules ou retours à la ligne
     */
    fun createMemory(
        title: String,
        location: String,
        description: String = "",
        date: String = "",
        latitude: String = "",
        longitude: String = "",
        photosRaw: String = ""
    ) {
        // ── Validation des champs obligatoires ──────────────────────────────
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
                val id = UUID.randomUUID().toString()

                // ── Parse latitude / longitude ──────────────────────────────
                val lat = latitude.trim().toDoubleOrNull() ?: 0.0
                val lon = longitude.trim().toDoubleOrNull() ?: 0.0

                // ── Parse photos : séparer par virgule ou newline ───────────
                val photos = if (photosRaw.isBlank()) {
                    emptyList()
                } else {
                    photosRaw.split(",", "\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
                }

                val memory = Memory(
                    id          = id,
                    title       = title.trim(),
                    location    = location.trim(),
                    description = description.trim(),
                    date        = date.trim(),
                    latitude    = lat,
                    longitude   = lon,
                    photos      = photos
                )

                repository.createMemory(memory)
                _uiState.value = CreateMemoryUiState.Success(id)

            } catch (e: Exception) {
                _uiState.value = CreateMemoryUiState.Error(
                    e.message ?: "Erreur lors de la création"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateMemoryUiState.Idle
    }
}
