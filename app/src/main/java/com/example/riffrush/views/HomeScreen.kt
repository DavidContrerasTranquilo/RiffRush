package com.example.riffrush.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF6A00F4), Color(0xFF9C27B0))
                    )
                ),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "RiffRush",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PLAYLIST DEL DÍA", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Trivia del Retro")
                        Text("Pon a prueba tus oídos con hits clásicos")
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(onClick = { }) {
                            Text("Jugar ahora")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Partida Rápida")
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickButton("Rock 90s", Color(0xFFF8BBD0))
                    QuickButton("Pop Actual", Color(0xFFBBDEFB))
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickButton("Indie España", Color(0xFFFFE0B2))
                    QuickButton("Aleatorio", Color(0xFFC8E6C9))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("NOVEDADES")
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Exitos de la decada de los 2010")
                        Text("Descubre nuestra nueva playlist")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { }) {
                            Text("Jugar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.QuickButton(text: String, color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(80.dp)
            .background(color, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}