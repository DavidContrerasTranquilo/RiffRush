package com.example.riffrush.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserStatsScreen(
    userId: String,
    onBack: () -> Unit,
    onNavigateToMatchHistory: (String) -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val selectedUser by authViewModel.selectedUser.collectAsState()
    val compatibilityPercent by authViewModel.compatibilityPercent.collectAsState()
    val isOwnProfile = authViewModel.getCurrentUserId() == userId

    LaunchedEffect(userId) {
        authViewModel.loadSelectedUser(userId)

        if (!isOwnProfile) {
            authViewModel.calculateCompatibility(userId)
        } else {
            authViewModel.clearCompatibility()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        selectedUser?.let { user ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserStatsHeader(user = user)

                Spacer(modifier = Modifier.height(28.dp))

                StatCard(
                    icon = Icons.Default.SportsEsports,
                    title = "Partidas jugadas",
                    value = user.gamesPlayed.toString()
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatCard(
                    icon = Icons.Default.Whatshot,
                    title = "Racha máxima reto diario",
                    value = "${user.maxDailyStreak} 🔥"
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatCard(
                    icon = Icons.Default.MusicNote,
                    title = "Canciones escuchadas",
                    value = user.songsPlayed.toString()
                )

                if (!isOwnProfile) {
                    Spacer(modifier = Modifier.height(28.dp))

                    CompatibilitySection(
                        compatibilityPercent = compatibilityPercent
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onNavigateToMatchHistory(userId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver historial de partidas")
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun UserStatsHeader(user: FriendModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user.pfpUrl.isNotBlank()) {
            AsyncImage(
                model = user.pfpUrl,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = user.name.ifBlank { "Usuario" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (user.status.isNotBlank()) {
            Text(
                text = user.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CompatibilitySection(
    compatibilityPercent: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Compatibilidad musical",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (compatibilityPercent == null) {
                CircularProgressIndicator()
            } else {
                LinearProgressIndicator(
                    progress = { compatibilityPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$compatibilityPercent%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = when {
                        compatibilityPercent >= 75 -> "Tenéis gustos muy parecidos"
                        compatibilityPercent >= 40 -> "Tenéis algunas canciones en común"
                        compatibilityPercent > 0 -> "Compatibilidad baja, pero algo compartís"
                        else -> "No tenéis favoritos en común todavía"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

            }
        }
    }
}