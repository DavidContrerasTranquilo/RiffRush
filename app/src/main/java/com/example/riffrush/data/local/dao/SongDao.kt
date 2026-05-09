package com.example.riffrush.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.riffrush.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/*
 DAO (Data Access Object) para gestionar las operaciones de lectura y escritura
 de canciones favoritas en la base de datos local Room.
 Todas las consultas de lectura retornan Flow para observabilidad reactiva en Compose.
 */
@Dao
interface SongDao {

    // Obtiene todas las canciones favoritas de un usuario específico, ordenadas por fecha de adición (más recientes primero)
    @Query("SELECT * FROM favorite_songs WHERE ownerId = :userId ORDER BY addedAt DESC")
    fun getFavoritesByUser(userId: String): Flow<List<SongEntity>>

    // Inserta o reemplaza una lista completa de canciones favoritas (útil para sincronización desde Firestore)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    // Inserta o reemplaza una canción individual en los favoritos locales
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    // Elimina una canción específica de los favoritos de un usuario
    @Query("DELETE FROM favorite_songs WHERE id = :songId AND ownerId = :userId")
    suspend fun delete(songId: Long, userId: String)

    // Borra todos los favoritos de un usuario (útil para cerrar sesión o limpiar caché)
    @Query("DELETE FROM favorite_songs WHERE ownerId = :userId")
    suspend fun clearAll(userId: String)
}
