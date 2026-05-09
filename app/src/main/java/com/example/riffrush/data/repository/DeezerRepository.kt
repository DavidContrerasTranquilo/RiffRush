package com.example.riffrush.data.repository

import android.util.Log
import com.example.riffrush.data.model.Song

class DeezerRepository(private val api: DeezerApi) {

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

    suspend fun getSong(trackId: Long): Song? {
        return try {
            val item = api.getTrack(trackId)
            item.toSong()
        } catch (e: Exception) {
            Log.e("DeezerRepository", "Error loading song with id: $trackId", e)
            null
        }
    }

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