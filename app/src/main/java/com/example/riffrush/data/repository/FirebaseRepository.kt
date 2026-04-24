package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.riffrush.data.model.DailyChallengeConfig
import com.google.firebase.firestore.FieldValue
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
                        id = doc.getLong("id") ?: 0L,
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

    suspend fun saveDailyChallengeResult(ownerId: String, isWin: Boolean) {
        try {
            val statsRef = firestore.collection("users")
                .document(ownerId)
                .collection("stats")
                .document("daily_challenge")

            val incrementField = if (isWin) "wins" else "losses"
            val updates = hashMapOf<String, Any>(
                incrementField to com.google.firebase.firestore.FieldValue.increment(1)
            )

            // Usamos set con merge por si el documento no existe
            statsRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getDailyChallengeConfig(date: String): DailyChallengeConfig? {
        return try {
            val doc = firestore.collection("daily_challenges")
                .document(date)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(DailyChallengeConfig::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveDailyChallengeConfig(config: DailyChallengeConfig) {
        try {
            firestore.collection("daily_challenges")
                .document(config.date)
                .set(config)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteDailyChallengeConfig(date: String) {
        try {
            firestore.collection("daily_challenges")
                .document(date)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun incrementGamesPlayed(uid: String) {
        firestore.collection("users").document(uid)
            .update("gamesPlayed", FieldValue.increment(1))
    }

    suspend fun incrementSongsPlayed(uid: String) {
        firestore.collection("users")
            .document(uid)
            .update(
                "songsPlayed",
                com.google.firebase.firestore.FieldValue.increment(1)
            )
            .await()
    }

    suspend fun updateMaxStreak(uid: String, newStreak: Int) {
        val ref = firestore.collection("users").document(uid)
        val doc = ref.get().await()

        val currentMax = doc.getLong("maxDailyStreak") ?: 0

        if (newStreak > currentMax) {
            ref.update("maxDailyStreak", newStreak)
        }
    }
}