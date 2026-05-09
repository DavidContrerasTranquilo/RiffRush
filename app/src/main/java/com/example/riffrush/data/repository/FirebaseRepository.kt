package com.example.riffrush.data.repository

import com.example.riffrush.data.model.DailyChallengeConfig
import com.example.riffrush.data.model.MatchHistoryModel
import com.example.riffrush.data.model.PlaylistModel
import com.example.riffrush.data.model.Song
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

/*
 Repositorio para operaciones persistentes en Cloud Firestore.
 Gestiona colecciones de canciones favoritas de usuarios, historiales de partidas, estadísticas globales de juego,
 configuraciones diarias del "Daily Challenge" y playlists personalizadas del usuario mediante flujos reactivos (Flows) y transacciones.
 */
class FirebaseRepository(private val firestore: FirebaseFirestore) {

    // Obtiene un flujo reactivo (Flow) en tiempo real de las canciones favoritas agregadas por un usuario específico
    fun getFavorites(ownerId: String): Flow<List<Song>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(ownerId)
            .collection("songs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Mapea la lista de documentos de Firestore al modelo local de canciones
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

                trySend(songs) // Emite la nueva lista a los colectores de Flow
            }
        // Desregistra el SnapshotListener de Firestore de manera segura al cancelarse el Flow (evita fugas de memoria)
        awaitClose { subscription.remove() }
    }

    // Guarda una canción favorita en la subcolección del usuario
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
            Log.e("FirebaseRepository", "Error añadiendo favorito", e)
        }
    }

    // Elimina una canción de la lista de favoritos del usuario
    suspend fun removeFavorite(ownerId: String, songId: String) {
        try {
            firestore.collection("users")
                .document(ownerId)
                .collection("songs")
                .document(songId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error eliminando favorito", e)
        }
    }

    // Incrementa el contador de victorias/derrotas del Reto Diario utilizando operadores atómicos de incremento de Firestore
    suspend fun saveDailyChallengeResult(ownerId: String, isWin: Boolean) {
        try {
            val statsRef = firestore.collection("users")
                .document(ownerId)
                .collection("stats")
                .document("daily_challenge")

            val incrementField = if (isWin) "wins" else "losses"
            val updates = hashMapOf<String, Any>(
                incrementField to FieldValue.increment(1)
            )

            // Usamos set con merge por si el documento no existe
            statsRef.set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error guardando resultado del reto diario", e)
        }
    }

    // Recupera la configuración de canciones de un Reto Diario para una fecha específica
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
            Log.e("FirebaseRepository", "Error obteniendo configuración del reto diario", e)
            null
        }
    }

    // Agrega o actualiza un reto diario en la colección global (administración)
    suspend fun saveDailyChallengeConfig(config: DailyChallengeConfig) {
        try {
            firestore.collection("daily_challenges")
                .document(config.date)
                .set(config)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error guardando configuración del reto diario", e)
        }
    }

    // Elimina un reto diario de la colección global (administración)
    suspend fun deleteDailyChallengeConfig(date: String) {
        try {
            firestore.collection("daily_challenges")
                .document(date)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error eliminando configuración del reto diario", e)
        }
    }

    // Incrementa de manera atómica la cantidad de partidas globales completadas por el usuario
    suspend fun incrementGamesPlayed(uid: String) {
        if (uid.isBlank()) return

        firestore.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "gamesPlayed" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )
            .await()
    }

    // Incrementa la cantidad de canciones individuales que el usuario ha respondido en total
    suspend fun incrementSongsPlayed(uid: String) {
        if (uid.isBlank()) return

        firestore.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "uid" to uid,
                    "songsPlayed" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )
            .await()
    }

    // Actualiza la racha máxima de días consecutivos completando el reto diario si la actual supera a la almacenada
    suspend fun updateMaxStreak(uid: String, newStreak: Int) {
        val ref = firestore.collection("users").document(uid)
        val doc = ref.get().await()

        val currentMax = doc.getLong("maxDailyStreak") ?: 0

        if (newStreak > currentMax) {
            ref.update("maxDailyStreak", newStreak).await()
        }
    }

    // Almacena un registro de partida jugada en el historial individual del usuario
    suspend fun saveMatchHistory(history: MatchHistoryModel) {
        val docRef = firestore.collection("users")
            .document(history.userId)
            .collection("match_history")
            .document()

        val historyWithId = history.copy(historyId = docRef.id)

        docRef.set(historyWithId).await()
    }

    // Descarga todo el historial de partidas del usuario ordenado por fecha de forma descendente (más reciente primero)
    suspend fun getMatchHistory(userId: String): List<MatchHistoryModel> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("match_history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull {
                it.toObject(MatchHistoryModel::class.java)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error obteniendo historial de partidas", e)
            emptyList()
        }
    }

    // Obtiene un flujo en tiempo real de todas las playlists creadas por el usuario
    fun getPlaylists(ownerId: String): Flow<List<PlaylistModel>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(ownerId)
            .collection("playlists")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val playlists = snapshot?.documents?.mapNotNull { doc ->
                    PlaylistModel(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        songsCount = (doc.getLong("songsCount") ?: 0L).toInt()
                    )
                } ?: emptyList()

                trySend(playlists)
            }

        awaitClose { subscription.remove() }
    }

    // Crea una nueva playlist personalizada vacía en Firestore
    suspend fun createPlaylist(ownerId: String, name: String) {
        val ref = firestore.collection("users")
            .document(ownerId)
            .collection("playlists")
            .document()

        val playlist = PlaylistModel(
            id = ref.id,
            name = name,
            createdAt = System.currentTimeMillis(),
            songsCount = 0
        )

        ref.set(playlist).await()
    }

    // Elimina una playlist de la colección del usuario
    suspend fun deletePlaylist(ownerId: String, playlistId: String) {
        firestore.collection("users")
            .document(ownerId)
            .collection("playlists")
            .document(playlistId)
            .delete()
            .await()
    }

    // Obtiene un flujo de tiempo real de todas las canciones contenidas dentro de una playlist específica
    fun getPlaylistSongs(ownerId: String, playlistId: String): Flow<List<Song>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(ownerId)
            .collection("playlists")
            .document(playlistId)
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

    // Agrega una canción a una playlist y actualiza el contador de canciones de forma segura usando una Transacción atómica
    suspend fun addSongToPlaylist(ownerId: String, playlistId: String, song: Song) {
        val playlistRef = firestore.collection("users")
            .document(ownerId)
            .collection("playlists")
            .document(playlistId)

        val songRef = playlistRef
            .collection("songs")
            .document(song.id.toString())

        firestore.runTransaction { transaction ->
            val existingSong = transaction.get(songRef)

            val songData = hashMapOf(
                "id" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "album" to song.album,
                "preview" to song.preview,
                "cover" to song.cover
            )

            transaction.set(songRef, songData)

            // Si la canción no estaba agregada previamente en esta playlist, incrementa el contador
            if (!existingSong.exists()) {
                transaction.update(
                    playlistRef,
                    "songsCount",
                    FieldValue.increment(1)
                )
            }
        }.await()
    }

    // Elimina una canción de una playlist y decrementa el contador de canciones de forma segura usando una Transacción atómica
    suspend fun removeSongFromPlaylist(ownerId: String, playlistId: String, songId: String) {
        val playlistRef = firestore.collection("users")
            .document(ownerId)
            .collection("playlists")
            .document(playlistId)

        val songRef = playlistRef
            .collection("songs")
            .document(songId)

        firestore.runTransaction { transaction ->
            val playlistSnapshot = transaction.get(playlistRef)
            val songSnapshot = transaction.get(songRef)

            // Si la canción existe, disminuye el contador evitando números negativos y la remueve
            if (songSnapshot.exists()) {
                val currentCount = playlistSnapshot.getLong("songsCount") ?: 0L
                val newCount = (currentCount - 1).coerceAtLeast(0L)

                transaction.delete(songRef)
                transaction.update(playlistRef, "songsCount", newCount)
            }
        }.await()
    }
}