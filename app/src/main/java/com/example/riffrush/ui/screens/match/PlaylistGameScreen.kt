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
import com.example.riffrush.viewmodels.FavoritesGameViewModel
import com.example.riffrush.viewmodels.PlaylistViewModel
import com.example.riffrush.ui.screens.challenge.DailyChallengeGameView
import com.example.riffrush.ui.screens.challenge.DailyChallengeResultView
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistGameScreen(
    playlistId: String,
    onBack: () -> Unit,
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    gameViewModel: FavoritesGameViewModel = koinViewModel(),
    musicPlayer: MusicPlayer = koinInject()
) {
    val songs by playlistViewModel.selectedPlaylistSongs.collectAsState()
    val playingSongs by gameViewModel.playingSongs.collectAsState()
    val currentIndex by gameViewModel.currentIndex.collectAsState()
    val score by gameViewModel.score.collectAsState()
    val gameState by gameViewModel.gameState.collectAsState()
    val hasAnswered by gameViewModel.hasAnsweredCurrent.collectAsState()
    val answerCorrect by gameViewModel.currentAnswerCorrect.collectAsState()

    LaunchedEffect(playlistId) {
        playlistViewModel.loadPlaylistSongs(playlistId)
    }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty() && playingSongs.isEmpty()) {
            gameViewModel.initGame(songs)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicPlayer.stop()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PLAYLIST GAME", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
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
                songs.isEmpty() -> {
                    EmptyPlaylistGameView()
                }

                songs.size < 2 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Necesitas al menos 2 canciones para jugar")
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
                                gameViewModel.submitAnswer(isCorrect)
                            },
                            onNextRound = {
                                gameViewModel.nextRound()
                            },
                            musicPlayer = musicPlayer
                        )
                    } else {
                        EmptyPlaylistGameView()
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaylistGameView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Cargando canciones de la playlist...")
    }
}