package com.example.riffrush.ui.screens.match

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riffrush.R
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.RoundResultModel
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    matchId: String,
    onExit: () -> Unit,
    onNavigateToUserStats: (String) -> Unit,
    viewModel: MatchViewModel = koinViewModel(),
    musicPlayer: MusicPlayer = koinInject()
) {
    val match by viewModel.currentMatch.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val timerDuration by viewModel.currentTimerDuration.collectAsState()
    val showResult by viewModel.showAnswerResult.collectAsState()
    val isCorrect by viewModel.lastAnswerCorrect.collectAsState()

    LaunchedEffect(matchId) {
        if (matchId.startsWith("offline_")) {
            val rawCategory = matchId.substringAfter("offline_")
            val category = when (rawCategory) {
                "reggaeton" -> "REGGAETON"
                "rap_frances" -> "RAP FRANCÉS"
                "rock_espanol" -> "ROCK ESPAÑOL"
                "top_50_espana" -> "TOP 50 ESPAÑA"
                else -> "REGGAETON"
            }
            viewModel.startOfflineMode(category)
        } else {
            viewModel.listenToMatch(matchId)
        }
    }

    DisposableEffect(Unit) {
        onDispose { musicPlayer.stop() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_match_1vs1), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.abandonCurrentMatch()
                            onExit()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_exit))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading || songs.isEmpty()) {
                LoadingView()
            } else {
                match?.let { currentMatch ->
                    when (currentMatch.status) {
                        MatchStatus.WAITING -> WaitingOpponentView()

                        MatchStatus.PLAYING -> {
                            val roundIndex = currentMatch.currentRound
                            val currentSong = songs.getOrNull(roundIndex)

                            if (currentSong != null) {
                                val currentPlayerId = viewModel.getCurrentPlayerId()
                                val amIPlayer1 = currentPlayerId == currentMatch.player1Id
                                val isOffline = currentMatch.matchId.startsWith("offline")

                                val rawPlayer1Name = currentMatch.player1Name.ifBlank {
                                    if (isOffline) stringResource(R.string.label_you) else stringResource(R.string.default_username_guest)
                                }

                                val rawPlayer2Name = currentMatch.player2Name.ifBlank {
                                    if (isOffline) stringResource(R.string.label_bot) else stringResource(R.string.default_username_guest)
                                }

                                val player1DisplayName =
                                    if (amIPlayer1 && !isOffline) stringResource(R.string.label_player_you, rawPlayer1Name) else rawPlayer1Name

                                val player2DisplayName =
                                    if (!amIPlayer1 && !isOffline) stringResource(R.string.label_player_you, rawPlayer2Name) else rawPlayer2Name

                                val hasAnswered = if (isOffline) {
                                    false
                                } else {
                                    if (amIPlayer1) currentMatch.answeredP1 else currentMatch.answeredP2
                                }

                                GameView(
                                    song = currentSong,
                                    round = roundIndex + 1,
                                    player1Name = player1DisplayName,
                                    player2Name = player2DisplayName,
                                    scoreP1 = currentMatch.scoreP1,
                                    scoreP2 = currentMatch.scoreP2,
                                    allSongs = songs,
                                    hasAnswered = hasAnswered,
                                    timeLeft = timeLeft,
                                    onAnswer = { isCorrect ->
                                        viewModel.onAnswerSelected(isCorrect)
                                    },
                                    onPlayer1Click = {
                                        if (!isOffline) {
                                            onNavigateToUserStats(currentMatch.player1Id)
                                        }
                                    },
                                    onPlayer2Click = {
                                        if (!isOffline) {
                                            onNavigateToUserStats(currentMatch.player2Id)
                                        }
                                    },
                                    musicPlayer = musicPlayer,
                                    timerDuration = timerDuration
                                )
                            }
                        }

                        MatchStatus.FINISHED -> {
                            val currentPlayerId = viewModel.getCurrentPlayerId()
                            val amIPlayer1 = currentPlayerId == currentMatch.player1Id
                            val isOffline = currentMatch.matchId.startsWith("offline")

                            val rawPlayer1Name = currentMatch.player1Name.ifBlank {
                                if (isOffline) stringResource(R.string.label_you) else stringResource(R.string.default_username_guest)
                            }

                            val rawPlayer2Name = currentMatch.player2Name.ifBlank {
                                if (isOffline) stringResource(R.string.label_bot) else stringResource(R.string.default_username_guest)
                            }

                            val player1DisplayName =
                                if (amIPlayer1 && !isOffline) stringResource(R.string.label_player_you, rawPlayer1Name) else rawPlayer1Name
                            val player2DisplayName =
                                if (!amIPlayer1 && !isOffline) stringResource(R.string.label_player_you, rawPlayer2Name) else rawPlayer2Name

                            val opponentAbandoned = currentMatch.endReason == "ABANDONED" &&
                                    currentMatch.abandonedBy != currentPlayerId

                            val iAbandoned = currentMatch.endReason == "ABANDONED" &&
                                    currentMatch.abandonedBy == currentPlayerId
                            val opponentLostConnection =
                                currentMatch.endReason == "CONNECTION_LOST" &&
                                        currentMatch.abandonedBy != currentPlayerId
                            val iLostConnection = currentMatch.endReason == "CONNECTION_LOST" &&
                                    currentMatch.abandonedBy == currentPlayerId
                            val iWinByEndReason = currentMatch.winnerId == currentPlayerId
                            ResultView(
                                player1Name = player1DisplayName,
                                player2Name = player2DisplayName,
                                scoreP1 = currentMatch.scoreP1,
                                scoreP2 = currentMatch.scoreP2,
                                onPlayer1Click = {
                                    if (!isOffline) {
                                        onNavigateToUserStats(currentMatch.player1Id)
                                    }
                                },
                                onPlayer2Click = {
                                    if (!isOffline) {
                                        onNavigateToUserStats(currentMatch.player2Id)
                                    }
                                },
                                onExit = onExit,
                                viewModel = viewModel,
                                roundResults = currentMatch.roundResults,
                                songs = songs,
                                endReason = currentMatch.endReason,
                                opponentAbandoned = opponentAbandoned,
                                iAbandoned = iAbandoned,
                                opponentLostConnection = opponentLostConnection,
                                iLostConnection = iLostConnection,
                                iWinByEndReason = iWinByEndReason
                            )
                        }
                    }
                }
            }
            if (showResult && isCorrect != null) {
                AnswerResultOverlay(isCorrect = isCorrect == true)
            }
        }
    }
}

