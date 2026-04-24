package com.example.riffrush.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.riffrush.ui.components.UserItem
import com.example.riffrush.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialListScreen(
    type: String,
    onNavigateBack: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onNavigateToUserStats: (String) -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val list by authViewModel.socialList.collectAsState()
    val following by authViewModel.following.collectAsState()
    val isLoggedIn by authViewModel.authState.collectAsState()

    LaunchedEffect(type) {
        authViewModel.loadSocialList(type)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(type) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
    //Añado que si no esta logueado no mostremos nada!
        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Inicia sesión para ver tu lista social",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else if (list.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val emptyMessage = if (type == "Seguidores") {
                    "Aún no tienes seguidores. ¡Comparte tu música para que otros te vean!"
                } else {
                    "No estás siguiendo a nadie todavía. ¡Explora la comunidad!"
                }

                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onNavigateToSocial) {
                    Text("Explorar Página Social")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list) { user ->
                    UserItem(
                        user = user,
                        isFollowing = following.contains(user.uid),
                        onFollowClick = { authViewModel.addFriend(user) },
                        onUserClick = { uid ->
                            onNavigateToUserStats(uid)
                        }
                    )
                }
            }
        }
    }
}