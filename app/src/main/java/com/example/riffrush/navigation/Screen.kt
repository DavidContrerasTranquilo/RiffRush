package com.example.riffrush.navigation

import com.example.riffrush.R

sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Home : Screen("home", "Inicio", R.drawable.home_24px)
    object Social : Screen("social", "Social", R.drawable.group_24px)
    object MyMusic : Screen("mymusic", "Mi Música", R.drawable.music_note_24px)
    object Profile : Screen("profile", "Perfil", R.drawable.person_24px)
}

val items = listOf(
    Screen.Home,
    Screen.Social,
    Screen.MyMusic,
    Screen.Profile
)