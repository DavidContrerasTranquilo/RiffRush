package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.local.dao.MatchHistoryDao
import com.example.riffrush.data.local.entity.toEntity
import com.example.riffrush.data.local.entity.toModel
import com.example.riffrush.data.model.MatchHistoryModel
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

/*
 ViewModel del Historial de Partidas con patrón SSOT (Single Source of Truth).
 Lee instantáneamente desde la caché local de Room y sincroniza con Firestore en segundo plano.
 */
class MatchHistoryViewModel(
    private val firebaseRepository: FirebaseRepository,
    private val matchHistoryDao: MatchHistoryDao    // DAO de Room para caché local del historial
) : ViewModel() {

    private val _history = MutableStateFlow<List<MatchHistoryModel>>(emptyList())
    val history: StateFlow<List<MatchHistoryModel>> = _history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadHistory(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Paso 1: Cargar instantáneamente desde Room (caché local)
            val cached = matchHistoryDao.getHistoryByUserOnce(userId)
            if (cached.isNotEmpty()) {
                _history.value = cached.map { it.toModel() }
                _isLoading.value = false
            }

            // Paso 2: Sincronizar desde Firestore en segundo plano
            try {
                val cloudHistory = firebaseRepository.getMatchHistory(userId)
                _history.value = cloudHistory

                // Actualizar la caché local de Room con los datos frescos de la nube
                val entities = cloudHistory.map { it.toEntity() }
                matchHistoryDao.insertAll(entities)
            } catch (e: Exception) {
                Log.e("MatchHistoryVM", "Error sincronizando historial desde Firestore", e)
                // Si falla la nube, los datos cacheados siguen visibles
            }

            _isLoading.value = false
        }
    }
}