@Composable
fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.msg_tuning_match),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun WaitingOpponentView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.msg_waiting_opponent),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.msg_music_start_soon),
            color = Color.Gray
        )
    }
}

@Composable
fun GameView(
    song: Song,
    round: Int,
    player1Name: String,
    player2Name: String,
    scoreP1: Int,
    scoreP2: Int,
    allSongs: List<Song>,
    hasAnswered: Boolean = false,
    timeLeft: Float,
    onAnswer: (Boolean) -> Unit,
    onPlayer1Click: () -> Unit,
    onPlayer2Click: () -> Unit,
    musicPlayer: MusicPlayer,
    timerDuration: Long
) {
    val options = remember(song) {
        val wrongOptions = allSongs
            .filter { it.id != song.id }
            .shuffled()
            .take(3)

        (wrongOptions + song).shuffled()
    }

    LaunchedEffect(song.id) {
        musicPlayer.playPreview(song.preview)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScoreBoard(
            player1Name = player1Name,
            player2Name = player2Name,
            scoreP1 = scoreP1,
            scoreP2 = scoreP2,
            onPlayer1Click = onPlayer1Click,
            onPlayer2Click = onPlayer2Click
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.label_round_progress_5, round),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        TimerView(
            timeLeft = timeLeft,
            timerDuration = timerDuration
        )

        Spacer(modifier = Modifier.height(24.dp))

        options.forEach { option ->
            Button(
                onClick = { onAnswer(option.id == song.id) },
                enabled = !hasAnswered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = option.title,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (hasAnswered) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.msg_waiting_opponent_answer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ScoreBoard(
    player1Name: String,
    player2Name: String,
    scoreP1: Int,
    scoreP2: Int,
    onPlayer1Click: () -> Unit,
    onPlayer2Click: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = player1Name,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable { onPlayer1Click() }
            )
            Text(
                text = "$scoreP1",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }

        Text(
            text = "VS",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = player2Name,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable { onPlayer2Click() }
            )
            Text(
                text = "$scoreP2",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun TimerView(
    timeLeft: Float,
    timerDuration: Long
) {
    val totalSeconds = timerDuration / 1000f
    val secondsLeft = kotlin.math.ceil(timeLeft * totalSeconds)
        .toInt()
        .coerceIn(0, totalSeconds.toInt())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⏱ ${secondsLeft}s",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                secondsLeft <= 3 -> Color.Red
                secondsLeft <= 7 -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { timeLeft.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50.dp))
        )
    }
}

@Composable
fun ResultView(
    player1Name: String,
    player2Name: String,
    scoreP1: Int,
    scoreP2: Int,
    onPlayer1Click: () -> Unit,
    onPlayer2Click: () -> Unit,
    onExit: () -> Unit,
    viewModel: MatchViewModel = koinViewModel(),
    roundResults: List<RoundResultModel>,
    songs: List<Song> = emptyList(),
    endReason: String = "",
    opponentAbandoned: Boolean = false,
    iAbandoned: Boolean = false,
    opponentLostConnection: Boolean = false,
    iLostConnection: Boolean = false,
    iWinByEndReason: Boolean = false
) {
    val currentPlayerId = viewModel.getCurrentPlayerId()
    val currentMatch = viewModel.currentMatch.collectAsState().value
    val amIPlayer1 = currentMatch?.let { currentPlayerId == it.player1Id } ?: false

    val myScore = if (amIPlayer1) scoreP1 else scoreP2
    val opponentScore = if (amIPlayer1) scoreP2 else scoreP1

    val hasSpecialWinner = endReason == "ABANDONED" || endReason == "CONNECTION_LOST"

    val win = if (hasSpecialWinner) {
        iWinByEndReason
    } else {
        myScore > opponentScore
    }

    val draw = if (hasSpecialWinner) {
        false
    } else {
        myScore == opponentScore
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (endReason == "ABANDONED") {
            Text(
                text = if (opponentAbandoned) {
                    stringResource(R.string.msg_opponent_abandoned)
                } else if (iAbandoned) {
                    stringResource(R.string.msg_you_abandoned)
                } else {
                    stringResource(R.string.msg_match_abandoned)
                },
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (endReason == "CONNECTION_LOST") {
            Text(
                text = if (opponentLostConnection) {
                    stringResource(R.string.msg_opponent_disconnected)
                } else if (iLostConnection) {
                    stringResource(R.string.msg_you_disconnected)
                } else {
                    stringResource(R.string.msg_connection_lost)
                },
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = when {
                win -> stringResource(R.string.msg_victory)
                draw -> stringResource(R.string.msg_draw)
                else -> stringResource(R.string.msg_defeat)
            },
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = when {
                win -> Color(0xFF4CAF50)
                draw -> Color.Gray
                else -> Color.Red
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        ScoreBoard(
            player1Name = player1Name,
            player2Name = player2Name,
            scoreP1 = scoreP1,
            scoreP2 = scoreP2,
            onPlayer1Click = onPlayer1Click,
            onPlayer2Click = onPlayer2Click
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.title_rounds_summary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            roundResults.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.label_round_n, result.round),
                            fontWeight = FontWeight.Bold
                        )

                        val songTitle = songs.getOrNull(result.round - 1)?.let { "${it.title} - ${it.artist}" }
                            ?: result.songTitle.ifBlank { stringResource(R.string.msg_unknown_song_id) }

                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$player1Name: ${if (result.player1Correct) "✔" else "✖"}")
                            Text("$player2Name: ${if (result.player2Correct) "✔" else "✖"}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.btn_back_to_home))
        }
    }
}

@Composable
fun AnswerResultOverlay(isCorrect: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isCorrect) {
                    Color(0xFF4CAF50).copy(alpha = 0.28f)
                } else {
                    Color.Red.copy(alpha = 0.28f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCorrect) "✔" else "✖",
            fontSize = 90.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}