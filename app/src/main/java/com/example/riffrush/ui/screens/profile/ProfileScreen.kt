package com.example.riffrush.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.riffrush.ui.theme.getAppThemeColors
import com.example.riffrush.R
import coil.compose.AsyncImage
import com.example.riffrush.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSocialList: (String) -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onNavigateToUserStats: (String) -> Unit = {},
    onNavigateToMatchHistory: (String) -> Unit = {},
    onNavigateToWelcome: () -> Unit = {},
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
    val userRole by authViewModel.userRole.collectAsState()
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessage()
        }
    }

    val colors = getAppThemeColors()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            authViewModel.loadUserBio()
            authViewModel.loadUserStats()
            authViewModel.loadUserProfileImage()
            authViewModel.loadModerationState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = colors.topBarText
                    ),
                    title = {
                        Text(
                            text = "Perfil",
                            fontWeight = FontWeight.Bold,
                            color = colors.topBarText
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(bottom = 24.dp)
            ) {
                ProfileHeader(
                    username = authViewModel.currentUserName,
                    bio = if (isLoggedIn && userBio.isNotBlank()) {
                        userBio
                    } else {
                        stringResource(R.string.msg_login_to_see_bio)
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
                        text = stringResource(R.string.section_account),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.menu_edit_profile),
                        subtitle = stringResource(R.string.menu_edit_profile_desc),
                        onClick = {
                            if (isLoggedIn) onNavigateToEditProfile() else onNavigateToLogin()
                        }
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.BarChart,
                        title = stringResource(R.string.menu_stats),
                        subtitle = stringResource(R.string.menu_stats_desc),
                        onClick = {
                            if (isLoggedIn) {
                                onNavigateToUserStats(authViewModel.getCurrentUserId())
                            } else {
                                onNavigateToLogin()
                            }
                        }
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.menu_match_history),
                        subtitle = stringResource(R.string.menu_match_history_desc),
                        onClick = {
                            if (isLoggedIn) {
                                onNavigateToMatchHistory(authViewModel.getCurrentUserId())
                            } else {
                                onNavigateToLogin()
                            }
                        }
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.menu_favorites),
                        subtitle = stringResource(R.string.menu_favorites_desc),
                        onClick = onNavigateToMusic
                    )

                    if (userRole == "admin") {
                        ProfileMenuItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = stringResource(R.string.menu_admin),
                            subtitle = stringResource(R.string.menu_admin_desc),
                            onClick = onNavigateToAdminPanel
                        )
                    }

                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.menu_settings),
                        subtitle = stringResource(R.string.menu_settings_desc),
                        onClick = onNavigateToSettings
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoggedIn) {
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = stringResource(R.string.menu_logout),
                            color = MaterialTheme.colorScheme.error,
                            onClick = {
                                //Le envio al inico de sesion/registro
                                authViewModel.signOut()
                                onNavigateToWelcome()
                            }
                        )
                    } else {
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Login,
                            title = stringResource(R.string.menu_login),
                            subtitle = stringResource(R.string.menu_login_desc),
                            onClick = onNavigateToLogin
                        )
                    }
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
                contentDescription = stringResource(R.string.content_desc_profile_image),
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
            text = username.ifBlank { stringResource(R.string.default_username_guest) },
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
        StatItem(label = stringResource(R.string.stat_songs), value = songsCount)

        StatItem(
            label = stringResource(R.string.stat_followers),
            value = followersCount,
            onClick = { onStatClick("followers") }
        )

        StatItem(
            label = stringResource(R.string.stat_following),
            value = followingCount,
            onClick = { onStatClick("following") }
        )
    }
}

@Composable
fun StatItem(label: String, value: String, onClick: () -> Unit = {}) {
    val songsLabel = stringResource(R.string.stat_songs)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = label != songsLabel) { onClick() }
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