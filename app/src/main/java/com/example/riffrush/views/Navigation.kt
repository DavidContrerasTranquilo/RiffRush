package com.example.riffrush.views

import androidx.annotation.DrawableRes
import com.example.riffrush.R
//Clase que define las pantallas de la app
sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int) {
    object Home : Screen("home", "Principal", R.drawable.home_24px)
    object Social : Screen("social", "Social", R.drawable.group_24px)
    object MyMusic : Screen("my_music", "Mi Música", R.drawable.music_note_24px)
    object Profile : Screen("profile", "Perfil", R.drawable.person_24px)
}
//Lista de pantallas
val items = listOf(
    Screen.Home,
    Screen.Social,
    Screen.MyMusic,
    Screen.Profile
)
