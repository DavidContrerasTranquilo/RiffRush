package com.example.riffrush.ui.screens.match

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.FavoritesGameViewModel
import com.example.riffrush.ui.screens.challenge.DailyChallengeGameView
import com.example.riffrush.ui.screens.challenge.DailyChallengeResultView
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesGameScreen(
    onBack: () -> Unit,
    musicViewModel: MusicViewModel = koinViewModel(),
    favoritesViewModel: FavoritesGameViewModel = koinViewModel(),
    musicPlayer: MusicPlayer = koinInject()
) {
    val favorites by musicViewModel.favoriteSongs.collectAsState()
    val playingSongs by favoritesViewModel.playingSongs.collectAsState()
    val currentIndex by favoritesViewModel.currentIndex.collectAsState()
    val score by favoritesViewModel.score.collectAsState()
    val gameState by favoritesViewModel.gameState.collectAsState()
    val hasAnswered by favoritesViewModel.hasAnsweredCurrent.collectAsState()
    val answerCorrect by favoritesViewModel.currentAnswerCorrect.collectAsState()

    // Inicializamos la partida con los favoritos cargados
    LaunchedEffect(favorites) {
        if (favorites.isNotEmpty() && playingSongs.isEmpty()) {
            favoritesViewModel.initGame(favorites)
        }
    }

    // Parar música al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.stop()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MODO FAVORITOS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                favorites.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No tienes canciones favoritas", style = MaterialTheme.typography.headlineSmall)
                        Text("Añade algunas en la sección de música", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                gameState == FavoritesGameViewModel.FavoritesState.FINISHED -> {
                    DailyChallengeResultView(
                        score = score,
                        streak = -1,
                        totalRounds = playingSongs.size,
                        onExit = onBack
                    )
                }

                else -> {
                    val currentSong = playingSongs.getOrNull(currentIndex)
                    if (currentSong != null) {
                        DailyChallengeGameView(
                            song = currentSong,
                            round = currentIndex + 1,
                            score = score,
                            allSongs = playingSongs,
                            hasAnswered = hasAnswered,
                            answerCorrect = answerCorrect,
                            onAnswer = { isCorrect ->
                                favoritesViewModel.submitAnswer(isCorrect)
                            },
                            onNextRound = {
                                favoritesViewModel.nextRound()
                            },
                            musicPlayer = musicPlayer
                        )
                    }
                }
            }
        }
    }
}