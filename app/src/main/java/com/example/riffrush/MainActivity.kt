package com.example.riffrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.riffrush.ui.theme.RiffRushTheme
import com.example.riffrush.views.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RiffRushTheme {
                MainScreen()
            }
        }
    }
}
