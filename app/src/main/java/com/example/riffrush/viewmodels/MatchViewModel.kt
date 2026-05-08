package com.example.riffrush.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.MatchHistoryModel
import com.example.riffrush.data.model.MatchModel
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.MatchmakingResult
import com.example.riffrush.data.model.MatchmakingUiState
import com.example.riffrush.data.model.RoundResultModel
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.DeezerApi
import com.example.riffrush.data.repository.FirebaseRepository
import com.example.riffrush.data.repository.MatchRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class MatchViewModel(
    private val repository: MatchRepository,
    private val firebaseRepository: FirebaseRepository,
    private val deezerApi: DeezerApi,
    private val musicPlayer: MusicPlayer
) : ViewModel() {

    private val _currentMatch = MutableStateFlow<MatchModel?>(null)
    val currentMatch: StateFlow<MatchModel?> = _currentMatch

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _currentRound = MutableStateFlow(0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _timeLeft = MutableStateFlow(1f)
    val timeLeft: StateFlow<Float> = _timeLeft

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var matchJob: Job? = null
    private val _matchmakingState = MutableStateFlow<MatchmakingUiState>(MatchmakingUiState.Idle)
    val matchmakingState: StateFlow<MatchmakingUiState> = _matchmakingState
    private var historySavedForMatchId: String? = null
    private var queueJob: Job? = null
    private var currentQueueId: String? = null
    private var gamesPlayedRegisteredForMatchId: String? = null
    private val _currentTimerDuration = MutableStateFlow(15000L)
    val currentTimerDuration: StateFlow<Long> = _currentTimerDuration
    private val _showAnswerResult = MutableStateFlow(false)
    val showAnswerResult: StateFlow<Boolean> = _showAnswerResult

    private val _lastAnswerCorrect = MutableStateFlow<Boolean?>(null)
    val lastAnswerCorrect: StateFlow<Boolean?> = _lastAnswerCorrect
    private var presenceJob: Job? = null
    private var disconnectWatcherJob: Job? = null
    private var presenceMatchId: String? = null
    private var watcherMatchId: String? = null
    fun startMatchmaking(opponentId: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val currentUserId = repository.getCurrentUserId()
                val matchId = java.util.UUID.randomUUID().toString()
                val songIds = listOf(1109152L, 2109522L, 3109522L, 4109522L, 5109522L)

                // Obtener información del oponente desde Firebase
                val firestore = FirebaseFirestore.getInstance()
                val opponentDoc = firestore.collection("users").document(opponentId).get().await()
                val opponentName = opponentDoc.getString("name")
                    ?: opponentDoc.getString("username")
                    ?: opponentDoc.getString("displayName")
                    ?: "Invitado"
                val opponentPfp = opponentDoc.getString("pfpUrl") ?: ""

                // Obtener información del jugador actual
                val currentPlayerDoc =
                    firestore.collection("users").document(currentUserId).get().await()
                val currentPlayerName = currentPlayerDoc.getString("name")
                    ?: currentPlayerDoc.getString("username")
                    ?: currentPlayerDoc.getString("displayName")
                    ?: "Tú"
                val currentPlayerPfp = currentPlayerDoc.getString("pfpUrl") ?: ""
                val newMatch = MatchModel(
                    matchId = matchId,
                    player1Id = currentUserId,
                    player2Id = opponentId,
                    player1Name = currentPlayerName,
                    player2Name = opponentName,
                    player1PfpUrl = currentPlayerPfp,
                    player2PfpUrl = opponentPfp,
                    status = MatchStatus.WAITING,
                    staticSongList = songIds,
                    player1LastSeen = System.currentTimeMillis(),
                    player2LastSeen = System.currentTimeMillis()
                )
                repository.createMatch(newMatch)
                listenToMatch(matchId)

            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error creating match", e)
                _error.value = "Error al crear la partida: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun listenToMatch(matchId: String) {
        matchJob?.cancel()
        presenceJob?.cancel()
        disconnectWatcherJob?.cancel()
        timerJob?.cancel()

        presenceJob = null
        disconnectWatcherJob = null
        timerJob = null
        opponentAnsweredTimerStartedForRound = null

        _currentMatch.value = null
        _songs.value = emptyList()
        _currentRound.value = 0
        _isLoading.value = true

        Log.d("MATCH_DEBUG", "listenToMatch START matchId=$matchId")

        matchJob = viewModelScope.launch {
            try {
                repository.listenToMatch(matchId).collect { match ->
                    Log.d(
                        "MATCH_DEBUG",
                        "listenToMatch RECEIVED matchId=${match?.matchId} " +
                                "round=${match?.currentRound} " +
                                "score=${match?.scoreP1}-${match?.scoreP2} " +
                                "answered=${match?.answeredP1}-${match?.answeredP2} " +
                                "status=${match?.status} " +
                                "p1=${match?.player1Id} " +
                                "p2=${match?.player2Id}"
                    )

                    val previousMatch = _currentMatch.value
                    val previousRound = previousMatch?.currentRound ?: -1

                    Log.d(
                        "MATCH_DEBUG",
                        "listenToMatch PREVIOUS previousRound=$previousRound " +
                                "previousStatus=${previousMatch?.status} " +
                                "previousAnswered=${previousMatch?.answeredP1}-${previousMatch?.answeredP2}"
                    )

                    _currentMatch.value = match

                    if (
                        match?.status == MatchStatus.FINISHED &&
                        gamesPlayedRegisteredForMatchId != match.matchId
                    ) {
                        presenceJob?.cancel()
                        disconnectWatcherJob?.cancel()
                        presenceJob = null
                        disconnectWatcherJob = null

                        Log.d(
                            "MATCH_DEBUG",
                            "listenToMatch FINISHED detected. Registering stats/history for match=${match.matchId}"
                        )

                        gamesPlayedRegisteredForMatchId = match.matchId

                        val currentUserId = repository.getCurrentUserId()

                        Log.d(
                            "MATCH_DEBUG",
                            "listenToMatch FINISHED currentUserId=$currentUserId"
                        )

                        if (currentUserId.isNotBlank()) {
                            val isAnonymous =
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                            if (!isAnonymous) {
                                firebaseRepository.incrementGamesPlayed(currentUserId)
                                saveHistoryForCurrentUser(match)
                            }
                        }
                    }

                    match?.let { currentMatch ->
                        val isOffline = currentMatch.matchId.startsWith("offline")

                        if (!isOffline && currentMatch.status == MatchStatus.PLAYING) {
                            val amIPlayer1 = getCurrentPlayerId() == currentMatch.player1Id
                            startPresence(currentMatch.matchId, amIPlayer1)
                            watchOpponentConnection(currentMatch)
                        }

                        Log.d(
                            "MATCH_DEBUG",
                            "listenToMatch CURRENT isOffline=$isOffline " +
                                    "songsLoaded=${_songs.value.size} " +
                                    "staticSongList=${currentMatch.staticSongList.size}"
                        )

                        // Cargar canciones solo una vez por partida
                        if (_songs.value.isEmpty() && currentMatch.staticSongList.isNotEmpty()) {
                            Log.d(
                                "MATCH_DEBUG",
                                "listenToMatch LOAD_SONGS starting with ${currentMatch.staticSongList.size} songs"
                            )

                            loadSongs(currentMatch.staticSongList) {
                                Log.d(
                                    "MATCH_DEBUG",
                                    "listenToMatch LOAD_SONGS complete. status=${currentMatch.status} isOffline=$isOffline"
                                )

                                // Al terminar de cargar por primera vez, si ya estamos jugando, arrancar timer
                                if (currentMatch.status == MatchStatus.PLAYING && !isOffline) {
                                    val amIPlayer1 = getCurrentPlayerId() == currentMatch.player1Id
                                    val hasAnswered = if (amIPlayer1) {
                                        currentMatch.answeredP1
                                    } else {
                                        currentMatch.answeredP2
                                    }

                                    Log.d(
                                        "MATCH_DEBUG",
                                        "listenToMatch LOAD_SONGS timerCheck " +
                                                "currentPlayer=${getCurrentPlayerId()} " +
                                                "amIPlayer1=$amIPlayer1 " +
                                                "hasAnswered=$hasAnswered"
                                    )

                                    if (!hasAnswered) {
                                        Log.d(
                                            "MATCH_DEBUG",
                                            "listenToMatch LOAD_SONGS startTimer 15000"
                                        )
                                        startTimer()
                                    }
                                }
                            }
                        }

                        if (!isOffline) {
                            _currentRound.value = currentMatch.currentRound

                            if (currentMatch.status == MatchStatus.PLAYING && _songs.value.isNotEmpty()) {
                                val hasAnswered = haveIAnswered(currentMatch)
                                val opponentAnswered = hasOpponentAnswered(currentMatch)

                                val roundChanged = currentMatch.currentRound != previousRound
                                val justStarted = previousMatch?.status == MatchStatus.WAITING

                                Log.d(
                                    "MATCH_DEBUG",
                                    "listenToMatch TIMER currentPlayer=${getCurrentPlayerId()} " +
                                            "hasAnswered=$hasAnswered " +
                                            "opponentAnswered=$opponentAnswered " +
                                            "previousRound=$previousRound " +
                                            "currentRound=${currentMatch.currentRound} " +
                                            "roundChanged=$roundChanged " +
                                            "justStarted=$justStarted " +
                                            "opponentAnsweredTimerStartedForRound=$opponentAnsweredTimerStartedForRound"
                                )

                                if (roundChanged || justStarted) {
                                    Log.d(
                                        "MATCH_DEBUG",
                                        "listenToMatch TIMER roundChanged/justStarted -> reset opponentAnsweredTimerStartedForRound"
                                    )

                                    opponentAnsweredTimerStartedForRound = null

                                    if (!hasAnswered) {
                                        Log.d(
                                            "MATCH_DEBUG",
                                            "listenToMatch TIMER startTimer 15000"
                                        )
                                        startTimer(15000L)
                                    } else {
                                        Log.d(
                                            "MATCH_DEBUG",
                                            "listenToMatch TIMER not starting 15000 because hasAnswered=true"
                                        )
                                    }
                                }

                                if (
                                    opponentAnswered &&
                                    !hasAnswered &&
                                    opponentAnsweredTimerStartedForRound != currentMatch.currentRound
                                ) {
                                    Log.d(
                                        "MATCH_DEBUG",
                                        "listenToMatch TIMER opponent answered -> startTimer 7000"
                                    )

                                    opponentAnsweredTimerStartedForRound = currentMatch.currentRound
                                    startTimer(7000L)
                                } else {
                                    Log.d(
                                        "MATCH_DEBUG",
                                        "listenToMatch TIMER no 7s timer. " +
                                                "condition opponentAnswered=$opponentAnswered " +
                                                "!hasAnswered=${!hasAnswered} " +
                                                "alreadyStarted=${opponentAnsweredTimerStartedForRound == currentMatch.currentRound}"
                                    )
                                }
                            } else {
                                Log.d(
                                    "MATCH_DEBUG",
                                    "listenToMatch TIMER skipped. " +
                                            "status=${currentMatch.status} songsLoaded=${_songs.value.size}"
                                )
                            }
                        }

                        // Si por alguna razón la partida online sigue en WAITING y ya tiene dos jugadores, arrancarla
                        if (
                            !isOffline &&
                            currentMatch.status == MatchStatus.WAITING &&
                            currentMatch.player1Id.isNotEmpty() &&
                            currentMatch.player2Id.isNotEmpty()
                        ) {
                            Log.d(
                                "MATCH_DEBUG",
                                "listenToMatch WAITING with two players -> update PLAYING"
                            )

                            repository.updateMatchStatus(matchId, MatchStatus.PLAYING)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MATCH_DEBUG", "listenToMatch ERROR", e)
                Log.e("MatchViewModel", "Error listening to match", e)
                _error.value = "Error de conexión: ${e.message}"
            }
        }
    }

    private fun loadSongs(ids: List<Long>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val loadedSongs = ids.map { id ->
                    val track = deezerApi.getTrack(id)
                    Song(
                        id = track.id,
                        title = track.title ?: "Sin título",
                        artist = track.artist?.name ?: "Artista desconocido",
                        album = track.album?.title ?: "Álbum desconocido",
                        preview = track.preview ?: "",
                        cover = track.album?.covermedium ?: ""
                    )
                }
                _songs.value = loadedSongs
                _isLoading.value = false
                onComplete()
            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error loading songs", e)
                _isLoading.value = false
            }
        }
    }

    private var timerJob: Job? = null
    private var opponentAnsweredTimerStartedForRound: Int? = null

    private fun startTimer(totalTime: Long = 15000L) {
        timerJob?.cancel()

        _currentTimerDuration.value = totalTime // 👈 IMPORTANTE
        _timeLeft.value = 1f

        timerJob = viewModelScope.launch {
            val interval = 100L
            var elapsed = 0L

            while (elapsed < totalTime) {
                delay(interval)
                elapsed += interval
                _timeLeft.value = 1f - (elapsed.toFloat() / totalTime)
            }

            onAnswerSelected(false)
        }
    }

    fun onAnswerSelected(isCorrect: Boolean) {
        timerJob?.cancel()
        musicPlayer.stop()

        val match = _currentMatch.value ?: return
        val isOffline = match.matchId.startsWith("offline")
        val currentUserId = repository.getCurrentUserId()

        _lastAnswerCorrect.value = isCorrect
        _showAnswerResult.value = true

        viewModelScope.launch {
            try {
                delay(1000)

                _showAnswerResult.value = false

                try {
                    val isAnonymous =
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                    if (!isAnonymous) {
                        firebaseRepository.incrementSongsPlayed(currentUserId)
                    }
                } catch (e: Exception) {
                    Log.e(
                        "MATCH_DEBUG",
                        "No se pudo incrementar songsPlayed, pero la partida continúa",
                        e
                    )
                }

                if (isOffline) {
                    handleOfflineAnswer(match, isCorrect)
                } else {
                    repository.submitAnswer(match.matchId, isCorrect)
                }

            } catch (e: Exception) {
                _showAnswerResult.value = false
                Log.e("MATCH_DEBUG", "Error submitting answer", e)
                _error.value = "Error al enviar la respuesta: ${e.message}"
            }
        }
    }

    private fun handleOfflineAnswer(match: MatchModel, isCorrect: Boolean) {
        // En offline, usamos el MatchModel para llevar el estado, 
        // pero necesitamos simular un poco de espera para que no sea instantáneo
        viewModelScope.launch {
            // Marcamos que hemos respondido localmente en el modelo para que la UI se bloquee
            val matchWithAnswer = match.copy(answeredP1 = true)
            _currentMatch.value = matchWithAnswer

            delay(1500) // Pausa para ver el resultado de la ronda

            var updatedMatch = matchWithAnswer
            if (isCorrect) {
                updatedMatch = updatedMatch.copy(scoreP1 = updatedMatch.scoreP1 + 1)
            }

            val botHits = (0..2).random() > 0 // El bot es decente
            if (botHits) {
                updatedMatch = updatedMatch.copy(scoreP2 = updatedMatch.scoreP2 + 1)
            }

            val currentSong = _songs.value.getOrNull(updatedMatch.currentRound)

            val newRoundResult = RoundResultModel(
                round = updatedMatch.currentRound + 1,
                songTitle = currentSong?.title ?: "Canción desconocida",
                songArtist = currentSong?.artist ?: "",
                player1Correct = isCorrect,
                player2Correct = botHits
            )

            updatedMatch = updatedMatch.copy(
                roundResults = updatedMatch.roundResults + newRoundResult
            )

            val totalSongs = _songs.value.size
            val currentRoundValue = updatedMatch.currentRound

            if (currentRoundValue < totalSongs - 1) {
                val nextRound = currentRoundValue + 1
                _currentRound.value = nextRound
                _currentMatch.value = updatedMatch.copy(
                    currentRound = nextRound,
                    answeredP1 = false,
                    answeredP2 = false
                )
                startTimer() // Reiniciamos el timer para la nueva ronda
            } else {
                val finishedMatch = updatedMatch.copy(
                    status = MatchStatus.FINISHED
                )

                _currentMatch.value = finishedMatch

                val currentUserId = repository.getCurrentUserId()

                if (currentUserId.isNotBlank() && gamesPlayedRegisteredForMatchId != finishedMatch.matchId) {
                    gamesPlayedRegisteredForMatchId = finishedMatch.matchId

                    val isAnonymous =
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                    if (!isAnonymous) {
                        firebaseRepository.incrementGamesPlayed(currentUserId)
                        saveHistoryForCurrentUser(finishedMatch)
                    }
                }

            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        matchJob?.cancel()
        queueJob?.cancel()
        presenceJob?.cancel()
        disconnectWatcherJob?.cancel()
    }

    fun startOfflineMode(category: String) {
        _isLoading.value = true
        _error.value = null
        _songs.value = emptyList()
        _currentRound.value = 0
        _currentMatch.value = null
        gamesPlayedRegisteredForMatchId = null
        historySavedForMatchId = null
        timerJob?.cancel()

        viewModelScope.launch {
            try {
                val offlineMatch = repository.getOfflineMatch(category).copy(
                    currentRound = 0
                )

                _currentMatch.value = offlineMatch
                loadSongs(offlineMatch.staticSongList) {
                    startTimer() // Iniciar el timer SOLO cuando las canciones estén listas
                }

            } catch (e: Exception) {
                Log.e("MatchViewModel", "Error starting offline mode", e)
                _error.value = "Error al iniciar la partida offline: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun listenToQueue(queueId: String) {
        queueJob?.cancel()
        queueJob = viewModelScope.launch {
            repository.listenToQueueEntry(queueId).collect { entry ->
                val matchId = entry?.matchId
                if (entry?.status == "MATCHED" && !matchId.isNullOrBlank()) {
                    _matchmakingState.value = MatchmakingUiState.Matched(matchId)
                    listenToMatch(matchId)
                }
            }
        }
    }

    fun joinPlaylistMatchmaking(playlist: String) {
        _matchmakingState.value = MatchmakingUiState.Searching

        viewModelScope.launch {
            try {
                when (val result = repository.joinMatchmaking(playlist)) {
                    is MatchmakingResult.Matched -> {
                        _matchmakingState.value = MatchmakingUiState.Matched(result.matchId)
                        listenToMatch(result.matchId)
                    }

                    is MatchmakingResult.Waiting -> {
                        currentQueueId = result.queueId
                        _matchmakingState.value = MatchmakingUiState.Waiting(playlist)
                        listenToQueue(result.queueId)
                    }
                }
            } catch (e: Exception) {
                _matchmakingState.value =
                    MatchmakingUiState.Error("Error en matchmaking: ${e.message}")
            }
        }
    }

    fun cancelMatchmaking() {
        val queueId = currentQueueId ?: return
        viewModelScope.launch {
            repository.cancelMatchmaking(queueId)
            currentQueueId = null
            _matchmakingState.value = MatchmakingUiState.Idle
        }
    }

    fun getCurrentPlayerId(): String {
        return repository.getCurrentUserId()
    }

    private fun hasOpponentAnswered(match: MatchModel): Boolean {
        val currentUserId = getCurrentPlayerId()
        val amIPlayer1 = currentUserId == match.player1Id

        return if (amIPlayer1) {
            match.answeredP2
        } else {
            match.answeredP1
        }
    }

    private fun haveIAnswered(match: MatchModel): Boolean {
        val currentUserId = getCurrentPlayerId()
        val amIPlayer1 = currentUserId == match.player1Id

        return if (amIPlayer1) {
            match.answeredP1
        } else {
            match.answeredP2
        }
    }

    private suspend fun saveHistoryForCurrentUser(match: MatchModel) {
        val currentUserId = repository.getCurrentUserId()
        if (currentUserId.isBlank()) return

        val amIPlayer1 = currentUserId == match.player1Id

        val myScore = if (amIPlayer1) match.scoreP1 else match.scoreP2
        val opponentScore = if (amIPlayer1) match.scoreP2 else match.scoreP1

        val opponentId = if (amIPlayer1) match.player2Id else match.player1Id
        val opponentName = if (amIPlayer1) {
            match.player2Name.ifBlank { "Invitado" }
        } else {
            match.player1Name.ifBlank { "Invitado" }
        }

        val result = when {
            myScore > opponentScore -> "WIN"
            myScore < opponentScore -> "LOSE"
            else -> "DRAW"
        }

        val mode = if (match.matchId.startsWith("offline")) "OFFLINE" else "ONLINE"

        firebaseRepository.saveMatchHistory(
            MatchHistoryModel(
                matchId = match.matchId,
                userId = currentUserId,
                opponentId = opponentId,
                opponentName = opponentName,
                mode = mode,
                playlist = "",
                myScore = myScore,
                opponentScore = opponentScore,
                result = result
            )
        )
    }

    fun abandonCurrentMatch() {
        val match = _currentMatch.value ?: return
        val isOffline = match.matchId.startsWith("offline")

        if (isOffline || match.status != MatchStatus.PLAYING) return

        viewModelScope.launch {
            try {
                repository.abandonMatch(match.matchId)
            } catch (e: Exception) {
                Log.e("MATCH_DEBUG", "Error abandoning match", e)
            }
        }
    }

    private fun startPresence(matchId: String, isPlayer1: Boolean) {
        if (presenceJob?.isActive == true && presenceMatchId == matchId) return

        presenceJob?.cancel()
        presenceMatchId = matchId

        presenceJob = viewModelScope.launch {
            while (true) {
                try {
                    repository.updatePresence(matchId, isPlayer1)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("MATCH_DEBUG", "Error updating presence", e)
                }

                delay(3000)
            }
        }
    }

    private fun watchOpponentConnection(match: MatchModel) {
        if (disconnectWatcherJob?.isActive == true && watcherMatchId == match.matchId) return

        disconnectWatcherJob?.cancel()
        watcherMatchId = match.matchId

        disconnectWatcherJob = viewModelScope.launch {
            while (true) {
                delay(5000)

                val current = _currentMatch.value ?: continue

                if (current.matchId != match.matchId) break
                if (current.status != MatchStatus.PLAYING) break

                val currentUserId = repository.getCurrentUserId()
                val now = System.currentTimeMillis()

                val opponentId: String
                val opponentLastSeen: Long

                if (currentUserId == current.player1Id) {
                    opponentId = current.player2Id
                    opponentLastSeen = current.player2LastSeen
                } else {
                    opponentId = current.player1Id
                    opponentLastSeen = current.player1LastSeen
                }

                Log.d(
                    "MATCH_DEBUG",
                    "watchOpponentConnection match=${current.matchId} opponent=$opponentId " +
                            "lastSeen=$opponentLastSeen diff=${now - opponentLastSeen}"
                )

                if (opponentLastSeen > 0L && now - opponentLastSeen > 20000L) {
                    repository.endMatchByConnectionLoss(
                        matchId = current.matchId,
                        disconnectedPlayerId = opponentId,
                        winnerId = currentUserId
                    )

                    break
                }
            }
        }
    }
}