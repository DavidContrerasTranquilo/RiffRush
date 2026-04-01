package com.example.riffrush.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.riffrush.viewmodel.SongViewModel
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
                                painter = painterResource(id = screen.iconRes),
                                contentDescription = null
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
                HomeScreen()
            }
            composable(Screen.Social.route) { SocialScreen() }
            composable(Screen.MyMusic.route) { MyMusicScreen() }
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
                    onNavigateToLogin = {
                        navController.navigate("login")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }
            composable("login") {
                LoginScreen(
                    onNavigateToSignUp = {
                        navController.navigate("signup")
                    },
                    onLoginSuccess = {
                        navController.navigate("profile") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("signup") {
                SignUpScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSignUpSuccess = {
                        navController.navigate("profile") {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                )
            }
            composable("edit_profile") {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToMusic = { navController.navigate(Screen.MyMusic.route) },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToEditProfile = { navController.navigate("edit_profile") } // Conecta la nueva pantalla
                )
            }
        }
    }
}
