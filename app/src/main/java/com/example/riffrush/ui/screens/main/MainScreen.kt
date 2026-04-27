package com.example.riffrush.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.riffrush.navigation.Screen
import com.example.riffrush.navigation.items
import com.example.riffrush.viewmodels.SongViewModel
import com.example.riffrush.ui.screens.auth.LoginScreen
import com.example.riffrush.ui.screens.auth.SignUpScreen
import com.example.riffrush.ui.screens.home.HomeScreen
import com.example.riffrush.ui.screens.match.FavoritesGameScreen
import com.example.riffrush.ui.screens.match.MatchScreen
import com.example.riffrush.ui.screens.music.MusicScreen
import com.example.riffrush.ui.screens.profile.EditProfileScreen
import com.example.riffrush.ui.screens.profile.ProfileScreen
import com.example.riffrush.ui.screens.settings.SettingsScreen
import com.example.riffrush.ui.screens.social.SocialListScreen
import com.example.riffrush.ui.screens.social.SocialScreen
import org.koin.androidx.compose.koinViewModel
import com.example.riffrush.ui.screens.match.MatchmakingScreen
import com.example.riffrush.ui.screens.profile.UserStatsScreen
import com.example.riffrush.ui.screens.profile.MatchHistoryScreen
import com.example.riffrush.ui.screens.music.PlaylistDetailScreen
import com.example.riffrush.ui.screens.match.PlaylistGameScreen
@Composable
fun MainScreen(
    songViewModel: SongViewModel = koinViewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route
            //Cuando estamos no estamos en la pestaña d elogin la mostramos
            //Lo dejo asi porque no tenia sentido que pudieses ver la parte de abajo de la barra cuando estas inciado sesion
            if (currentRoute != "login" && currentRoute != "signup") {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(id = screen.icon),
                                    contentDescription = null
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            alwaysShowLabel = false,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    onNavigateToMatch = { matchId ->
                        navController.navigate("match/$matchId")
                    },
                    onNavigateToFavorites = {
                        navController.navigate("favorites_game")
                    }
                )
            }

            composable(Screen.Social.route) {
                SocialScreen(
                    onNavigateToMatch = { matchId ->
                        navController.navigate("match/$matchId")
                    },
                    onNavigateToUserStats = { uid ->
                        navController.navigate("user_stats/$uid")
                    }
                )
            }

            composable(Screen.MyMusic.route) {
                MusicScreen(
                    onNavigateToPlaylistDetail = { playlistId, playlistName ->
                        navController.navigate(
                            "playlist_detail/$playlistId/${android.net.Uri.encode(playlistName)}"
                        )
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToMusic = {
                        navController.navigate(Screen.MyMusic.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAdminPanel = { navController.navigate("admin_panel") },
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToEditProfile = { navController.navigate("edit_profile") },
                    onNavigateToSocialList = { type -> navController.navigate("social_list/$type") } ,
                    onNavigateToUserStats = { uid ->
                        navController.navigate("user_stats/$uid")
                    },
                    onNavigateToMatchHistory = { uid ->
                        navController.navigate("match_history/$uid")
                    },
                )
            }

            composable("settings") {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }

            composable("edit_profile") {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                "social_list/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "Social"
                SocialListScreen(
                    type = type,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSocial = {
                        navController.navigate(Screen.Social.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    },
                    onNavigateToUserStats = { uid ->
                        navController.navigate("user_stats/$uid")
                    }
                )
            }

            composable(
                "match/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                MatchScreen(
                    matchId = matchId,
                    onExit = { navController.popBackStack() },
                    onNavigateToUserStats = { uid ->
                        navController.navigate("user_stats/$uid")
                    }
                )
            }

            composable("favorites_game") {
                FavoritesGameScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        //El reto diario !
            composable("daily_challenge") {
                com.example.riffrush.ui.screens.challenge.DailyChallengeScreen(
                    onExit = { navController.popBackStack() }
                )
            }

            composable("login") {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSignUp = { navController.navigate("signup") },
                    onLoginSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("signup") {
                SignUpScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSignUpSuccess = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                "matchmaking/{playlist}",
                arguments = listOf(navArgument("playlist") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlist = backStackEntry.arguments?.getString("playlist") ?: "REGGAETON"

                MatchmakingScreen(
                    playlist = playlist,
                    onMatched = { matchId ->
                        navController.navigate("match/$matchId") {
                            popUpTo("matchmaking/$playlist") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("admin_panel") {
                com.example.riffrush.ui.screens.admin.AdminPanelScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("user_stats/{uid}") { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: ""
                UserStatsScreen(
                    userId = uid,
                    onBack = { navController.popBackStack() },
                    onNavigateToMatchHistory = {
                        navController.navigate("match_history/$uid")
                    }
                )
            }
            composable("match_history/{uid}") { backStackEntry ->
                MatchHistoryScreen(
                    userId = backStackEntry.arguments?.getString("uid") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "playlist_detail/{playlistId}/{playlistName}",
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.StringType },
                    navArgument("playlistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                val playlistName = android.net.Uri.decode(
                    backStackEntry.arguments?.getString("playlistName") ?: "Playlist"
                )
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistName = playlistName,
                    onBack = { navController.popBackStack() },
                    onPlayPlaylist = { id ->
                        navController.navigate("playlist_game/$id")
                    }
                )
            }

            composable(
                "playlist_game/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                PlaylistGameScreen(
                    playlistId = backStackEntry.arguments?.getString("playlistId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

        }
    }
}
