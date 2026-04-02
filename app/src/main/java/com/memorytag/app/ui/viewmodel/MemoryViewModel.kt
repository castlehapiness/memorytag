package com.memorytag.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memorytag.app.data.model.MemoryUiState
import com.memorytag.app.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel MVVM pour la récupération des souvenirs.
 * Survit aux rotations d'écran.
 * Expose un StateFlow immuable à l'Activity.
 */
class MemoryViewModel : ViewModel() {

    private val repository = MemoryRepository()

    // État interne mutable (privé)
    private val _uiState = MutableStateFlow<MemoryUiState>(MemoryUiState.Idle)

    // État exposé en lecture seule à l'UI
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    /**
     * Charge un souvenir depuis son ID NFC.
     * Gère automatiquement les états Loading / Success / Error.
     */
    fun loadMemory(memoryId: String) {
        viewModelScope.launch {
            _uiState.value = MemoryUiState.Loading

            try {
                val memory = repository.fetchMemory(memoryId)
                _uiState.value = MemoryUiState.Success(memory)
            } catch (e: Exception) {
                _uiState.value = MemoryUiState.Error(
                    e.message ?: "Impossible de charger ce souvenir"
                )
            }
        }
    }

    /** Remet l'état à Idle (ex: après fermeture du détail) */
    fun resetState() {
        _uiState.value = MemoryUiState.Idle
    }
}
