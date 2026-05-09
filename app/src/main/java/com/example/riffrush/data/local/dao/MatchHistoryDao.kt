package com.example.riffrush.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.riffrush.data.local.entity.MatchHistoryEntity
import kotlinx.coroutines.flow.Flow

/*
 DAO para gestionar las operaciones de lectura y escritura del historial de partidas
 en la base de datos local Room. Permite acceso instantáneo al historial sin conexión.
 */
@Dao
interface MatchHistoryDao {

    // Obtiene el historial de partidas de un usuario, ordenado por las más recientes primero
    @Query("SELECT * FROM match_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getHistoryByUser(userId: String): Flow<List<MatchHistoryEntity>>

    // Obtiene el historial de forma síncrona (suspend) para carga puntual
    @Query("SELECT * FROM match_history WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getHistoryByUserOnce(userId: String): List<MatchHistoryEntity>

    // Inserta o actualiza una lista completa de registros de historial (sincronización desde Firestore)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<MatchHistoryEntity>)

    // Inserta o actualiza un registro individual
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MatchHistoryEntity)

    // Borra todo el historial local de un usuario (limpieza de caché o logout)
    @Query("DELETE FROM match_history WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}
