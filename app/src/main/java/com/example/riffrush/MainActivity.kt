package com.example.riffrush

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.riffrush.notifications.DailyChallengeNotificationScheduler
import com.example.riffrush.ui.screens.main.MainScreen
import com.example.riffrush.ui.theme.RiffRushTheme

/*
 Actividad principal de la aplicación.
 Actúa como el punto de entrada de la interfaz de usuario, inicializando las notificaciones,
 aplicando el diseño de pantalla completa (Edge-to-Edge) y cargando el tema dinámico con Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    
    // Registra el callback para solicitar de manera asíncrona el permiso de notificaciones en Android 13+ (API 33+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solicita permiso de notificaciones al usuario si el dispositivo corre Android 13 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Programa el envío de notificaciones diarias para recordar el Reto Diario de música
        DailyChallengeNotificationScheduler.scheduleDailyNotification(this)
        
        // Habilita el modo de pantalla completa nativo, extendiendo los componentes bajo las barras del sistema
        enableEdgeToEdge()

        setContent {
            // Obtenemos el SettingsViewModel para leer la preferencia del tema configurada por el usuario
            val settingsViewModel: com.example.riffrush.viewmodels.SettingsViewModel =
                org.koin.androidx.compose.koinViewModel()

            // Observamos el estado del modo oscuro en tiempo real
            val darkMode by settingsViewModel.darkMode.collectAsState()

            // Aplicamos el tema de la aplicación pasando la configuración leída del usuario
            RiffRushTheme(darkTheme = darkMode) {
                MainScreen()
            }
        }
    }
}