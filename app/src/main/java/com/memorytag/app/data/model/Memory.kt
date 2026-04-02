package com.memorytag.app.data.model

/**
 * Modèle principal d'un souvenir.
 * Correspond exactement à la réponse JSON de l'API.
 */
data class Memory(
    val id: String,
    val title: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val photos: List<String>,
    val description: String? = null,
    val date: String? = null
)

/**
 * États possibles du chargement — utilisés dans le ViewModel.
 */
sealed class MemoryUiState {
    object Idle : MemoryUiState()          // En attente d'un scan
    object Loading : MemoryUiState()       // Chargement API en cours
    data class Success(val memory: Memory) : MemoryUiState()
    data class Error(val message: String) : MemoryUiState()
}
