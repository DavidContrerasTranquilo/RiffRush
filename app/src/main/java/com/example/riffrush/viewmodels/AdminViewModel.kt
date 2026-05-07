package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.DailyChallengeConfig
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<FriendModel>>(emptyList())
    val users: StateFlow<List<FriendModel>> = _users

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = authRepository.getAllUsersForAdmin()
        }
    }

    fun blockUser(uid: String) {
        viewModelScope.launch {
            try {
                authRepository.setUserBlocked(uid, true)
                _uiMessage.value = "Usuario bloqueado"
                loadUsers()
            } catch (e: Exception) {
                _uiMessage.value = "Error al bloquear usuario"
            }
        }
    }

    fun unblockUser(uid: String) {
        viewModelScope.launch {
            try {
                authRepository.setUserBlocked(uid, false)
                _uiMessage.value = "Usuario desbloqueado"
                loadUsers()
            } catch (e: Exception) {
                _uiMessage.value = "Error al desbloquear usuario"
            }
        }
    }

    fun clearProfilePicture(uid: String) {
        viewModelScope.launch {
            try {
                authRepository.clearUserProfilePicture(uid)
                _uiMessage.value = "Foto eliminada"
                loadUsers()
            } catch (e: Exception) {
                _uiMessage.value = "Error al eliminar foto"
            }
        }
    }

    fun saveDailyChallenge(
        date: String,
        songIds: List<String>,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                if (date.isBlank()) {
                    _uiMessage.value = "La fecha no puede estar vacía"
                    return@launch
                }

                if (songIds.size != 6) {
                    _uiMessage.value = "Debes indicar exactamente 6 canciones"
                    return@launch
                }

                val config = DailyChallengeConfig(
                    date = date,
                    songIds = songIds,
                    isActive = isActive
                )

                firebaseRepository.saveDailyChallengeConfig(config)
                _uiMessage.value = "Reto diario guardado correctamente"
            } catch (e: Exception) {
                _uiMessage.value = "Error al guardar el reto diario"
            }
        }
    }

    fun deleteDailyChallenge(date: String) {
        viewModelScope.launch {
            try {
                if (date.isBlank()) {
                    _uiMessage.value = "La fecha no puede estar vacía"
                    return@launch
                }

                firebaseRepository.deleteDailyChallengeConfig(date)
                _uiMessage.value = "Reto diario eliminado"
            } catch (e: Exception) {
                _uiMessage.value = "Error al eliminar el reto diario"
            }
        }
    }

    fun showMessage(message: String) {
        _uiMessage.value = message
    }
}