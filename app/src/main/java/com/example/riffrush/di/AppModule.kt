package com.example.riffrush.di

import com.example.riffrush.data.local.GuestIdManager
import com.example.riffrush.data.local.PlayerIdentityManager
import com.example.riffrush.data.local.SettingsDataStore
import com.example.riffrush.data.player.MusicPlayer
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.DeezerApi
import com.example.riffrush.data.repository.DeezerRepository
import com.example.riffrush.data.repository.FirebaseRepository
import com.example.riffrush.data.repository.MatchRepository
import com.example.riffrush.viewmodels.AdminViewModel
import com.example.riffrush.viewmodels.AuthViewModel
import com.example.riffrush.viewmodels.DailyChallengeViewModel
import com.example.riffrush.viewmodels.FavoritesGameViewModel
import com.example.riffrush.viewmodels.MatchHistoryViewModel
import com.example.riffrush.viewmodels.MatchViewModel
import com.example.riffrush.viewmodels.MusicViewModel
import com.example.riffrush.viewmodels.PlaylistViewModel
import com.example.riffrush.viewmodels.SettingsViewModel
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