package com.example.riffrush.ui.screens.challenge

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riffrush.R
import com.example.riffrush.data.model.Song
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodels.DailyChallengeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/*
 Pantalla del Reto Diario (Daily Challenge).
 Presenta un juego diario donde los usuarios intentan adivinar las canciones de la jornada.
 Controla la lógica de visualización del juego, el reproductor de avances musicales,
 la carga de estados y el despliegue del resumen de puntuaciones/rachas al completar la actividad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    onExit: () -> Unit,
    // Inyección de dependencias con Koin
    viewModel: DailyChallengeViewModel = koinViewModel(),
    musicPlayer: MusicPlayer = koinInject()
) {
    // Suscripción reactiva al estado del Reto Diario
    val uiState by viewModel.uiState.collectAsState()

    // Detiene la preescucha musical de inmediato cuando la pantalla es destruida o el usuario sale
    DisposableEffect(Unit) {
        onDispose { musicPlayer.stop() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_daily_challenge), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_exit))
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
                // Caso 1: Los datos del reto diario se están descargando desde la base de datos
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.msg_loading_challenge))
                    }
                }

                // Caso 2: El usuario ya completó el juego de hoy o ha agotado sus intentos
                uiState.isCompletedToday || uiState.isGameOver -> {
                    // Verifica si se trata de un usuario anónimo (invitado)
                    val isGuest =
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                    DailyChallengeResultView(
                        score = uiState.score,
                        streak = uiState.streak,
                        isGuest = isGuest,
                        onExit = onExit
                    )
                }

                // Caso 3: Juego activo. Muestra la ronda actual
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

                // Caso 4: Error inesperado de conexión o base de datos vacía
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.msg_error_loading_challenge))
                        Button(onClick = onExit) {
                            Text(stringResource(R.string.btn_back))
                        }
                    }
                }
            }
        }
    }
}

// Vista interna del juego activo para cada ronda del reto diario
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
    // Genera las 4 opciones de respuesta (1 correcta y 3 incorrectas mezcladas aleatoriamente)
    val options = remember(song) {
        val wrongOptions = allSongs
            .filter { it.id != song.id }
            .shuffled()
            .take(3)
        (wrongOptions + song).shuffled()
    }

    // Inicia la reproducción automática del fragmento sonoro cuando cambia la ronda o canción activa
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
        // Marcador de puntuación y progreso de rondas en la parte superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.label_score_value, score),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.label_round_progress, round, allSongs.size),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Despliegue de los botones interactivos con las opciones de respuesta
        options.forEach { option ->
            // Color de respuesta reactivo: verde si es la correcta revelada, grisáceo las demás, o violeta si aún no responde
            val buttonColor = if (hasAnswered) {
                if (option.id == song.id) Color(0xFF4CAF50)
                else MaterialTheme.colorScheme.surfaceVariant
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

        // Bloque informativo revelado tras contestar a la pregunta de la ronda
        if (hasAnswered) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (answerCorrect == true) stringResource(R.string.msg_correct) else stringResource(R.string.msg_incorrect),
                color = if (answerCorrect == true) Color(0xFF4CAF50) else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNextRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (round == allSongs.size) stringResource(R.string.btn_view_results) else stringResource(R.string.btn_next_song))
            }
        }
    }
}

// Vista final del resumen diario de puntuaciones y rachas consecutivas
@Composable
fun DailyChallengeResultView(
    score: Int,
    streak: Int,
    totalRounds: Int = 6,
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
            text = stringResource(R.string.msg_challenge_completed),
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
                Text(stringResource(R.string.label_score_today), fontSize = 18.sp)
                Text(
                    text = "$score / $totalRounds",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Racha consecutiva de juego en días consecutivos (Racha 🔥)
                if (streak >= 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.label_current_streak), fontSize = 18.sp)
                    Text(
                        text = "$streak 🔥",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }

        // Alerta informativa especial para invitados: les advierte de que su racha no se mantendrá permanentemente si no se registran
        if (isGuest) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.msg_guest_save_streak),
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
            Text(stringResource(R.string.btn_back_to_home))
        }
    }
}
