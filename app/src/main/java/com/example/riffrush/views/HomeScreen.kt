package com.example.riffrush.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riffrush.viewmodel.MatchViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.text.style.TextAlign

@Composable
fun HomeScreen(
    matchViewModel: MatchViewModel = koinViewModel(),
    onNavigateToMatch: (String) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val currentMatch by matchViewModel.currentMatch.collectAsState()

    LaunchedEffect(currentMatch) {
        currentMatch?.let {
            if (it.matchId == "offline_test_id") {
                onNavigateToMatch(it.matchId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF6A00F4), Color(0xFF9C27B0))
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = "RiffRush",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("PLAYLIST DEL DÍA", style = MaterialTheme.typography.labelLarge, color = Color(0xFF7B1FA2))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Trivia del Retro", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Pon a prueba tus oídos con tus canciones preferidas.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToFavorites() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                        ) {
                            Text("Jugar con favoritos")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Partida Rápida", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickButton("Rock 90s", Color(0xFFF8BBD0))
                    QuickButton("Pop Actual", Color(0xFFBBDEFB))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickButton("Indie España", Color(0xFFFFE0B2))
                    QuickButton("Aleatorio", Color(0xFFC8E6C9))
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text("NOVEDADES", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("¡NUEVO!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Modo de Juego Offline",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("¿Sin conexión? No hay problema. Entrena tus habilidades contra nuestra IA en el modo de prueba local.")
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onNavigateToMatch("offline_test_id") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("PROBAR AHORA")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Espacio para la BottomBar
        }
    }
}

@Composable
fun RowScope.QuickButton(text: String, color: Color) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(90.dp),
        color = color,
        shape = RoundedCornerShape(16.dp),
        onClick = { /* Acción rápida */ }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}