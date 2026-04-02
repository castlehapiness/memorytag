package com.memorytag.app.data.repository

import com.memorytag.app.data.model.Memory
import kotlinx.coroutines.delay
import android.util.Log
/**
 * Repository des souvenirs.
 * Abstrait la source de données (API réelle ou mock).
 * En production : remplacer fetchMemory() par un appel Retrofit.
 */
class MemoryRepository {

    /**
     * Récupère un souvenir par son ID (lu depuis le tag NFC).
     * Simule un délai réseau de 800ms pour le mock.
     */
    suspend fun fetchMemory(memoryId: String): Memory {
        delay(800) // Simule la latence réseau

        val normalizedId = memoryId.trim().uppercase()
        Log.d("MEMORY_DEBUG", "memoryId brut='$memoryId' | normalisé='$normalizedId'")


        // --- MOCK DATA ---
        // En production, remplacer par : apiService.getMemory(memoryId)
        return getMockMemory(normalizedId)
    }

    /**
     * Données de démonstration.
     * Utilise des images Unsplash libres de droits.
     */
    private fun getMockMemory(id: String): Memory {
        val mockDatabase = mapOf(
            "PARIS_001" to Memory(
                id = "PARIS_001",
                title = "Week-end incroyable",
                location = "Paris, France",
                latitude = 48.8566,
                longitude = 2.3522,
                description = "Une escapade inoubliable au cœur de la Ville Lumière",
                date = "Juin 2024",
                photos = listOf(
                    "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200",
                    "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=1200",
                    "https://images.unsplash.com/photo-1543349689-9a4d426bee8e?w=1200",
                    "https://images.unsplash.com/photo-1471874708680-a9f1ba5a6abe?w=1200",
                    "https://images.unsplash.com/photo-1522093007474-d86e9bf7ba6f?w=1200"
                )
            ),
            "KYOTO_001" to Memory(
                id = "KYOTO_001",
                title = "Sakura Season",
                location = "Kyoto, Japan",
                latitude = 35.0116,
                longitude = 135.7681,
                description = "Les cerisiers en fleur, un moment suspendu dans le temps",
                date = "Avril 2024",
                photos = listOf(
                    "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1200",
                    "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=1200",
                    "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=1200"
                )
            )
        )

        // Retourne le mock correspondant, ou une erreur
        return mockDatabase[id]
            ?: throw IllegalArgumentException("Aucun souvenir trouvé pour l'id: '$id'")    }
}
