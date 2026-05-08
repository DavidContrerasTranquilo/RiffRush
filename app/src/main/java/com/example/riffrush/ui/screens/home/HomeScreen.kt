package com.example.riffrush.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.stringResource
import com.example.riffrush.R
import com.example.riffrush.viewmodels.AuthViewModel
import com.example.riffrush.viewmodels.DailyChallengeViewModel
import com.example.riffrush.viewmodels.MatchViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel

data class PlaylistOption(
    val displayName: String,
    val onlineValue: String,
    val offlineRouteSuffix: String
)

@Composable
fun HomeScreen(
    navController: NavHostController,
    matchViewModel: MatchViewModel = koinViewModel(),
    dailyChallengeViewModel: DailyChallengeViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    onNavigateToMatch: (String) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val currentMatch by matchViewModel.currentMatch.collectAsState()
    val dailyChallengeState by dailyChallengeViewModel.uiState.collectAsState()
    val isBlocked by authViewModel.isBlocked.collectAsState()

    val context = LocalContext.current

    var selectedPlaylist by remember { mutableStateOf<PlaylistOption?>(null) }

    val reggaeton = PlaylistOption(
        displayName = stringResource(R.string.category_reggaeton),
        onlineValue = "REGGAETON",
        offlineRouteSuffix = "reggaeton"
    )

    val rapFrances = PlaylistOption(
        displayName = stringResource(R.string.category_rap_frances),
        onlineValue = "RAP FRANCÉS",
        offlineRouteSuffix = "rap_frances"
    )

    val rockEspanol = PlaylistOption(
        displayName = stringResource(R.string.category_rock_espanol),
        onlineValue = "ROCK ESPAÑOL",
        offlineRouteSuffix = "rock_espanol"
    )

    val top50Espana = PlaylistOption(
        displayName = stringResource(R.string.category_top_50_espana),
        onlineValue = "TOP 50 ESPAÑA",
        offlineRouteSuffix = "top_50_espana"
    )

    LaunchedEffect(Unit) {
        authViewModel.loadModerationState()
    }

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
                if (isBlocked) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_account_blocked),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    navController.navigate("matchmaking/${selectedPlaylist!!.onlineValue}")
                }
                selectedPlaylist = null
            },
            onOfflineClick = {
                if (isBlocked) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_account_blocked),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    navController.navigate("match/offline_${selectedPlaylist!!.offlineRouteSuffix}")
                }
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
                text = stringResource(R.string.app_name),
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
                            text = stringResource(R.string.home_playlist_of_day_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF7B1FA2)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.home_playlist_of_day_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(stringResource(R.string.home_playlist_of_day_desc))

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isBlocked) {
                                    Toast.makeText(
                                        context,
                                        "Tu cuenta está bloqueada y no puede jugar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    //Le decimos que si no tiene cuenta no puede jugar con los favoritos (que inicies sesion si quiere)
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser == null || currentUser.isAnonymous) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.error_login_required_favorites),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onNavigateToFavorites()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B1FA2)
                            )
                        ) {
                            Text(stringResource(R.string.btn_play_favorites))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.home_match_by_playlist_label),
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
                    QuickButton(reggaeton.displayName, Color(0xFFF8BBD0)) {
                        if (isBlocked) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_account_blocked),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            selectedPlaylist = reggaeton
                        }
                    }

                    QuickButton(rapFrances.displayName, Color(0xFFBBDEFB)) {
                        if (isBlocked) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_account_blocked),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            selectedPlaylist = rapFrances
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickButton(rockEspanol.displayName, Color(0xFFFFE0B2)) {
                        if (isBlocked) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_account_blocked),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            selectedPlaylist = rockEspanol
                        }
                    }

                    QuickButton(top50Espana.displayName, Color(0xFFC8E6C9)) {
                        if (isBlocked) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_account_blocked),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            selectedPlaylist = top50Espana
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.home_daily_challenge_label),
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
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_daily_challenge_win_streak),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.home_daily_challenge_streak, dailyChallengeState.streak),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.home_daily_challenge_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (dailyChallengeState.isCompletedToday) {
                                stringResource(R.string.home_daily_challenge_completed_desc)
                            } else {
                                stringResource(R.string.home_daily_challenge_desc)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isBlocked) {
                                    Toast.makeText(
                                        context,
                                        "Tu cuenta está bloqueada y no puede jugar",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                            Text(
                                if (dailyChallengeState.isCompletedToday) {
                                    stringResource(R.string.btn_completed)
                                } else {
                                    stringResource(R.string.btn_play_now)
                                }
                            )
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
            Text(stringResource(R.string.dialog_choose_game_mode_title))
        },
        text = {
            Text(stringResource(R.string.dialog_choose_game_mode_desc, playlistName))
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOnlineClick) {
                    Text(stringResource(R.string.btn_online))
                }
                Button(onClick = onOfflineClick) {
                    Text(stringResource(R.string.btn_offline))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun RowScope.QuickButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
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