package com.example.riffrush.views

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodel.MatchViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    val currentRound by viewModel.currentRound.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.listenToMatch(matchId)
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
                            val currentSong = songs.getOrNull(currentRound)
                            if (currentSong != null) {
                                GameView(
                                    song = currentSong,
                                    round = currentRound + 1,
                                    scoreP1 = currentMatch.scoreP1,
                                    scoreP2 = currentMatch.scoreP2,
                                    allSongs = songs,
                                    onAnswer = { isCorrect -> 
                                        viewModel.onAnswerSelected(isCorrect)
                                    },
                                    musicPlayer = musicPlayer
                                )
                            }
                        }
                        MatchStatus.FINISHED -> ResultView(
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
    scoreP1: Int,
    scoreP2: Int,
    allSongs: List<Song>,
    onAnswer: (Boolean) -> Unit,
    musicPlayer: MusicPlayer
) {
    // Generar opciones (1 correcta + 3 aleatorias)
    val options = remember(song) {
        val wrongOptions = allSongs.filter { it.id != song.id }.shuffled().take(3)
        (wrongOptions + song).shuffled()
    }

    LaunchedEffect(song) {
        musicPlayer.playPreview(song.preview)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Marcador
        ScoreBoard(scoreP1, scoreP2)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Info Ronda
        Text("RONDA $round / 5", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Animación de música sonando
        MusicWaves()

        Spacer(modifier = Modifier.height(48.dp))

        // Opciones
        options.forEach { option ->
            Button(
                onClick = { onAnswer(option.id == song.id) },
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(option.title, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ScoreBoard(s1: Int, s2: Int) {
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
            Text("TÚ", style = MaterialTheme.typography.labelSmall)
            Text("$s1", fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
        Text("VS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RIVAL", style = MaterialTheme.typography.labelSmall)
            Text("$s2", fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MusicWaves() {
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
        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
    }
}

@Composable
fun ResultView(scoreP1: Int, scoreP2: Int, onExit: () -> Unit) {
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

        ScoreBoard(scoreP1, scoreP2)

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("VOLVER AL INICIO")
        }
    }
}