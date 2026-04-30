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
                    ResultView(
                        player1Name = "Tú",
                        player2Name = "Playlist",
                        scoreP1 = score,
                        scoreP2 = 0,
                        onPlayer1Click = {},
                        onPlayer2Click = {},
                        onExit = onBack
                    )
                }

                else -> {
                    val currentSong = playingSongs.getOrNull(currentIndex)

                    if (currentSong != null) {
                        GameView(
                            song = currentSong,
                            round = currentIndex + 1,
                            player1Name = "Tú",
                            player2Name = "Playlist",
                            scoreP1 = score,
                            scoreP2 = 0,
                            allSongs = songs,
                            hasAnswered = false,
                            timeLeft = 1f,
                            onAnswer = { isCorrect ->
                                gameViewModel.submitAnswer(isCorrect)
                            },
                            onPlayer1Click = {},
                            onPlayer2Click = {},
                            musicPlayer = musicPlayer,
                            timerDuration = 15000L,
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