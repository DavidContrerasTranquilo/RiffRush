package com.example.riffrush

import android.app.Application
import com.example.riffrush.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/*
 Se ejecuta al iniciar la aplicación, antes de que se cree cualquier actividad o servicio.
 Se utiliza principalmente para inicializar servicios globales como el contenedor de inyección de dependencias (Koin).
 */
class RiffRushApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicialización de Koin para la inyección de dependencias en toda la aplicación
        startKoin {
            androidLogger()
            // Proporciona el contexto de la aplicación para que esté disponible para dependencias que lo requieran (como repositorios o bases de datos locales)
            androidContext(this@RiffRushApp)
            // Carga el módulo de dependencias principal definido en AppModule
            modules(appModule)
        }
    }
}
