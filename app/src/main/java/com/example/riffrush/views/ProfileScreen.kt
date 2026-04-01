package com.example.riffrush.views

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.riffrush.viewmodel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    authViewModel: AuthViewModel = koinViewModel()
) {
    val isLoggedIn by authViewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        ProfileHeader()

        Spacer(modifier = Modifier.height(16.dp))

        StatsSection()

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
                subtitle = "Nombre, bio y foto"
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

            Text(
                text = "Más",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "Ayuda y Soporte"
            )

            if (isLoggedIn) {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Cerrar Sesión",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        authViewModel.signOut()
                    }
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

@Composable
fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "John Joe",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Explorando nuevos ritmos desde 2024",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun StatsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(label = "Playlists", value = "12")
        StatItem(label = "Seguidores", value = "156")
        StatItem(label = "Siguiendo", value = "89")
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
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