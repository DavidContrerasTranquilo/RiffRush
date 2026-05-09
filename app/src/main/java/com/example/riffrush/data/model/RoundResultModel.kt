package com.example.riffrush.data.model

/*
 Resultados parciales de una ronda individual dentro de una partida.
 Guarda qué canción sonó, el artista, y si cada jugador respectivo la adivinó correctamente o no.
 */
data class RoundResultModel(
    val round: Int = 0,                         // Número de ronda (de 1 a 5)
    val songTitle: String = "",                 // Título de la canción reproducida
    val songArtist: String = "",                // Artista de la canción reproducida
    val player1Correct: Boolean = false,        // Indica si el jugador 1 acertó la respuesta
    val player2Correct: Boolean = false         // Indica si el jugador 2 acertó la respuesta
)