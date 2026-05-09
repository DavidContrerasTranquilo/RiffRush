package com.example.riffrush.data.model

/*
 Modelo central de una Partida (Match).
 Almacena toda la lógica sincronizada en tiempo real (Firebase RTDB) entre dos jugadores, incluyendo
 puntuaciones, rondas actuales, estado de respuesta de cada jugador, carátulas/canciones e inactividad (pings).
 */
data class MatchModel(
    val matchId: String = "",                         // ID único de la partida en Firebase
    val player1Id: String = "",                       // ID del primer jugador (Host)
    val player2Id: String = "",                       // ID del segundo jugador (Invitado/Bot)
    val player1Name: String = "",                     // Nombre visible del primer jugador
    val player2Name: String = "",                     // Nombre visible del segundo jugador
    val playlist: String = "",                         // Categoría de música cargada (ej: "REGGAETON")
    val status: MatchStatus = MatchStatus.WAITING,    // Estado general de la partida (WAITING, PLAYING, FINISHED)
    val staticSongList: List<Long> = emptyList(),     // Lista fija de IDs de Deezer elegidos dinámicamente al inicio para esta partida
    val currentRound: Int = 0,                        // Índice de la ronda actual (0 a 4 para 5 canciones)
    val scoreP1: Int = 0,                             // Puntuación actual del jugador 1
    val scoreP2: Int = 0,                             // Puntuación actual del jugador 2
    val answeredP1: Boolean = false,                  // Indica si el jugador 1 ya respondió a la ronda actual
    val answeredP2: Boolean = false,                  // Indica si el jugador 2 ya respondió a la ronda actual
    val player1PfpUrl: String = "",                   // Foto de perfil del jugador 1
    val player2PfpUrl: String = "",                   // Foto de perfil del jugador 2
    val roundResults: List<RoundResultModel> = emptyList(), // Historial detallado ronda por ronda de quién acertó cada canción
    val abandonedBy: String = "",                      // ID del jugador que abandonó/se rindió de la partida (si aplica)
    val winnerId: String = "",                         // ID del ganador absoluto una vez terminada la partida
    val endReason: String = "",                        // Motivo de finalización (ej: "COMPLETED", "SURRENDER", "DISCONNECTED")
    val player1LastSeen: Long = 0L,                   // Timestamp del último ping enviado por el jugador 1 (evita congelamiento si se desconecta)
    val player2LastSeen: Long = 0L                    // Timestamp del último ping enviado por el jugador 2 (evita congelamiento si se desconecta)
)

/*
 Estados posibles por los que atraviesa una partida activa en el flujo del juego.
 */
enum class MatchStatus {
    WAITING,   // Esperando que se una el segundo jugador (en Matchmaking) o cargando recursos
    PLAYING,   // Partida en curso, rondas de adivinación activas
    FINISHED   // Partida finalizada con éxito, desconexión o rendición
}