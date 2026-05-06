package com.example.riffrush.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
    val uiMessage by authViewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val isSearching = query.isNotBlank()

    val filteredUsers = remember(users, query) {
        users.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.status.contains(query, ignoreCase = true)
        }
    }

    val mutualFriends = remember(users, following, followers) {
        users.filter { user ->
            following.contains(user.uid) && followers.contains(user.uid)
        }
    }

    val exploreUsers = remember(users, mutualFriends) {
        users.filterNot { user ->
            mutualFriends.any { friend -> friend.uid == user.uid }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.loadAllUsers()
        authViewModel.loadUserStats()
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    IconButton(
                        onClick = { query = "" }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }

                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar usuarios...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {})
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when {
                    !isLoggedIn -> {
                        EmptySocialMessage("Inicia sesión para ver a otros usuarios")
                    }

                    users.isEmpty() -> {
                        EmptySocialMessage("No hay usuarios disponibles")
                    }

                    isSearching -> {
                        UserList(
                            users = filteredUsers,
                            following = following,
                            emptyMessage = "No se encontraron usuarios",
                            onUserClick = onNavigateToUserStats,
                            onFollowClick = { user -> authViewModel.addFriend(user) },
                            onMatchClick = { user ->
                                matchViewModel.startMatchmaking(user.uid)
                            }
                        )
                    }

                    selectedTab == 0 -> {
                        UserList(
                            users = mutualFriends,
                            following = following,
                            emptyMessage = "Aún no tienes amigos mutuos",
                            onUserClick = onNavigateToUserStats,
                            onFollowClick = { user -> authViewModel.addFriend(user) },
                            onMatchClick = { user ->
                                matchViewModel.startMatchmaking(user.uid)
                            }
                        )
                    }

                    selectedTab == 1 -> {
                        UserList(
                            users = exploreUsers,
                            following = following,
                            emptyMessage = "No hay usuarios para explorar",
                            onUserClick = onNavigateToUserStats,
                            onFollowClick = { user -> authViewModel.addFriend(user) },
                            onMatchClick = { user ->
                                matchViewModel.startMatchmaking(user.uid)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<FriendModel>,
    following: List<String>,
    emptyMessage: String,
    onUserClick: (String) -> Unit,
    onFollowClick: (FriendModel) -> Unit,
    onMatchClick: (FriendModel) -> Unit
) {
    if (users.isEmpty()) {
        EmptySocialMessage(emptyMessage)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = users,
                key = { it.uid }
            ) { user ->
                UserItem(
                    user = user,
                    onUserClick = onUserClick,
                    isFollowing = following.contains(user.uid),
                    onFollowClick = { onFollowClick(user) },
                    onMatchClick = { onMatchClick(user) }
                )
            }
        }
    }
}

@Composable
private fun EmptySocialMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message)
    }
}