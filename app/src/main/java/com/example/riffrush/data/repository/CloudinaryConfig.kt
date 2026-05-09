package com.example.riffrush.data.repository

import com.example.riffrush.BuildConfig

/*
 Configuración global del servicio de almacenamiento Cloudinary.
 Lee los tokens de configuración expuestos de manera segura mediante variables de Gradle (BuildConfig).
 */
object CloudinaryConfig {
    // Nombre del cloud asignado a la cuenta de Cloudinary
    val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    // Preset de subida configurado para permitir subidas asíncronas no firmadas desde el dispositivo móvil
    val UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET
}