package com.memorytag.app.data.model

/**
 * Modèle principal d'un souvenir.
 *
 * IMPORTANT : les valeurs par défaut sont requises pour la désérialisation
 * Firestore — Firestore instancie la data class via réflexion et a besoin
 * d'un constructeur sans argument.
 */
data class Memory(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photos: List<String> = emptyList(),
    val description: String = "",
    val date: String = ""
)

/**
 * États possibles du chargement — partagés entre les ViewModels.
 */
sealed class MemoryUiState {
    object Idle    : MemoryUiState()
    object Loading : MemoryUiState()
    data class Success(val memory: Memory) : MemoryUiState()
    data class Error(val message: String)  : MemoryUiState()
}

/**
 * États spécifiques à la création d'un souvenir.
 */
sealed class CreateMemoryUiState {
    object Idle    : CreateMemoryUiState()
    object Loading : CreateMemoryUiState()
    // Retourne l'ID Firestore du souvenir créé
    data class Success(val memoryId: String) : CreateMemoryUiState()
    data class Error(val message: String)    : CreateMemoryUiState()
}
