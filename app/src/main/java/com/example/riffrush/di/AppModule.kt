package com.example.riffrush.di

import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.viewmodel.AuthViewModel
import com.example.riffrush.viewmodel.SongViewModel
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    singleOf(::OkHttpClient)
    singleOf(::DeezerRepository)
    singleOf(::AuthRepository)
    singleOf(::MusicPlayer)
    viewModelOf(::SongViewModel)
    viewModelOf(::AuthViewModel)
}
