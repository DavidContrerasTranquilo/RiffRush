package com.example.riffrush.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.riffrush.ui.components.UserItem
import com.example.riffrush.viewmodels.AuthViewModel
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SocialScreen(
    authViewModel: AuthViewModel = koinViewModel(),
    matchViewModel: MatchViewModel = koinViewModel(),
    onNavigateToMatch: (String) -> Unit,
    onNavigateToUserStats: (String) -> Unit
) {
    val users by authViewModel.allUsers.collectAsState()
    val following by authViewModel.following.collectAsState()
    val isLoggedIn by authViewModel.authState.collectAsState()
    val currentMatch by matchViewModel.currentMatch.collectAsState()

    // CORRECCIÓN: Navegación controlada mediante un Effect
    LaunchedEffect(currentMatch) {
        currentMatch?.let {
            onNavigateToMatch(it.matchId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Comunidad RiffRush",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            !isLoggedIn -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Inicia sesión para ver a otros usuarios")
                }
            }
            users.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() // Mejor visualmente que solo texto
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users) { user ->
                        UserItem(
                            user = user,
                            onUserClick = { uid ->
                                onNavigateToUserStats(uid)
                            },
                            isFollowing = following.contains(user.uid),
                            onFollowClick = { authViewModel.addFriend(user) },
                            onMatchClick = { matchViewModel.startMatchmaking(user.uid) }
                        )
                    }
                }
            }
        }
    }
}