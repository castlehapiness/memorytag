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

/**
 * ViewModel dédié à la création d'un souvenir.
 *
 * Responsabilités :
 *  - Valider les champs saisis par l'utilisateur
 *  - Générer l'UUID unique
 *  - Appeler MemoryRepository.createMemory()
 *  - Exposer l'état de l'opération via StateFlow
 */
class CreateMemoryViewModel : ViewModel() {

    private val repository = MemoryRepository()

    private val _uiState = MutableStateFlow<CreateMemoryUiState>(CreateMemoryUiState.Idle)
    val uiState: StateFlow<CreateMemoryUiState> = _uiState.asStateFlow()

    /**
     * Point d'entrée principal : valide les champs et crée le souvenir.
     *
     * @param title       Titre saisi (obligatoire)
     * @param location    Lieu saisi (obligatoire)
     * @param description Description optionnelle
     */
    fun createMemory(title: String, location: String, description: String) {

        // ── Validation ───────────────────────────────────────────────────────
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
                // ── Génération de l'ID unique ─────────────────────────────────
                // UUID v4 — format : "550e8400-e29b-41d4-a716-446655440000"
                // Cet ID sera aussi à encoder sur le tag NFC
                val id = UUID.randomUUID().toString()

                // ── Construction de l'objet Memory ────────────────────────────
                val memory = Memory(
                    id          = id,
                    title       = title.trim(),
                    location    = location.trim(),
                    description = description.trim(),
                    latitude    = 0.0,          // À enrichir plus tard (GPS)
                    longitude   = 0.0,
                    date        = "",            // À enrichir plus tard
                    photos      = emptyList()    // Upload photo = feature future
                )

                // ── Écriture Firestore ────────────────────────────────────────
                repository.createMemory(memory)

                // Succès — retourne l'ID pour affichage dans l'UI
                _uiState.value = CreateMemoryUiState.Success(id)

            } catch (e: Exception) {
                _uiState.value = CreateMemoryUiState.Error(
                    e.message ?: "Erreur lors de la création"
                )
            }
        }
    }

    /** Remet l'état à Idle (ex: après snackbar succès) */
    fun resetState() {
        _uiState.value = CreateMemoryUiState.Idle
    }
}
