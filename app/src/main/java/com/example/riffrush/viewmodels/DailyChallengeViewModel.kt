package com.example.riffrush.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailyChallengeUiState(
    val isLoading: Boolean = true,
    val isCompletedToday: Boolean = false,
    val songs: List<Song> = emptyList(),
    val currentRoundIndex: Int = 0,
    val score: Int = 0,
    val streak: Int = 0,
    val isGameOver: Boolean = false,
    val hasAnsweredCurrent: Boolean = false,
    val currentAnswerCorrect: Boolean? = null
)

class DailyChallengeViewModel(
    private val context: Context,
    private val deezerRepository: DeezerRepository,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("DailyChallengePrefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    init {
        checkDailyStatusAndLoad()
    }

    private fun checkDailyStatusAndLoad() {
        viewModelScope.launch {
            val todayStr = getCurrentDateString()
            val lastPlayedDate = prefs.getString("last_played_date", "")
            val currentStreak = prefs.getInt("current_streak", 0)

            android.util.Log.d("DAILY_DEBUG", "todayStr=$todayStr lastPlayedDate=$lastPlayedDate")

            if (todayStr == lastPlayedDate) {
                android.util.Log.d("DAILY_DEBUG", "Reto ya completado hoy, no se vuelve a cargar")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isCompletedToday = true,
                    streak = currentStreak
                )
            } else {
                android.util.Log.d("DAILY_DEBUG", "Cargando reto diario...")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isCompletedToday = false,
                    streak = currentStreak
                )
                loadDailySongs()
            }
        }
    }

    private suspend fun loadDailySongs() {
        val todayStr = getCurrentDateString()
        android.util.Log.d("DAILY_DEBUG", "Fecha de hoy: $todayStr")

        val configuredChallenge = firebaseRepository.getDailyChallengeConfig(todayStr)
        android.util.Log.d("DAILY_DEBUG", "Config encontrada: $configuredChallenge")

        if (configuredChallenge != null &&
            configuredChallenge.isActive &&
            configuredChallenge.songIds.size >= 6
        ) {
            android.util.Log.d("DAILY_DEBUG", "Song IDs configuradas: ${configuredChallenge.songIds}")

            val loadedSongs = configuredChallenge.songIds.mapNotNull { id ->
                val parsed = id.toLongOrNull()
                android.util.Log.d("DAILY_DEBUG", "ID original=$id parsed=$parsed")

                parsed?.let { deezerRepository.getSong(it) }
            }

            android.util.Log.d("DAILY_DEBUG", "Canciones cargadas desde config: ${loadedSongs.size}")

            if (loadedSongs.size >= 6) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    songs = loadedSongs.take(6)
                )
                android.util.Log.d("DAILY_DEBUG", "Usando reto configurado por admin")
                return
            }
        }

        android.util.Log.d("DAILY_DEBUG", "Entrando en fallback automático")
        val allSongs = deezerRepository.getChart()

        if (allSongs.size >= 6) {
            val seed = getDailySeed()
            val random = kotlin.random.Random(seed)
            val selectedSongs = allSongs.shuffled(random).take(6)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songs = selectedSongs
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songs = emptyList()
            )
        }
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        if (_uiState.value.hasAnsweredCurrent || _uiState.value.isGameOver) return

        val newScore = if (isCorrect) _uiState.value.score + 1 else _uiState.value.score

        _uiState.value = _uiState.value.copy(
            hasAnsweredCurrent = true,
            currentAnswerCorrect = isCorrect,
            score = newScore
        )
        
        saveResultToFirebase(isCorrect)
    }
    
    private fun saveResultToFirebase(isCorrect: Boolean) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                firebaseRepository.saveDailyChallengeResult(currentUser.uid, isCorrect)
            }
        }
    }

    fun nextRound() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentRoundIndex + 1

        if (nextIndex < currentState.songs.size) {
            _uiState.value = currentState.copy(
                currentRoundIndex = nextIndex,
                hasAnsweredCurrent = false,
                currentAnswerCorrect = null
            )
        } else {
            finishGame()
        }
    }

    private fun finishGame() {
        val todayStr = getCurrentDateString()
        val lastPlayedDate = prefs.getString("last_played_date", "")
        var streak = prefs.getInt("current_streak", 0)

        if (lastPlayedDate == getYesterdayDateString()) {
            streak += 1
        } else if (lastPlayedDate != todayStr) {
            streak = 1
        }

        prefs.edit()
            .putString("last_played_date", todayStr)
            .putInt("current_streak", streak)
            .apply()

        _uiState.value = _uiState.value.copy(
            isGameOver = true,
            streak = streak
        )

        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                firebaseRepository.updateMaxStreak(currentUser.uid, streak)
            }
        }
    }

    //Para conseguir la rachaa
    fun getStreak(): Int {
        return prefs.getInt("current_streak", 0)
    }

    private fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getDailySeed(): Long {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return (year * 1000 + dayOfYear).toLong()
    }
}
