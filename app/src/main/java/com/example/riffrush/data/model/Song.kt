package com.example.riffrush.data.model

/*
 Representa los datos estructurados de una Canción (Song) dentro de la aplicación.
 Mapea la información obtenida desde la API de Deezer para que pueda reproducirse y mostrarse en el juego.
 */
data class Song(
    val id: Long = 0,               // ID oficial único de la canción en la API de Deezer
    val title: String = "",         // Título de la canción
    val artist: String = "",        // Nombre del artista o grupo musical
    val album: String = "",         // Nombre del álbum musical
    val preview: String = "",       // URL de streaming de 30 segundos (mp3 preview) para reproducción
    val cover: String = ""          // URL de la carátula mediana del álbum para el juego
) {
    // Constructor secundario sin argumentos requerido para la deserialización automática de Firebase
    constructor() : this(0, "", "", "", "", "")
}