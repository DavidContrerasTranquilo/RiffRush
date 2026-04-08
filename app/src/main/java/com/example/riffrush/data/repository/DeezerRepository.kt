package com.example.riffrush.data.repository

import com.example.riffrush.data.model.Song

class DeezerRepository(private val api: DeezerApi) {

    suspend fun searchSongs(query: String): List<Song> {
        return try {
            val response = api.search(query)
            response.data.map { item ->
                Song(
                    id = item.id,
                    title = item.title,
                    artist = item.artist.name,
                    album = item.album.title,
                    preview = item.preview,
                    cover = item.album.cover_medium
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
                    title = item.title,
                    artist = item.artist.name,
                    album = item.album.title,
                    preview = item.preview,
                    cover = item.album.cover_medium
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}