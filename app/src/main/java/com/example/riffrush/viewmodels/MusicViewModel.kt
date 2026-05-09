package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.local.GuestIdManager
import com.example.riffrush.data.local.dao.SongDao
import com.example.riffrush.data.local.entity.toEntity
import com.example.riffrush.data.local.entity.toSong
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

/*
 ViewModel principal de la sección de Música.
 Implementa el patrón Single Source of Truth (SSOT):
 - La UI siempre lee los favoritos desde Room (acceso instantáneo, funciona offline).
 - Firestore se sincroniza en segundo plano y escribe los cambios en Room.
 - Los usuarios invitados (no autenticados) mantienen favoritos solo en memoria RAM.
 */
class MusicViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    private val deezerRepository: DeezerRepository,
    private val guestIdManager: GuestIdManager,
    private val musicPlayer: MusicPlayer,
    private val songDao: SongDao               // DAO de Room para caché local de favoritos
) : ViewModel() {
    // Estado para la lista de canciones que se muestra en la UI
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // Estado para los favoritos (fuente local Room o RAM según autenticación)
    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs

    // Estado para saber si estamos en modo búsqueda o viendo "Mi música"
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // Estado para la canción que se está reproduciendo
    private val _playingSongId = MutableStateFlow<Long?>(null)
    val playingSongId: StateFlow<Long?> = _playingSongId
    private var favoritesJob: Job? = null

    init {
        loadMyMusic()
    }

    private fun getOwnerId(): String {
        return authRepository.getCurrentUser()?.uid ?: guestIdManager.getGuestId()
    }

    // Comprueba si el usuario tiene sesión activa (no anónima)
    fun isLoggedIn(): Boolean {
        val user = authRepository.getCurrentUser()
        return user != null && !user.isAnonymous
    }

    /*
     Carga los favoritos del usuario.
     Si está autenticado: lee primero desde Room (instantáneo) y sincroniza con Firestore en segundo plano.
     Si es invitado: mantiene la lista en memoria RAM.
     */
    fun loadMyMusic() {
        _isSearching.value = false
        favoritesJob?.cancel()

        if (isLoggedIn()) {
            val ownerId = getOwnerId()

            // Paso 1: Leer desde Room (caché local instantánea - SSOT)
            favoritesJob = viewModelScope.launch {
                songDao.getFavoritesByUser(ownerId).collect { entities ->
                    val favs = entities.map { it.toSong() }
                    _favoriteSongs.value = favs
                    if (!_isSearching.value) {
                        _songs.value = favs
                    }
                }
            }

            // Paso 2: Sincronizar Firestore → Room en segundo plano
            viewModelScope.launch {
                try {
                    firebaseRepository.getFavorites(ownerId).collect { cloudFavs ->
                        // Inserta los datos de la nube en Room (REPLACE resuelve duplicados)
                        val entities = cloudFavs.map { it.toEntity(ownerId) }
                        songDao.insertAll(entities)
                    }
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Error sincronizando favoritos desde Firestore", e)
                }
            }
        } else {
            if (!_isSearching.value) {
                _songs.value = _favoriteSongs.value
            }
        }
    }

    // Busca canciones nuevas por texto en la API de Deezer
    fun searchSongs(query: String) {
        if (query.isBlank()) return
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = deezerRepository.searchSongs(query)
                _songs.value = results
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error buscando canciones: $query", e)
                _songs.value = emptyList()
            }
        }
    }

    /*
     Agrega o quita de favoritos.
     Si está autenticado: escribe en Room (instantáneo) + Firestore (segundo plano).
     Si es invitado: solo modifica la lista en memoria RAM.
     */
    fun toggleFavorite(song: Song) {
        val isFavorite = _favoriteSongs.value.any { it.id == song.id }

        if (isLoggedIn()) {
            val ownerId = getOwnerId()
            viewModelScope.launch {
                if (isFavorite) {
                    // Elimina de Room (la UI se actualiza instantáneamente vía Flow)
                    songDao.delete(song.id, ownerId)
                    // Elimina de Firestore en segundo plano
                    firebaseRepository.removeFavorite(ownerId, song.id.toString())
                } else {
                    // Inserta en Room (la UI se actualiza instantáneamente vía Flow)
                    songDao.insert(song.toEntity(ownerId))
                    // Inserta en Firestore en segundo plano
                    firebaseRepository.addFavorite(ownerId, song)
                }
            }
        } else {
            // Usuarios invitados: solo en memoria RAM
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

    // Lógica de reproducción de preescucha de 30 segundos
    fun playPause(song: Song) {
        viewModelScope.launch {
            if (_playingSongId.value == song.id) {
                musicPlayer.stop()
                _playingSongId.value = null
                return@launch
            }

            var url = song.preview

            try {
                val freshSong = deezerRepository.getSong(song.id)

                if (freshSong != null && freshSong.preview.isNotBlank()) {
                    url = freshSong.preview

                    // Actualiza la info local cacheada sin afectar la lista de favoritos automáticamente
                    val currentFavs = _favoriteSongs.value.toMutableList()
                    val index = currentFavs.indexOfFirst { it.id == freshSong.id }

                    if (index != -1) {
                        currentFavs[index] = freshSong
                        _favoriteSongs.value = currentFavs

                        if (!_isSearching.value) {
                            _songs.value = currentFavs
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error actualizando preview de canción: ${song.id}", e)
            }

            if (url.isNotBlank()) {
                musicPlayer.stop()
                musicPlayer.playPreview(url)
                _playingSongId.value = song.id
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        favoritesJob?.cancel()
        stopPlayback()
    }

    fun stopPlayback() {
        musicPlayer.stop()
        _playingSongId.value = null
    }
}