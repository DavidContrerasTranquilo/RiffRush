package com.example.riffrush.data.model

/*
 Modelo que representa un reto/desafío directo enviado de un jugador a otro.
 Contiene información de ambos jugadores, la playlist seleccionada, el estado del reto y el ID de la partida asociada.
 Continene la mayoria de datos que ya estan en firebase
 */
data class ChallengeModel(
    val challengeId: String = "",       // ID único identificativo del reto
    val fromUid: String = "",          // UID del jugador que envía el reto
    val fromName: String = "",         // Nombre del jugador que envía el reto
    val fromPfpUrl: String = "",       // URL de la foto de perfil del retador
    val toUid: String = "",            // UID del jugador retado
    val toName: String = "",           // Nombre del jugador retado
    val playlist: String = "",         // Nombre de la playlist elegida para jugar (ej: "REGGAETON")
    val status: String = "PENDING",    // Estado del reto: PENDING (pendiente), ACCEPTED (aceptado), DECLINED (rechazado), EXPIRED (expirado)
    val matchId: String? = null,       // ID de la partida generada una vez aceptado el reto
    val timestamp: Long = System.currentTimeMillis() // Momento en el que se creó el reto
)
