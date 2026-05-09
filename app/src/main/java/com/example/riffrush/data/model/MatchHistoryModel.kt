package com.example.riffrush.data.model

/*
 Historial individual de una partida completada por el usuario.
 Registra los resultados finales de cada duelo para que puedan ser mostrados en la pantalla de historial del perfil.
 */
data class MatchHistoryModel(
    val historyId: String = "",                    // ID único del registro del historial
    val matchId: String = "",                      // ID de la partida multijugador completada
    val userId: String = "",                       // UID del usuario dueño de este historial
    val opponentId: String = "",                   // UID del oponente (o bot/offline)
    val opponentName: String = "",                 // Nombre visible del oponente
    val mode: String = "",                         // Modo de juego: ONLINE, OFFLINE, FAVORITES
    val playlist: String = "",                     // Categoría o Playlist jugada (ej: "ROCK ESPAÑOL")
    val myScore: Int = 0,                          // Puntuación obtenida por el usuario
    val opponentScore: Int = 0,                    // Puntuación obtenida por el oponente
    val result: String = "",                       // Resultado de la partida desde el punto de vista del usuario: WIN (ganó), LOSE (perdió), DRAW (empate)
    val timestamp: Long = System.currentTimeMillis() // Fecha y hora en milisegundos de la partida
)