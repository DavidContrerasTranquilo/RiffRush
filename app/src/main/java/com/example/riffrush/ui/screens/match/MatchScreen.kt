package com.example.riffrush.ui.screens.match

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.riffrush.R
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.RoundResultModel
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

import com.example.riffrush.ui.theme.getAppThemeColors

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
    val distractorPool by viewModel.distractorPool.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val timerDuration by viewModel.currentTimerDuration.collectAsState()
    val showResult by viewModel.showAnswerResult.collectAsState()
    val isCorrect by viewModel.lastAnswerCorrect.collectAsState()

    val colors = getAppThemeColors()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = { Text(stringResource(R.string.title_match_1vs1), fontWeight = FontWeight.Bold, color = colors.topBarText) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.abandonCurrentMatch()
                                onExit()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_exit), tint = colors.topBarText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = colors.topBarText,
                        titleContentColor = colors.topBarText,
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
                    LoadingView(colors)
                } else {
                    match?.let { currentMatch ->
                        when (currentMatch.status) {
                            MatchStatus.WAITING -> WaitingOpponentView(colors)

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
                                        if (isOffline) stringResource(R.string.label_rival) else stringResource(R.string.default_username_guest)
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
                                        distractorPool = distractorPool,
                                        isOffline = isOffline,
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
                                        timerDuration = timerDuration,
                                        colors = colors
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
                                    if (isOffline) stringResource(R.string.label_rival) else stringResource(R.string.default_username_guest)
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
                                    iWinByEndReason = iWinByEndReason,
                                    isOffline = isOffline,
                                    colors = colors
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
}

@Composable
fun LoadingView(colors: com.example.riffrush.ui.theme.AppThemeColors) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = colors.accentPrimary,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.msg_tuning_match),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textMain
        )
    }
}

