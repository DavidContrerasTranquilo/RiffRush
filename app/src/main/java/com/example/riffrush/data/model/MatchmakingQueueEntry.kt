package com.example.riffrush.data.model

/*
 Representa un ticket o entrada en la cola de matchmaking multijugador online.
 El usuario se registra en la cola para una playlist específica y espera a que el sistema asocie su ticket con otra persona.
 */
data class MatchmakingQueueEntry(
    val queueId: String = "",          // ID único del ticket en la cola
    val playerId: String = "",         // UID del jugador en la cola
    val playerName: String = "",       // Nombre visible del jugador
    val playlist: String = "",         // Playlist solicitada para competir (ej: "REGGAETON")
    val status: String = "WAITING",    // Estado del ticket: WAITING (esperando rival) o MATCHED (emparejado)
    val matchId: String? = null,       // ID de la partida generada tras realizar el emparejamiento con éxito
    val isGuest: Boolean = true,       // Indica si el jugador en cola es un invitado o usuario registrado
    val playerPfpUrl: String = ""      // Foto de perfil del jugador
)