package com.memorytag.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.memorytag.app.data.model.Memory
import android.util.Log
import kotlinx.coroutines.tasks.await


/**
 * Repository unique pour toutes les opérations sur les souvenirs.
 *
 * Stratégie de lecture :
 *   1. Cherche dans Firestore (collection "memories", document = memoryId)
 *   2. Si le document n'existe pas → retombe sur le mock local
 *
 * Ainsi le mock Paris/Kyoto continue de fonctionner pendant le dev,
 * et les vrais souvenirs créés via CreateMemoryActivity sont aussi lisibles.
 */
class MemoryRepository {

    // Instance Firestore — singleton géré par le SDK Firebase
    private val db = FirebaseFirestore.getInstance()

    // Référence à la collection "memories" dans Firestore
    private val memoriesCollection = db.collection("memories")

    // ── READ ─────────────────────────────────────────────────────────────────

    /**
     * Charge un souvenir par son ID.
     * Cherche d'abord dans Firestore, puis dans le mock local en fallback.
     *
     * @param memoryId ID du document Firestore (ex: UUID ou "PARIS_001")
     * @throws Exception si Firestore échoue ET que le mock ne contient pas l'ID
     */
    suspend fun fetchMemory(memoryId: String): Memory {
        return try {
            val doc = memoriesCollection.document(memoryId).get().await()

            if (doc.exists()) {
                // Désérialisation automatique Firestore → Memory
                // Requiert les valeurs par défaut dans la data class
                doc.toObject(Memory::class.java)
                    ?: throw Exception("Erreur de désérialisation")
            } else {
                // Document absent → fallback mock (utile en dev)
                getMockMemory(memoryId)
            }
        } catch (e: Exception) {
            // Si Firestore est inaccessible (pas de réseau), fallback mock
            getMockMemory(memoryId)
        }
    }

    // ── WRITE ────────────────────────────────────────────────────────────────

    /**
     * Crée un nouveau souvenir dans Firestore.
     *
     * Le document est créé avec l'ID défini dans memory.id.
     * Utilisation : CoroutineScope (viewModelScope) via suspend + await().
     *
     * Structure Firestore créée :
     * memories/
     *   {memory.id}/
     *     id: "..."
     *     title: "..."
     *     location: "..."
     *     latitude: 0.0
     *     longitude: 0.0
     *     description: "..."
     *     date: ""
     *     photos: []
     *
     * @param memory L'objet Memory à persister
     * @throws Exception si l'écriture Firestore échoue
     */
    suspend fun createMemory(memory: Memory) {
        memoriesCollection
            .document(memory.id)  // ID explicite = l'UUID généré dans le ViewModel
            .set(memory)          // set() crée ou écrase le document
            .await()              // Suspend jusqu'à la confirmation Firestore
    }

    // ── MOCK LOCAL ───────────────────────────────────────────────────────────

    /**
     * Données de démonstration — reste actif tant que Firestore n'a pas
     * ces IDs, et sert de fallback hors-ligne.
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
        return mockDatabase[id] ?: throw Exception("Souvenir introuvable : $id")
    }
}
