package com.memorytag.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.memorytag.app.data.model.Memory
import kotlinx.coroutines.tasks.await

class MemoryRepository {

    companion object { private const val TAG = "MemoryRepo" }

    private val db = FirebaseFirestore.getInstance()
    private val memoriesCollection = db.collection("memories")

    suspend fun fetchMemory(memoryId: String): Memory {
        Log.d(TAG, "fetchMemory($memoryId)")
        if (memoryId == "PARIS_001" || memoryId == "KYOTO_001") {
            return getMockMemory(memoryId)
        }
        return try {
            val doc = memoriesCollection.document(memoryId).get().await()
            Log.d(TAG, "exists=${doc.exists()} data=${doc.data}")
            if (doc.exists()) {
                doc.toObject(Memory::class.java) ?: throw Exception("Deserialisation echouee")
            } else {
                throw Exception("Souvenir introuvable : $memoryId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "ERREUR : ${e.message}", e)
            throw e
        }
    }

    suspend fun createMemory(memory: Memory) {
        memoriesCollection.document(memory.id).set(memory).await()
        Log.d(TAG, "createMemory(${memory.id}) succes")
    }

    private fun getMockMemory(id: String): Memory {
        val mockDb = mapOf(
            "PARIS_001" to Memory(
                id = "PARIS_001", title = "Week-end incroyable",
                location = "Paris, France", latitude = 48.8566, longitude = 2.3522,
                description = "Une escapade inoubliable au coeur de la Ville Lumiere",
                date = "Juin 2024",
                photos = listOf(
                    "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200",
                    "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=1200",
                    "https://images.unsplash.com/photo-1543349689-9a4d426bee8e?w=1200"
                )
            ),
            "KYOTO_001" to Memory(
                id = "KYOTO_001", title = "Sakura Season",
                location = "Kyoto, Japan", latitude = 35.0116, longitude = 135.7681,
                description = "Les cerisiers en fleur", date = "Avril 2024",
                photos = listOf(
                    "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1200",
                    "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=1200"
                )
            )
        )
        return mockDb[id] ?: throw Exception("ID inconnu : $id")
    }
}
