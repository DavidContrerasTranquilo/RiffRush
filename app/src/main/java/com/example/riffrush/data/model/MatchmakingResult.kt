package com.example.riffrush.data.model

/*
 Resultado de la operación de emparejamiento (Matchmaking) asíncrona.
 Modela los dos estados posibles tras realizar la solicitud a la base de datos de Realtime Database.
 */
sealed class MatchmakingResult {
    // El jugador fue puesto en la cola con éxito y tiene un queueId asignado mientras espera a un rival
    data class Waiting(val queueId: String) : MatchmakingResult()
    
    // Se encontró un rival de forma instantánea y se ha creado y enlazado la partida con éxito
    data class Matched(val matchId: String) : MatchmakingResult()
}