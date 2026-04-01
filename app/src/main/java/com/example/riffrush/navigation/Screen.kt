package com.example.riffrush.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Social : Screen("social", "Social", Icons.Default.People)
    object MyMusic : Screen("mymusic", "Mi Música", Icons.Default.LibraryMusic)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
}

val items = listOf(
    Screen.Home,
    Screen.Social,
    Screen.MyMusic,
    Screen.Profile
)