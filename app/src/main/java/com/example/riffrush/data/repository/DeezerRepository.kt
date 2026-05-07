package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song

class DeezerRepository(private val api: DeezerApi) {

    suspend fun searchSongs(query: String): List<Song> {
        return try {
            val response = api.search(query)
            response.data.map { item ->
                Song(
                    id = item.id,
                    title = item.title ?: "Unknown Title",
                    artist = item.artist?.name ?: "Unknown Artist",
                    album = item.album?.title ?: "Unknown Album",
                    preview = item.preview ?: "",
                    cover = item.album?.cover_medium ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getChart(): List<Song> {
        return try {
            val response = api.getChart()
            response.tracks.data.map { item ->
                Song(
                    id = item.id,
                    title = item.title ?: "Unknown Title",
                    artist = item.artist?.name ?: "Unknown Artist",
                    album = item.album?.title ?: "Unknown Album",
                    preview = item.preview ?: "",
                    cover = item.album?.cover_medium ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    //He modificado esto para que me devuelva la cancion con su preview!!
    suspend fun getSong(trackId: Long): Song? {
        return try {
            val item = api.getTrack(trackId)
            Song(
                id = item.id,
                title = item.title ?: "Unknown Title",
                artist = item.artist?.name ?: "Unknown Artist",
                album = item.album?.title ?: "Unknown Album",
                preview = item.preview ?: "",
                cover = item.album?.cover_medium ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
