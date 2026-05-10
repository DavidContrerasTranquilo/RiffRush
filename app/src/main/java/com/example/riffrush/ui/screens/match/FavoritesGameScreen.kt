package com.example.riffrush.ui.screens.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.riffrush.R
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.ui.screens.challenge.DailyChallengeGameView
import com.example.riffrush.ui.screens.challenge.DailyChallengeResultView
import com.example.riffrush.viewmodels.FavoritesGameViewModel
import com.example.riffrush.viewmodels.MusicViewModel
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

    var gameInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(favorites) {
        if (!gameInitialized && favorites.size >= 4) {
            favoritesViewModel.initGame(favorites)
            gameInitialized = true
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
                title = { Text(stringResource(R.string.title_favorites_mode), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                favorites.size < 4 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.msg_need_4_favorites),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            stringResource(R.string.msg_add_more_songs),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                playingSongs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.msg_preparing_favorites))
                        }
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
                    val distractorPool by favoritesViewModel.distractorPool.collectAsState()

                    if (currentSong != null) {
                        DailyChallengeGameView(
                            song = currentSong,
                            round = currentIndex + 1,
                            score = score,
                            allSongs = playingSongs,
                            distractorPool = distractorPool,
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