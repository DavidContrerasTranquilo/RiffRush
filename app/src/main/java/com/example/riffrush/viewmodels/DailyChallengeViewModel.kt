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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.edit

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
    context: Context,
    private val deezerRepository: DeezerRepository,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("DailyChallengePrefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    init {
        checkDailyStatusAndLoad()
    }

    private fun checkDailyStatusAndLoad() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: "guest"
            val todayStr = getCurrentDateString()
            val lastPlayedDate = prefs.getString("last_played_date_$uid", "")
            val currentStreak = prefs.getInt("current_streak_$uid", 0)

            if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "todayStr=$todayStr lastPlayedDate=$lastPlayedDate")

            if (todayStr == lastPlayedDate) {
                if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "Reto ya completado hoy, no se vuelve a cargar")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isCompletedToday = true,
                    streak = currentStreak
                )
            } else {
                if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "Cargando reto diario...")
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
        if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "Fecha de hoy: $todayStr")

        val configuredChallenge = firebaseRepository.getDailyChallengeConfig(todayStr)
        if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "Config encontrada: $configuredChallenge")

        if (configuredChallenge != null &&
            configuredChallenge.isActive &&
            configuredChallenge.songIds.size >= 6
        ) {
            android.util.Log.d(
                "DAILY_DEBUG",
                "Song IDs configuradas: ${configuredChallenge.songIds}"
            )

            val loadedSongs = configuredChallenge.songIds.mapNotNull { id ->
                val parsed = id.toLongOrNull()
                if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "ID original=$id parsed=$parsed")

                parsed?.let { deezerRepository.getSong(it) }
            }

            android.util.Log.d(
                "DAILY_DEBUG",
                "Canciones cargadas desde config: ${loadedSongs.size}"
            )

            if (loadedSongs.size >= 6) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    songs = loadedSongs.take(6)
                )
                if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "Usando reto configurado por admin")
                return
            }
        }

        if (com.example.riffrush.BuildConfig.DEBUG) android.util.Log.d("DAILY_DEBUG", "Entrando en fallback automático")
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
        val uid = authRepository.getCurrentUser()?.uid ?: "guest"
        val todayStr = getCurrentDateString()
        val lastPlayedDate = prefs.getString("last_played_date_$uid", "")
        var streak = prefs.getInt("current_streak_$uid", 0)

        if (lastPlayedDate == getYesterdayDateString()) {
            streak += 1
        } else if (lastPlayedDate != todayStr) {
            streak = 1
        }

        prefs.edit {
            putString("last_played_date_$uid", todayStr)
            putInt("current_streak_$uid", streak)
        }

        _uiState.value = _uiState.value.copy(
            isGameOver = true,
            streak = streak
        )

        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null && !currentUser.isAnonymous) {
                val isWin = _uiState.value.score == _uiState.value.songs.size
                firebaseRepository.saveDailyChallengeResult(currentUser.uid, isWin)
                firebaseRepository.updateMaxStreak(currentUser.uid, streak)
            }
        }
    }

    private fun getCurrentDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun getDailySeed(): Long {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return (year * 1000 + dayOfYear).toLong()
    }
}
