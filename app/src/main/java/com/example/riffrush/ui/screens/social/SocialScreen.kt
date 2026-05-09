package com.example.riffrush.ui.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.riffrush.R
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.ui.components.UserItem
import com.example.riffrush.viewmodels.AuthViewModel
import com.example.riffrush.viewmodels.MatchViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
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
    var selectedTab by remember { mutableIntStateOf(0) }

    val incomingChallenges by matchViewModel.incomingChallenges.collectAsState()
    val sentChallenge by matchViewModel.sentChallengeState.collectAsState()
    
    var selectedUserForChallenge by remember { mutableStateOf<FriendModel?>(null) }

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
        matchViewModel.startListeningChallenges()
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessage()
        }
    }

    val currentMatch by matchViewModel.currentMatch.collectAsState()
    var hasNavigatedToMatch by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentMatch) {
        currentMatch?.let {
            if ((it.status == com.example.riffrush.data.model.MatchStatus.WAITING || 
                 it.status == com.example.riffrush.data.model.MatchStatus.PLAYING) && 
                 hasNavigatedToMatch != it.matchId) {
                hasNavigatedToMatch = it.matchId
                onNavigateToMatch(it.matchId)
            }
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
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }

                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.hint_search_users)) },
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
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_friends)) }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_explore)) }
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
                        EmptySocialMessage(stringResource(R.string.msg_login_to_see_users))
                    }

                    users.isEmpty() -> {
                        EmptySocialMessage(stringResource(R.string.msg_no_users_available))
                    }

                    isSearching -> {
                        UserList(
                            users = filteredUsers,
                            following = following,
                            emptyMessage = stringResource(R.string.msg_no_users_found),
                            onUserClick = onNavigateToUserStats,
                            onFollowClick = { user -> authViewModel.addFriend(user) },
                            onMatchClick = { user ->
                                selectedUserForChallenge = user
                            }
                        )
                    }

                    selectedTab == 0 -> {
                        UserList(
                            users = mutualFriends,
                            following = following,
                            emptyMessage = stringResource(R.string.msg_no_mutual_friends),
                            onUserClick = onNavigateToUserStats,
                            onFollowClick = { user -> authViewModel.addFriend(user) },
                            onMatchClick = { user ->
                                selectedUserForChallenge = user
                            }
                        )
                    }

                    selectedTab == 1 -> {
                        UserList(
                            users = exploreUsers,
                            following = following,
                            emptyMessage = stringResource(R.string.msg_no_users_to_explore),
                            onUserClick = onNavigateToUserStats,
                            onFollowClick = { user -> authViewModel.addFriend(user) },
                            onMatchClick = { user ->
                                selectedUserForChallenge = user
                            }
                        )
                    }
                }
            }
        }
        
        // --- DIALOGS ---

        // 1. Selector de Playlist
        if (selectedUserForChallenge != null) {
            AlertDialog(
                onDismissRequest = { selectedUserForChallenge = null },
                title = { Text("Elegir Playlist") },
                text = {
                    Column {
                        Text("Retando a ${selectedUserForChallenge?.name}")
                        Spacer(modifier = Modifier.height(16.dp))
                        val playlists = listOf("REGGAETON", "RAP FRANCÉS", "ROCK ESPAÑOL", "TOP 50 ESPAÑA")
                        playlists.forEach { playlist ->
                            Text(
                                text = playlist,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        matchViewModel.sendChallenge(
                                            toUid = selectedUserForChallenge!!.uid,
                                            toName = selectedUserForChallenge!!.name,
                                            playlist = playlist
                                        )
                                        selectedUserForChallenge = null
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedUserForChallenge = null }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // 2. Esperando respuesta
        if (sentChallenge != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Esperando respuesta...") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Esperando a que ${sentChallenge?.toName} acepte...")
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {
                    TextButton(onClick = { matchViewModel.cancelSentChallenge() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // 3. Retos entrantes
        if (incomingChallenges.isNotEmpty()) {
            val challenge = incomingChallenges.first()
            AlertDialog(
                onDismissRequest = { matchViewModel.declineChallenge(challenge.challengeId) },
                title = { Text("¡Nuevo Reto!") },
                text = {
                    Text("${challenge.fromName} te ha retado a jugar en ${challenge.playlist}")
                },
                confirmButton = {
                    TextButton(onClick = { matchViewModel.acceptChallenge(challenge) }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { matchViewModel.declineChallenge(challenge.challengeId) }) {
                        Text("Rechazar")
                    }
                }
            )
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