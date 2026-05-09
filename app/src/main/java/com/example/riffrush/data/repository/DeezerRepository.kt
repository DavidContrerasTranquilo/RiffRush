package com.example.riffrush.data.repository

import android.util.Log
import com.example.riffrush.data.model.Song

/*
 Repositorio intermedio para acceder a los datos musicales de Deezer.
 Actúa como capa de abstracción sobre DeezerApi, manejando excepciones de red y mapeando
 los modelos puros de la API a nuestro modelo de dominio unificado: Song.
 */
class DeezerRepository(private val api: DeezerApi) {

    // Realiza búsquedas de canciones basadas en un texto de consulta (query)
    suspend fun searchSongs(query: String): List<Song> {
        return try {
            val response = api.search(query)
            response.data.map { item ->
                item.toSong()
            }
        } catch (e: Exception) {
            Log.e("DeezerRepository", "Error searching songs with query: $query", e)
            emptyList()
        }
    }

    // Obtiene las canciones más populares en tendencia (chart) globales
    suspend fun getChart(): List<Song> {
        return try {
            val response = api.getChart()
            response.tracks.data.map { item ->
                item.toSong()
            }
        } catch (e: Exception) {
            Log.e("DeezerRepository", "Error loading chart", e)
            emptyList()
        }
    }

    // Obtiene los datos detallados de una canción específica por su ID
    suspend fun getSong(trackId: Long): Song? {
        return try {
            val item = api.getTrack(trackId)
            item.toSong()
        } catch (e: Exception) {
            Log.e("DeezerRepository", "Error loading song with id: $trackId", e)
            null
        }
    }

    // Obtiene todas las canciones de una playlist de Deezer por su identificador único
    suspend fun getPlaylistTracks(playlistId: Long): List<Song> {
        return try {
            val response = api.getPlaylistTracks(playlistId)
            response.tracks.data.map { item ->
                item.toSong()
            }
        } catch (e: Exception) {
            Log.e("DeezerRepository", "Error loading playlist tracks for id: $playlistId", e)
            emptyList()
        }
    }

    // Función de extensión interna para convertir los modelos puros de la API en canciones de nuestro dominio
    private fun DeezerTrack.toSong(): Song {
        return Song(
            id = id,
            title = title ?: "Unknown Title",
            artist = artist?.name ?: "Unknown Artist",
            album = album?.title ?: "Unknown Album",
            preview = preview ?: "",
            cover = album?.covermedium ?: ""
        )
    }
}