package com.example.riffrush.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.riffrush.data.local.dao.MatchHistoryDao
import com.example.riffrush.data.local.dao.SongDao
import com.example.riffrush.data.local.entity.MatchHistoryEntity
import com.example.riffrush.data.local.entity.SongEntity

/*
 Base de datos local Room de RiffRush.
 Actúa como caché persistente en SQLite para las entidades de canciones favoritas
 e historial de partidas, permitiendo acceso instantáneo offline (Single Source of Truth).
 La versión se incrementa cada vez que se modifique la estructura de las tablas.
 */
@Database(
    entities = [SongEntity::class, MatchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RiffRushDatabase : RoomDatabase() {

    // Provee acceso al DAO de canciones favoritas
    abstract fun songDao(): SongDao

    // Provee acceso al DAO de historial de partidas
    abstract fun matchHistoryDao(): MatchHistoryDao
}
