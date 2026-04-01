package com.example.riffrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.riffrush.ui.theme.RiffRushTheme
import com.example.riffrush.viewmodel.SongViewModel
import com.example.riffrush.views.MainScreen
import com.example.riffrush.views.HomeScreen
import org.koin.androidx.viewmodel.ext.android.viewModel
/*
class MainActivity : ComponentActivity() {
    private val songViewModel: SongViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            //RiffRushTheme {
                // Lógica de Login comentada para uso futuro, de momento iremos haciendo
                // var showLogin by remember { mutableStateOf(true) }

               /* Scaffold { innerPadding ->
                    // if (showLogin) {
                    //    LoginScreen(
                    //        onLoginSuccess = { showLogin = false }
                    //    )
                    // } else {

                    SongPreviewScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSearch = { songName ->
                            songViewModel.searchAndPlay(songName)
                        }
                    )*/
                    MainScreen()
                    }
                }
            }
        }
    }
}
*/
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