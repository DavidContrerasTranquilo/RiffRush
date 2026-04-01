package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val firestore: FirebaseFirestore) {

    fun getFavorites(ownerId: String): Flow<List<Song>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(ownerId)
            .collection("songs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val songs = snapshot?.documents?.mapNotNull { doc ->
                    Song(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        title = doc.getString("title") ?: "",
                        artist = doc.getString("artist") ?: "",
                        album = doc.getString("album") ?: "",
                        preview = doc.getString("preview") ?: "",
                        cover = doc.getString("cover") ?: ""
                    )
                } ?: emptyList()

                trySend(songs)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addFavorite(ownerId: String, song: Song) {
        try {
            val songData = hashMapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "album" to song.album,
                "preview" to song.preview,
                "cover" to song.cover
            )

            firestore.collection("users")
                .document(ownerId)
                .collection("songs")
                .document(song.id.toString())
                .set(songData)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeFavorite(ownerId: String, songId: String) {
        try {
            firestore.collection("users")
                .document(ownerId)
                .collection("songs")
                .document(songId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}