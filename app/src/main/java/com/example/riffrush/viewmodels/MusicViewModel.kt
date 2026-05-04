package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.local.GuestIdManager
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val deezerRepository: DeezerRepository,
    private val guestIdManager: GuestIdManager,
    private val musicPlayer: MusicPlayer
) : ViewModel() {
    // Estado para la lista de canciones que se muestra
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // Estado para los favoritos
    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs

    // Estado para saber si estamos en modo búsqueda o viendo "Mi música"
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // Estado para la canción que se está reproduciendo
    private val _playingSongId = MutableStateFlow<Long?>(null)
    val playingSongId: StateFlow<Long?> = _playingSongId

    init {
        loadMyMusic()
    }

    private fun getOwnerId(): String {
        return authRepository.getCurrentUser()?.uid ?: guestIdManager.getGuestId()
    }
    //David, Añado para ver si esta logueado
    fun isLoggedIn(): Boolean {
        val user = authRepository.getCurrentUser()
        return user != null && !user.isAnonymous
    }
    //Si no esta logueado no cargamos la musica
    fun loadMyMusic() {
        _isSearching.value = false
        if (isLoggedIn()) {
            val ownerId = getOwnerId()
            viewModelScope.launch {
                firebaseRepository.getFavorites(ownerId).collect { favs ->
                    _favoriteSongs.value = favs
                    if (!_isSearching.value) {
                        _songs.value = favs
                    }
                }
            }
        } else {
            // RAM only mode
            if (!_isSearching.value) {
                _songs.value = _favoriteSongs.value
            }
        }
    }

    /**
     * Busca canciones nuevas por texto
     */
    fun searchSongs(query: String) {
        if (query.isBlank()) return
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = deezerRepository.searchSongs(query)
                _songs.value = results
            } catch (e: Exception) {
                _songs.value = emptyList()
            }
        }
    }

    /**
     * Agrega o quita de favoritos
     */
     //Tambien añado aqui que si no esta logueado la musica se guarde en ram
     //Se podria hacer en Room!
    fun toggleFavorite(song: Song) {
        val isFavorite = _favoriteSongs.value.any { it.id == song.id }

        if (isLoggedIn()) {
            val ownerId = getOwnerId()
            viewModelScope.launch {
                if (isFavorite) {
                    firebaseRepository.removeFavorite(ownerId, song.id.toString())
                } else {
                    firebaseRepository.addFavorite(ownerId, song)
                }
            }
        } else {
            
            val currentFavs = _favoriteSongs.value.toMutableList()
            if (isFavorite) {
                currentFavs.removeAll { it.id == song.id }
            } else {
                currentFavs.add(song)
            }
            _favoriteSongs.value = currentFavs
            if (!_isSearching.value) {
                _songs.value = currentFavs
            }
        }
    }

    /**
     * Lógica de reproducción
     */
    fun playPause(song: Song) {
        viewModelScope.launch {
            var url = song.preview

            // Siempre obtenemos la canción fresca para evitar que el token del preview expire
            try {
                val fSong = deezerRepository.getSong(song.id)
                if (fSong != null && fSong.preview.isNotBlank()) {
                    url = fSong.preview
                    if (isLoggedIn()) {
                        // Actualizar en Firebase para no tener que buscar de nuevo
                        firebaseRepository.addFavorite(getOwnerId(), fSong)

                        // También actualizar localmente para que el preview funcione inmediatamente
                        val currentFavs = _favoriteSongs.value.toMutableList()
                        val index = currentFavs.indexOfFirst { it.id == fSong.id }
                        if (index != -1) {
                            currentFavs[index] = fSong
                            _favoriteSongs.value = currentFavs
                            if (!_isSearching.value) {
                                _songs.value = currentFavs
                            }
                        }

                    } else {
                        val currentFavs = _favoriteSongs.value.toMutableList()
                        val index = currentFavs.indexOfFirst { it.id == fSong.id }
                        if (index != -1) {
                            currentFavs[index] = fSong
                            _favoriteSongs.value = currentFavs
                            if (!_isSearching.value) {
                                _songs.value = currentFavs
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (url.isNotBlank()) {
                if (musicPlayer.isPlaying(url)) {
                    musicPlayer.stop()
                    _playingSongId.value = null
                } else {
                    musicPlayer.playPreview(url)
                    _playingSongId.value = song.id
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicPlayer.release()
        _playingSongId.value = null
    }

}