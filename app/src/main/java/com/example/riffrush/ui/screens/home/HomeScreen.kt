package com.example.riffrush.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.riffrush.viewmodels.MatchViewModel
import com.example.riffrush.viewmodels.DailyChallengeViewModel
import org.koin.androidx.compose.koinViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
data class PlaylistOption(
    val displayName: String,
    val onlineValue: String,
    val offlineRouteSuffix: String
)

@Composable
fun HomeScreen(
    navController: NavHostController,
    matchViewModel: MatchViewModel = koinViewModel(),
    //Añado el reto diario
    dailyChallengeViewModel: DailyChallengeViewModel = koinViewModel(),
    onNavigateToMatch: (String) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val currentMatch by matchViewModel.currentMatch.collectAsState()
    //Consigo el estado del reto diario
    val dailyChallengeState by dailyChallengeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedPlaylist by remember { mutableStateOf<PlaylistOption?>(null) }

    val reggaeton = PlaylistOption(
        displayName = "Reggaetón",
        onlineValue = "REGGAETON",
        offlineRouteSuffix = "reggaeton"
    )

    val rapFrances = PlaylistOption(
        displayName = "Rap Français",
        onlineValue = "RAP FRANCÉS",
        offlineRouteSuffix = "rap_frances"
    )

    val rockEspanol = PlaylistOption(
        displayName = "Rock Español",
        onlineValue = "ROCK ESPAÑOL",
        offlineRouteSuffix = "rock_espanol"
    )

    val top50Espana = PlaylistOption(
        displayName = "Top 50 España",
        onlineValue = "TOP 50 ESPAÑA",
        offlineRouteSuffix = "top_50_espana"
    )

    LaunchedEffect(currentMatch) {
        currentMatch?.let {
            if (it.matchId == "offline_test_id") {
                onNavigateToMatch(it.matchId)
            }
        }
    }

    if (selectedPlaylist != null) {
        PlaylistModeDialog(
            playlistName = selectedPlaylist!!.displayName,
            onOnlineClick = {
                navController.navigate("matchmaking/${selectedPlaylist!!.onlineValue}")
                selectedPlaylist = null
            },
            onOfflineClick = {
                navController.navigate("match/offline_${selectedPlaylist!!.offlineRouteSuffix}")
                selectedPlaylist = null
            },
            onDismiss = {
                selectedPlaylist = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF6A00F4), Color(0xFF9C27B0))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "RiffRush",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "PLAYLIST DEL DÍA",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF7B1FA2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Trivia del Retro",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Pon a prueba tus oídos con tus canciones preferidas.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToFavorites() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                        ) {
                            Text("Jugar con favoritos")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Partida por Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickButton("Reggaetón", Color(0xFFF8BBD0)) {
                        selectedPlaylist = reggaeton
                    }
                    QuickButton("Rap Français", Color(0xFFBBDEFB)) {
                        selectedPlaylist = rapFrances
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickButton("Rock Español", Color(0xFFFFE0B2)) {
                        selectedPlaylist = rockEspanol
                    }
                    QuickButton("Top 50 España", Color(0xFFC8E6C9)) {
                        selectedPlaylist = top50Espana
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "RETO DIARIO",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    //Aqui construyo la pestaña, podria sacarse a otro compsable quiza?
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "¡GANA LA RACHA!",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Racha: ${dailyChallengeState.streak} 🔥",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Adivina 6 canciones",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        //Si esta completado pues eso
                        Text(
                            if (dailyChallengeState.isCompletedToday) 
                                "Ya has completado el reto de hoy. ¡Vuelve mañana para continuar tu racha!"
                            else 
                                "6 canciones, 1 intento por canción. ¿Podrás adivinarlas todas y mantener tu racha?"
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { 
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser == null || currentUser.uid.startsWith("guest")) {
                                    Toast.makeText(context, "Debes iniciar sesión para jugar al Reto Diario", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (!dailyChallengeState.isCompletedToday) {
                                        navController.navigate("daily_challenge")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !dailyChallengeState.isCompletedToday
                        ) {
                            Text(if (dailyChallengeState.isCompletedToday) "COMPLETADO" else "JUGAR AHORA")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun PlaylistModeDialog(
    playlistName: String,
    onOnlineClick: () -> Unit,
    onOfflineClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Elegir modo de juego")
        },
        text = {
            Text("Has seleccionado \"$playlistName\". ¿Quieres jugar online o offline?")
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOnlineClick) {
                    Text("Online")
                }
                Button(onClick = onOfflineClick) {
                    Text("Offline")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RowScope.QuickButton(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(90.dp),
        color = color,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}