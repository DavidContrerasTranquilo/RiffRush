package com.example.riffrush.data.model

/*
 Estado de la UI durante el flujo de búsqueda de partida (Matchmaking).
 Permite representar de forma estructurada en la vista si el usuario está inactivo, buscando, esperando o si hubo error.
 */
sealed class MatchmakingUiState {
    object Idle : MatchmakingUiState()                      // Sin ninguna operación activa
    object Searching : MatchmakingUiState()                 // Conectando con el servidor para iniciar la búsqueda
    data class Waiting(val playlist: String) : MatchmakingUiState() // Esperando activamente un rival en la cola de la playlist indicada
    data class Matched(val matchId: String) : MatchmakingUiState()  // Rival encontrado con exito, listos para navegar al juego
    data class Error(val message: String) : MatchmakingUiState()    // Ocurrió un error en el servidor o red
}