@Composable
fun WaitingOpponentView(colors: com.example.riffrush.ui.theme.AppThemeColors) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = colors.accentPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.msg_waiting_opponent),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textMain,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.msg_music_start_soon),
            color = colors.textMuted
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
    distractorPool: List<Song> = emptyList(),
    isOffline: Boolean = false,
    hasAnswered: Boolean = false,
    timeLeft: Float,
    onAnswer: (Boolean) -> Unit,
    onPlayer1Click: () -> Unit,
    onPlayer2Click: () -> Unit,
    musicPlayer: MusicPlayer,
    timerDuration: Long,
    colors: com.example.riffrush.ui.theme.AppThemeColors
) {
    val options = remember(song, distractorPool) {
        val pastAnsweredIds = allSongs.take(round - 1).map { it.id }.toSet()
        val combinedPool = (allSongs + distractorPool).distinctBy { it.id }
        
        val wrongOptions = combinedPool
            .filter { it.id != song.id && !pastAnsweredIds.contains(it.id) }
            .shuffled()
            .take(3)
            
        val finalWrongOptions = if (wrongOptions.size < 3) {
            val fallback = allSongs.filter { it.id != song.id }.shuffled().take(3 - wrongOptions.size)
            wrongOptions + fallback
        } else wrongOptions

        (finalWrongOptions + song).distinctBy { it.id }.shuffled()
    }

    LaunchedEffect(song.id) {
        musicPlayer.playPreview(song.preview)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isOffline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.label_score_value, scoreP1),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.accentPrimary
                    )
                }
            }
        } else {
            ScoreBoard(
                player1Name = player1Name,
                player2Name = player2Name,
                scoreP1 = scoreP1,
                scoreP2 = scoreP2,
                onPlayer1Click = onPlayer1Click,
                onPlayer2Click = onPlayer2Click,
                colors = colors
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.label_round_progress_5, round),
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // VINYL + TIMER
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Neon Ring Timer
            CircularProgressIndicator(
                progress = { timeLeft.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = when {
                    timeLeft < 0.2f -> Color.Red
                    timeLeft < 0.5f -> Color(0xFFFF9800)
                    else -> colors.accentPrimary
                },
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
                trackColor = colors.cardBorder
            )

            // Spinning Vinyl
            val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RotationAnim"
            )

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(colors.vinylBg)
                    .border(2.dp, colors.cardBorder, CircleShape)
                    .rotate(if (hasAnswered) 0f else rotation),
                contentAlignment = Alignment.Center
            ) {
                // Inner grooves
                Box(modifier = Modifier.size(120.dp).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                Box(modifier = Modifier.size(60.dp).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape))
                
                // If answered, reveal cover!
                if (hasAnswered && song.cover.isNotBlank()) {
                    AsyncImage(
                        model = song.cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(colors.accentPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = colors.accentPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ANSWERS
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                Button(
                    onClick = { onAnswer(option.id == song.id) },
                    enabled = !hasAnswered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.buttonBg,
                        contentColor = colors.buttonText,
                        disabledContainerColor = if (hasAnswered && option.id == song.id) Color(0xFF10B981) else colors.cardBg,
                        disabledContentColor = if (hasAnswered && option.id == song.id) Color.White else colors.textMuted
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (hasAnswered) 0.dp else 2.dp)
                ) {
                    Text(
                        text = option.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (hasAnswered) {
            Text(
                text = stringResource(R.string.msg_waiting_opponent_answer),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ScoreBoard(
    player1Name: String,
    player2Name: String,
    scoreP1: Int,
    scoreP2: Int,
    onPlayer1Click: () -> Unit,
    onPlayer2Click: () -> Unit,
    colors: com.example.riffrush.ui.theme.AppThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = player1Name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                modifier = Modifier.clickable { onPlayer1Click() }
            )
            Text(
                text = "$scoreP1",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = colors.accentPrimary
            )
        }

        Text(
            text = "VS",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentSecondary
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = player2Name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                modifier = Modifier.clickable { onPlayer2Click() }
            )
            Text(
                text = "$scoreP2",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = colors.accentPrimary
            )
        }
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
    iWinByEndReason: Boolean = false,
    isOffline: Boolean = false,
    colors: com.example.riffrush.ui.theme.AppThemeColors
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

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
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
                color = colors.accentSecondary,
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
                color = colors.accentSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = if (isOffline) {
                stringResource(R.string.msg_game_over)
            } else when {
                win -> stringResource(R.string.msg_victory)
                draw -> stringResource(R.string.msg_draw)
                else -> stringResource(R.string.msg_defeat)
            },
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = if (isOffline) colors.accentPrimary else when {
                win -> colors.accentPrimary
                draw -> colors.textMuted
                else -> colors.accentSecondary
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isOffline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.label_score_value, scoreP1),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.accentPrimary
                    )
                }
            }
        } else {
            ScoreBoard(
                player1Name = player1Name,
                player2Name = player2Name,
                scoreP1 = scoreP1,
                scoreP2 = scoreP2,
                onPlayer1Click = onPlayer1Click,
                onPlayer2Click = onPlayer2Click,
                colors = colors
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.title_rounds_summary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textMain
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            roundResults.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.cardBg
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.label_round_n, result.round),
                            fontWeight = FontWeight.Bold,
                            color = colors.accentPrimary
                        )

                        val songTitle = songs.getOrNull(result.round - 1)?.let { "${it.title} - ${it.artist}" }
                            ?: result.songTitle.ifBlank { stringResource(R.string.msg_unknown_song_id) }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMain
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isOffline) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = if (result.player1Correct) "✔ Acertada" else "✖ Fallada",
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.player1Correct) Color(0xFF10B981) else colors.accentSecondary
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "$player1Name: ${if (result.player1Correct) "✔" else "✖"}",
                                    color = if (result.player1Correct) Color(0xFF10B981) else colors.accentSecondary
                                )
                                Text(
                                    "$player2Name: ${if (result.player2Correct) "✔" else "✖"}",
                                    color = if (result.player2Correct) Color(0xFF10B981) else colors.accentSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentPrimary,
                contentColor = Color.Black
            )
        ) {
            Text(stringResource(R.string.btn_back_to_home), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AnswerResultOverlay(isCorrect: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isCorrect) {
                    Color(0xFF10B981).copy(alpha = 0.6f)
                } else {
                    Color(0xFFE11D48).copy(alpha = 0.6f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCorrect) "✔" else "✖",
            fontSize = 120.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}