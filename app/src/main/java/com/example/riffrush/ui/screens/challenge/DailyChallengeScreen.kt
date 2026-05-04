package com.example.riffrush.ui.screens.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodels.DailyChallengeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    onExit: () -> Unit,
    viewModel: DailyChallengeViewModel = koinViewModel(),
    musicPlayer: MusicPlayer = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { musicPlayer.stop() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RETO DIARIO", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Salir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando reto de hoy...")
                    }
                }
                //Añado esto para el tema invitados, que si no se crea un usuario en firebase
                uiState.isCompletedToday || uiState.isGameOver -> {
                    val isGuest = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                    DailyChallengeResultView(
                        score = uiState.score,
                        streak = uiState.streak,
                        isGuest = isGuest,
                        onExit = onExit
                    )
                }
                uiState.songs.isNotEmpty() -> {
                    val currentSong = uiState.songs[uiState.currentRoundIndex]
                    
                    DailyChallengeGameView(
                        song = currentSong,
                        round = uiState.currentRoundIndex + 1,
                        score = uiState.score,
                        allSongs = uiState.songs,
                        hasAnswered = uiState.hasAnsweredCurrent,
                        answerCorrect = uiState.currentAnswerCorrect,
                        onAnswer = { isCorrect ->
                            viewModel.onAnswerSelected(isCorrect)
                        },
                        onNextRound = {
                            viewModel.nextRound()
                        },
                        musicPlayer = musicPlayer
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No se pudo cargar el reto. Intenta más tarde.")
                        Button(onClick = onExit) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
    }
}
//La pantalla de la partida del reto diario
@Composable
fun DailyChallengeGameView(
    song: Song,
    round: Int,
    score: Int,
    allSongs: List<Song>,
    hasAnswered: Boolean,
    answerCorrect: Boolean?,
    onAnswer: (Boolean) -> Unit,
    onNextRound: () -> Unit,
    musicPlayer: MusicPlayer
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Puntuación: $score",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Ronda $round / 6",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        options.forEach { option ->
            val buttonColor = if (hasAnswered) {
                if (option.id == song.id) Color(0xFF4CAF50) // Verde si es la correcta
                else MaterialTheme.colorScheme.surfaceVariant // Gris las demás
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }

            Button(
                onClick = { onAnswer(option.id == song.id) },
                enabled = !hasAnswered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = if (hasAnswered && option.id != song.id) Color.Gray else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = option.title,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        //Cuando se contesta a una cancion, pasa a la siguiente
        if (hasAnswered) {
            LaunchedEffect(hasAnswered) {
                kotlinx.coroutines.delay(1500)
                onNextRound()
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (answerCorrect == true) "¡Correcto!" else "¡Incorrecto!",
                color = if (answerCorrect == true) Color(0xFF4CAF50) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNextRound,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (round == 6) "VER RESULTADOS" else "SIGUIENTE CANCIÓN")
            }
        }
    }
}

@Composable
fun DailyChallengeResultView(
    score: Int,
    streak: Int,
    isGuest: Boolean = false,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¡Reto Completado!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Puntuación de hoy", fontSize = 18.sp)
                Text(
                    text = "$score / 6",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Racha actual", fontSize = 18.sp)
                Text(
                    text = "$streak 🔥",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }
        }

        if (isGuest) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Para guardar tu racha en la nube y competir con amigos, ¡créate una cuenta desde tu Perfil!",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isGuest) 24.dp else 48.dp))

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("VOLVER AL INICIO")
        }
    }
}
