package com.example.riffrush.data.model

/*
 Representa a un usuario o amigo en el ecosistema de RiffRush.
 Se utiliza para mostrar listados sociales, estadísticas del perfil de otros usuarios y gestionar relaciones (seguidos/seguidores).
 */
data class FriendModel(
    val uid: String = "",              // UID único de Firebase del usuario
    val name: String = "",             // Nombre o nombre de usuario visible
    val pfpUrl: String = "",           // URL de la foto de perfil almacenada en Firebase Storage
    val status: String = "",           // Estado personalizado o biografía breve
    val email: String = "",            // Dirección de correo electrónico del usuario
    val role: String = "user",         // Rol de permisos del usuario: "user" (por defecto) o "admin"
    val isBlocked: Boolean = false,    // Indica si el usuario ha sido bloqueado/baneado por moderación
    val gamesPlayed: Int = 0,          // Contador total de partidas multijugador completadas
    val songsPlayed: Int = 0,          // Contador acumulado de canciones individuales jugadas
    val maxDailyStreak: Int = 0        // Racha máxima de días consecutivos completando el Reto Diario
)