package com.example.riffrush.di

import com.example.riffrush.data.local.GuestIdManager
import com.example.riffrush.data.local.PlayerIdentityManager
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.*
import com.example.riffrush.viewmodels.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.riffrush.data.local.SettingsDataStore
val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single {
        FirebaseDatabase.getInstance(
            "https://riffrush-229d4-default-rtdb.europe-west1.firebasedatabase.app"
        )
    }

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

    single { GuestIdManager(androidContext()) }
    singleOf(::PlayerIdentityManager)
    singleOf(::MusicPlayer)

    singleOf(::FirebaseRepository)
    single {
        AuthRepository(
            firebaseAuth = get(),
            firestore = get(),
            context = androidContext(),
            okHttpClient = get()
        )
    }
    singleOf(::DeezerRepository)
    singleOf(::MatchRepository)
    single { SettingsDataStore(androidContext()) }
    viewModelOf(::AuthViewModel)
    viewModelOf(::MusicViewModel)
    viewModelOf(::MatchViewModel)
    viewModelOf(::FavoritesGameViewModel)
    viewModelOf(::MatchHistoryViewModel)
    viewModelOf(::DailyChallengeViewModel)
    viewModelOf(::AdminViewModel)
    viewModelOf(::PlaylistViewModel)
    viewModelOf(::SettingsViewModel)
}