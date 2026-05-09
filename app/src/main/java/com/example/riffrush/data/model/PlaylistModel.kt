package com.example.riffrush.data.model

/*
 Modelo que representa los metadatos básicos de una playlist personalizada (actualmente para favoritos).
 Permite almacenar y estructurar las agrupaciones de canciones del usuario.
 */
data class PlaylistModel(
    val id: String = "",                               // ID identificador de la playlist
    val name: String = "",                             // Nombre visible de la playlist
    val createdAt: Long = System.currentTimeMillis(),  // Fecha de creación en milisegundos
    val songsCount: Int = 0                            // Cantidad de canciones que componen la playlist
)