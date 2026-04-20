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
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("AUTH_DEBUG", "Login anónimo OK: ${it.user?.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("AUTH_DEBUG", "Error login anónimo", e)
                }
        } else {
            Log.d("AUTH_DEBUG", "Usuario ya logueado: ${auth.currentUser?.uid}")
        }


        startKoin {
            androidLogger()
            androidContext(this@RiffRushApp)
            modules(appModule)
        }
    }
}
