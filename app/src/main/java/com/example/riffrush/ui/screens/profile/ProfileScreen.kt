package com.example.riffrush.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.riffrush.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSocialList: (String) -> Unit = {},
    authViewModel: AuthViewModel = koinViewModel()
) {
    val isLoggedIn by authViewModel.authState.collectAsState()
    val uiMessage by authViewModel.uiMessage.collectAsState()
    val userBio by authViewModel.userBio.collectAsState()
    val profileImageUrl by authViewModel.profileImageUrl.collectAsState()

    val songsCount by authViewModel.songsCount.collectAsState()
    val followers by authViewModel.followers.collectAsState()
    val following by authViewModel.following.collectAsState()

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessage()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            authViewModel.loadUserBio()
            authViewModel.loadUserStats()
            authViewModel.loadUserProfileImage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(bottom = 24.dp)
        ) {
            ProfileHeader(
                username = authViewModel.currentUserName,
                bio = if (isLoggedIn && userBio.isNotBlank()) {
                    userBio
                } else {
                    "Inicia sesión para ver tu bio"
                },
                profileImageUrl = profileImageUrl
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatsSection(
                songsCount = songsCount.toString(),
                followersCount = followers.size.toString(),
                followingCount = following.size.toString(),
                onStatClick = { title ->
                    if (isLoggedIn) onNavigateToSocialList(title) else onNavigateToLogin()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Editar Perfil",
                    subtitle = "Nombre, bio y foto",
                    onClick = {
                        if (isLoggedIn) onNavigateToEditProfile() else onNavigateToLogin()
                    }
                )

                ProfileMenuItem(
                    icon = Icons.Default.Favorite,
                    title = "Mis Favoritos",
                    subtitle = "Tus canciones y artistas",
                    onClick = onNavigateToMusic
                )

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Configuración",
                    subtitle = "Privacidad y ajustes de la app",
                    onClick = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoggedIn) {
                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Cerrar Sesión",
                        color = MaterialTheme.colorScheme.error,
                        onClick = { authViewModel.signOut() }
                    )
                } else {
                    ProfileMenuItem(
                        icon = Icons.Default.Login,
                        title = "Iniciar Sesión",
                        subtitle = "Accede a tu cuenta",
                        onClick = onNavigateToLogin
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    username: String,
    bio: String,
    profileImageUrl: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = username.ifBlank { "Invitado" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = bio,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun StatsSection(
    songsCount: String,
    followersCount: String,
    followingCount: String,
    onStatClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(label = "Canciones", value = songsCount)

        StatItem(
            label = "Seguidores",
            value = followersCount,
            onClick = { onStatClick("Seguidores") }
        )

        StatItem(
            label = "Siguiendo",
            value = followingCount,
            onClick = { onStatClick("Siguiendo") }
        )
    }
}

@Composable
fun StatItem(label: String, value: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = label != "Canciones") { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}