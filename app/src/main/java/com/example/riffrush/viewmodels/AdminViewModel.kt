package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val authRepository: AuthRepository
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
}