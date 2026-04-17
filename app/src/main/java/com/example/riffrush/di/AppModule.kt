package com.example.riffrush.di

import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.MatchRepository
import com.example.riffrush.viewmodels.AuthViewModel
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.SongViewModel
import com.example.riffrush.viewmodels.MatchViewModel
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.example.riffrush.data.repository.FirebaseRepository
import org.koin.android.ext.koin.androidContext
import com.example.riffrush.data.repository.DeezerApi
import com.example.riffrush.viewmodels.FavoritesGameViewModel

val appModule = module {

    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }

    singleOf(::FirebaseRepository)
    singleOf(::OkHttpClient)

    single {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(get())
            .build()
    }
    single<DeezerApi> {
        get<Retrofit>().create(DeezerApi::class.java)
    }

    singleOf(::DeezerRepository)
    singleOf(::AuthRepository)
    singleOf(::MatchRepository)

    singleOf(::MusicPlayer)
    single { com.example.riffrush.data.local.GuestIdManager(androidContext()) }

    viewModelOf(::SongViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::MusicViewModel)
    viewModelOf(::MatchViewModel)
    viewModelOf(::FavoritesGameViewModel)
}