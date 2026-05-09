package com.example.riffrush.navigation

import com.example.riffrush.R

sealed class Screen(val route: String, val titleRes: Int, val icon: Int) {
    object Home : Screen("home", R.string.nav_home, R.drawable.home_24px)
    object Social : Screen("social", R.string.nav_social, R.drawable.group_24px)
    object MyMusic : Screen("mymusic", R.string.nav_music, R.drawable.music_note_24px)
    object Profile : Screen("profile", R.string.nav_profile, R.drawable.person_24px)
}

val items = listOf(
    Screen.Home,
    Screen.Social,
    Screen.MyMusic,
    Screen.Profile
)