package com.example.riffrush.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.riffrush.data.model.MatchHistoryModel

/*
 Entidad Room que representa un registro del historial de partidas almacenado localmente.
 Permite cargar el historial de forma instantánea desde SQLite sin esperar a Firestore,
 sincronizando los datos con la nube en segundo plano cuando hay conexión disponible.
 */
@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey val historyId: String,      // ID único del registro del historial
    val matchId: String,                    // ID de la partida multijugador
    val userId: String,                     // UID del usuario dueño de este historial
    val opponentId: String,                 // UID del oponente
    val opponentName: String,               // Nombre visible del oponente
    val mode: String,                       // Modo de juego: ONLINE, OFFLINE, FAVORITES
    val playlist: String,                   // Categoría o Playlist jugada
    val myScore: Int,                       // Puntuación obtenida por el usuario
    val opponentScore: Int,                 // Puntuación del oponente
    val result: String,                     // Resultado: WIN, LOSE, DRAW
    val timestamp: Long                     // Marca temporal en milisegundos
)

// Convierte un modelo de dominio MatchHistoryModel en una entidad Room almacenable
fun MatchHistoryModel.toEntity() = MatchHistoryEntity(
    historyId = historyId,
    matchId = matchId,
    userId = userId,
    opponentId = opponentId,
    opponentName = opponentName,
    mode = mode,
    playlist = playlist,
    myScore = myScore,
    opponentScore = opponentScore,
    result = result,
    timestamp = timestamp
)

// Convierte una entidad Room en un modelo de dominio MatchHistoryModel
fun MatchHistoryEntity.toModel() = MatchHistoryModel(
    historyId = historyId,
    matchId = matchId,
    userId = userId,
    opponentId = opponentId,
    opponentName = opponentName,
    mode = mode,
    playlist = playlist,
    myScore = myScore,
    opponentScore = opponentScore,
    result = result,
    timestamp = timestamp
)
