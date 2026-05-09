package com.example.riffrush.data.model

/*
 Configuración para el Reto Diario de música (Daily Challenge).
 Almacena la fecha del reto actual, la lista de canciones seleccionadas para ese día y su estado.
 */
data class DailyChallengeConfig(
    val date: String = "",                     // Fecha del reto diario con formato "yyyy-MM-dd"
    val songIds: List<String> = emptyList(),   // Lista de IDs de canciones oficiales de Deezer seleccionadas para ese día
    val isActive: Boolean = true               // Determina si el reto está activo y accesible para jugar
)