package com.example.riffrush.ui.screens.match

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.riffrush.R
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.ui.screens.challenge.DailyChallengeGameView
import com.example.riffrush.ui.screens.challenge.DailyChallengeResultView
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
    val hasAnswered by gameViewModel.hasAnsweredCurrent.collectAsState()
    val answerCorrect by gameViewModel.currentAnswerCorrect.collectAsState()

    LaunchedEffect(playlistId) {
        playlistViewModel.loadPlaylistSongs(playlistId)
    }

    LaunchedEffect(playlistId, songs) {
        if (songs.size >= 4) {
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
                title = { Text(stringResource(R.string.title_playlist_game), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
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

                songs.size < 4 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.msg_need_4_songs_playlist))
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
        Text(stringResource(R.string.msg_loading_playlist_songs))
    }
}