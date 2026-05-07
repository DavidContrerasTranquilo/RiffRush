package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.local.GuestIdManager
import com.example.riffrush.data.model.PlaylistModel
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class PlaylistViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val guestIdManager: GuestIdManager
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<PlaylistModel>>(emptyList())
    val playlists: StateFlow<List<PlaylistModel>> = _playlists

    private val _selectedPlaylistSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistSongs

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage
    private var playlistsJob: Job? = null
    private var playlistSongsJob: Job? = null
    private fun getOwnerId(): String {
        return authRepository.getCurrentUser()?.uid ?: guestIdManager.getGuestId()
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun loadPlaylists() {
        val ownerId = getOwnerId()

        playlistsJob?.cancel()
        playlistsJob = viewModelScope.launch {
            firebaseRepository.getPlaylists(ownerId).collect { list ->
                _playlists.value = list
            }
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) {
            _uiMessage.value = "El nombre no puede estar vacío"
            return
        }
        if (name.length > 30) {
            _uiMessage.value = "Máximo 30 caracteres"
            return
        }
        if (_playlists.value.any { it.name.equals(name.trim(), true) }) {
            _uiMessage.value = "Ya existe una playlist con ese nombre"
            return
        }

        val ownerId = getOwnerId()

        viewModelScope.launch {
            try {
                firebaseRepository.createPlaylist(ownerId, name.trim())
                _uiMessage.value = "Playlist creada"
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error creando playlist con nombre: $name", e)
                _uiMessage.value = "Error al crear playlist"
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        val ownerId = getOwnerId()

        viewModelScope.launch {
            try {
                firebaseRepository.deletePlaylist(ownerId, playlistId)
                _uiMessage.value = "Playlist eliminada"
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error eliminando playlist: $playlistId", e)
                _uiMessage.value = "Error al eliminar playlist"
            }
        }
    }

    fun loadPlaylistSongs(playlistId: String) {
        val ownerId = getOwnerId()

        playlistSongsJob?.cancel()
        playlistSongsJob = viewModelScope.launch {
            firebaseRepository.getPlaylistSongs(ownerId, playlistId).collect { songs ->
                _selectedPlaylistSongs.value = songs
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        val ownerId = getOwnerId()

        viewModelScope.launch {
            try {
                firebaseRepository.addSongToPlaylist(ownerId, playlistId, song)
                _uiMessage.value = "Canción añadida a playlist"
            } catch (e: Exception) {
                Log.e(
                    "PlaylistViewModel",
                    "Error añadiendo canción ${song.id} a playlist $playlistId",
                    e
                )
                _uiMessage.value = "Error al añadir canción"
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val ownerId = getOwnerId()

        viewModelScope.launch {
            try {
                firebaseRepository.removeSongFromPlaylist(ownerId, playlistId, songId)
                _uiMessage.value = "Canción eliminada"
            } catch (e: Exception) {
                Log.e(
                    "PlaylistViewModel",
                    "Error eliminando canción $songId de playlist $playlistId",
                    e
                )
                _uiMessage.value = "Error al eliminar canción"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playlistsJob?.cancel()
        playlistSongsJob?.cancel()
    }
}