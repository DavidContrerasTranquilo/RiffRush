package com.example.riffrush.ui.screens.match

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import android.view.HapticFeedbackConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    matchId: String,
    onExit: () -> Unit,
    viewModel: MatchViewModel = koinViewModel(),
    musicPlayer: MusicPlayer = koinInject()
) {
    val match by viewModel.currentMatch.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()

    LaunchedEffect(matchId) {
        if (matchId.startsWith("offline_")) {
            val rawCategory = matchId.substringAfter("offline_")
            val category = when(rawCategory) {
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
                title = { Text("DUELO 1VS1", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Salir")
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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

                                val rawPlayer1Name = currentMatch.player1Name.ifBlank { "Invitado" }
                                val rawPlayer2Name = currentMatch.player2Name.ifBlank { "Invitado" }

                                val player1DisplayName = if (amIPlayer1) "$rawPlayer1Name (Tú)" else rawPlayer1Name
                                val player2DisplayName = if (!amIPlayer1) "$rawPlayer2Name (Tú)" else rawPlayer2Name

                                val hasAnswered = if (amIPlayer1) currentMatch.answeredP1 else currentMatch.answeredP2

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
                                    musicPlayer = musicPlayer
                                )
                            }
                        }
                        MatchStatus.FINISHED -> {
                            val currentPlayerId = viewModel.getCurrentPlayerId()
                            val amIPlayer1 = currentPlayerId == currentMatch.player1Id

                            val rawPlayer1Name = currentMatch.player1Name.ifBlank { "Invitado" }
                            val rawPlayer2Name = currentMatch.player2Name.ifBlank { "Invitado" }

                            val player1DisplayName = if (amIPlayer1) "$rawPlayer1Name (Tú)" else rawPlayer1Name
                            val player2DisplayName = if (!amIPlayer1) "$rawPlayer2Name (Tú)" else rawPlayer2Name

                            ResultView(
                                player1Name = player1DisplayName,
                                player2Name = player2DisplayName,
                                scoreP1 = currentMatch.scoreP1,
                                scoreP2 = currentMatch.scoreP2,
                                onExit = onExit
                            )
                        }
                    }
                }
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
        Text("Sintonizando la partida...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun WaitingOpponentView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Esperando al oponente...", style = MaterialTheme.typography.headlineSmall)
        Text("La música empezará pronto", color = Color.Gray)
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
    timeLeft: Float = 1f,
    onAnswer: (Boolean) -> Unit,
    musicPlayer: MusicPlayer
) {
    val options = remember(song) {
        val wrongOptions = allSongs.filter { it.id != song.id }.shuffled().take(3)
        (wrongOptions + song).shuffled()
    }

    LaunchedEffect(song.id) {
        musicPlayer.playPreview(song.preview)
    }

    val view = LocalView.current
    var selectedOptionId by remember(song.id) { mutableStateOf<Long?>(null) }

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
            scoreP2 = scoreP2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BARRA DE TIEMPO
        val animatedProgress by animateFloatAsState(
            targetValue = timeLeft,
            label = "timer",
            // Un poco más "natural" que el lineal por defecto si fuera un juego real
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                timeLeft > 0.6f -> Color(0xFF4CAF50)
                timeLeft > 0.3f -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "RONDA $round / 5",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold, // Un toque más agresivo
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        IconButton(
            onClick = { musicPlayer.playPreview(song.preview) },
            modifier = Modifier.size(100.dp)
        ) {
            PlayMusicIndicator()
        }

        Spacer(modifier = Modifier.height(32.dp))

        options.forEach { option ->
            val isCorrect = option.id == song.id
            val isSelected = selectedOptionId == option.id
            
            val buttonColor by animateColorAsState(
                targetValue = when {
                    hasAnswered && isCorrect -> Color(0xFF4CAF50)
                    isSelected && !isCorrect -> Color(0xFFE57373)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }, label = "btn_color"
            )

            Button(
                onClick = { 
                    selectedOptionId = option.id
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onAnswer(isCorrect) 
                },
                enabled = !hasAnswered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = if (hasAnswered && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = buttonColor,
                    disabledContentColor = if (hasAnswered && (isCorrect || isSelected)) Color.White else Color.Gray.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    option.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (hasAnswered) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Esperando al rival...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScoreBoard(
    player1Name: String,
    player2Name: String,
    scoreP1: Int,
    scoreP2: Int
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
            Text(player1Name, style = MaterialTheme.typography.labelSmall)
            Text("$scoreP1", fontSize = 32.sp, fontWeight = FontWeight.Black)
        }

        Text("VS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(player2Name, style = MaterialTheme.typography.labelSmall)
            Text("$scoreP2", fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PlayMusicIndicator() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Reproducir audio",
            modifier = Modifier.size(60.dp),
            tint = Color.White
        )
    }
}

@Composable
fun ResultView(
    player1Name: String,
    player2Name: String,
    scoreP1: Int,
    scoreP2: Int,
    onExit: () -> Unit
) {
    val win = scoreP1 > scoreP2
    val draw = scoreP1 == scoreP2

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                win -> "¡VICTORIA!"
                draw -> "¡EMPATE!"
                else -> "DERROTA..."
            },
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = if (win) Color(0xFF4CAF50) else if (draw) Color.Gray else Color.Red
        )

        Spacer(modifier = Modifier.height(24.dp))

        ScoreBoard(
            player1Name = player1Name,
            player2Name = player2Name,
            scoreP1 = scoreP1,
            scoreP2 = scoreP2
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("VOLVER AL INICIO")
        }
    }
}