package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import com.example.riffrush.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesGameViewModel : ViewModel() {
    private val _playingSongs = MutableStateFlow<List<Song>>(emptyList())
    val playingSongs = _playingSongs.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    private val _gameState = MutableStateFlow(FavoritesState.PLAYING)
    val gameState = _gameState.asStateFlow()

    enum class FavoritesState { PLAYING, FINISHED }

    fun initGame(favorites: List<Song>) {
        _playingSongs.value = favorites.shuffled().take(5)
        _currentIndex.value = 0
        _score.value = 0
        _gameState.value = FavoritesState.PLAYING
    }

    fun submitAnswer(isCorrect: Boolean) {
        if (isCorrect) {
            _score.value++
        }
        
        if (_currentIndex.value < _playingSongs.value.size - 1) {
            _currentIndex.value++
        } else {
            _gameState.value = FavoritesState.FINISHED
        }
    }
}