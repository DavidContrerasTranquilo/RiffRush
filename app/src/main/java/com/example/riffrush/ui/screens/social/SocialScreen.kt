package com.example.riffrush.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.ui.components.UserItem
import com.example.riffrush.viewmodels.AuthViewModel
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    authViewModel: AuthViewModel = koinViewModel(),
    matchViewModel: MatchViewModel = koinViewModel(),
    onNavigateToMatch: (String) -> Unit,
    onNavigateToUserStats: (String) -> Unit
) {
    val users by authViewModel.allUsers.collectAsState()
    val following by authViewModel.following.collectAsState()
    val followers by authViewModel.followers.collectAsState()
    val isLoggedIn by authViewModel.authState.collectAsState()
    val currentMatch by matchViewModel.currentMatch.collectAsState()
    val uiMessage by authViewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    // Filtrado de usuarios para la búsqueda
    val filteredUsers = users.filter { it.name.contains(query, ignoreCase = true) }
    // Amigos mutuos: yo le sigo y él me sigue
    val mutualFriends = users.filter { following.contains(it.uid) && followers.contains(it.uid) }

    // Navegación controlada mediante un Effect
    LaunchedEffect(currentMatch) {
        currentMatch?.let {
            onNavigateToMatch(it.matchId)
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda al estilo MusicScreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    IconButton(
                        onClick = {
                            query = ""
                            isSearching = false
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }

                TextField(
                    value = query,
                    onValueChange = { 
                        query = it 
                        if (it.isBlank()) isSearching = false
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar usuarios...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) {
                            isSearching = true
                        }
                    })
                )
            }

            if (!isSearching) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Amigos") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Explorar") }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                when {
                    !isLoggedIn -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Inicia sesión para ver a otros usuarios")
                        }
                    }
                    users.isEmpty() && !isSearching -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    isSearching -> {
                        UserList(
                            users = filteredUsers,
                            following = following,
                            onNavigateToUserStats = onNavigateToUserStats,
                            authViewModel = authViewModel,
                            matchViewModel = matchViewModel
                        )
                    }
                    selectedTab == 0 -> {
                        UserList(
                            users = mutualFriends,
                            following = following,
                            onNavigateToUserStats = onNavigateToUserStats,
                            authViewModel = authViewModel,
                            matchViewModel = matchViewModel
                        )
                    }
                    selectedTab == 1 -> {
                        val exploreUsers = users.filterNot { user ->
                            mutualFriends.any { it.uid == user.uid }
                        }
                        UserList(
                            users = exploreUsers,
                            following = following,
                            onNavigateToUserStats = onNavigateToUserStats,
                            authViewModel = authViewModel,
                            matchViewModel = matchViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserList(
    users: List<FriendModel>,
    following: List<String>,
    onNavigateToUserStats: (String) -> Unit,
    authViewModel: AuthViewModel,
    matchViewModel: MatchViewModel
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron usuarios")
        }
    } else {
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
