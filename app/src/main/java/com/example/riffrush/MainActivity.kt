package com.example.riffrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.riffrush.ui.theme.RiffRushTheme
import com.example.riffrush.ui.screens.main.MainScreen
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.riffrush.notifications.DailyChallengeNotificationScheduler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
class MainActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        DailyChallengeNotificationScheduler.scheduleDailyNotification(this)
        enableEdgeToEdge()

        setContent {
            val settingsViewModel: com.example.riffrush.viewmodels.SettingsViewModel =
                org.koin.androidx.compose.koinViewModel()

            val darkMode by settingsViewModel.darkMode.collectAsState()

            RiffRushTheme(darkTheme = darkMode) {
                MainScreen()
            }
        }
    }
}