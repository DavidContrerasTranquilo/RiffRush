package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val firestore: FirebaseFirestore
) {

    suspend fun getUserSongs(userId: String): List<Song> {
        val result = mutableListOf<Song>()

        try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("songs")
                .get()
                .await()

            for (doc in snapshot.documents) {
                val song = Song(
                    title = doc.getString("title") ?: "",
                    artist = doc.getString("artist") ?: "",
                    previewUrl = doc.getString("previewUrl") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: ""
                )
                result.add(song)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    suspend fun saveSong(userId: String, song: Song) {
        try {
            firestore
                .collection("users")
                .document(userId)
                .collection("songs")
                .add(song)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSong(userId: String, previewUrl: String) {
        try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("songs")
                .whereEqualTo("previewUrl", previewUrl)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}