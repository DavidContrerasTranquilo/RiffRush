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

@Composable
fun MainScreen(
    songViewModel: SongViewModel = koinViewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

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
                      //Añado para saber en que pestaña estamos, ya que funciona un poco raro
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
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
                    }
                )
            }
            
            composable(Screen.MyMusic.route) { MusicScreen() }

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
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToEditProfile = { navController.navigate("edit_profile") },
                    onNavigateToSocialList = { type -> navController.navigate("social_list/$type") }
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
                    onExit = { navController.popBackStack() }
                )
            }

            composable("favorites_game") {
                FavoritesGameScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("login") {
                LoginScreen(
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
        }
    }
}
