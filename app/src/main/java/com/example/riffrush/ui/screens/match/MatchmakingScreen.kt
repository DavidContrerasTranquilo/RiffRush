package com.example.riffrush.ui.screens.match

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.riffrush.data.model.MatchmakingUiState
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MatchmakingScreen(
    playlist: String,
    onMatched: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MatchViewModel = koinViewModel()
) {
    val matchmakingState by viewModel.matchmakingState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.joinPlaylistMatchmaking(playlist)
    }

    LaunchedEffect(matchmakingState) {
        val state = matchmakingState
        if (state is MatchmakingUiState.Matched) {
            onMatched(state.matchId)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = matchmakingState) {
                is MatchmakingUiState.Error -> {
                    Text(
                        text = "Oops! Ocurrió un error:",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Buscando rival para $playlist")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Espera mientras encontramos una partida")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    viewModel.cancelMatchmaking()
                    onBack()
                }
            ) {
                Text(if (matchmakingState is MatchmakingUiState.Error) "Volver" else "Cancelar")
            }
        }
    }
}