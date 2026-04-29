package com.example.riffrush

import android.app.Application
import com.example.riffrush.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class RiffRushApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@RiffRushApp)
            modules(appModule)
        }
    }
}
