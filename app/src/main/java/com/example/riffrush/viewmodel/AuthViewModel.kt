package com.example.riffrush.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _authState = MutableStateFlow<Boolean>(repository.getCurrentUser() != null)
    val authState: StateFlow<Boolean> = _authState

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private val _userBio = MutableStateFlow("")
    val userBio: StateFlow<String> = _userBio

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    // --- NUEVOS ESTADOS PARA ESTADÍSTICAS ---
    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount

    private val _followers = MutableStateFlow<List<String>>(emptyList())
    val followers: StateFlow<List<String>> = _followers

    private val _following = MutableStateFlow<List<String>>(emptyList())
    val following: StateFlow<List<String>> = _following

    val currentUserName: String get() = repository.getCurrentUserName()

    init {
        if (_authState.value) {
            loadUserBio()
            loadUserStats()
        }
    }

    fun clearMessage() { _uiMessage.value = null }
    fun resetUpdateStatus() { _updateSuccess.value = false }

    fun loadUserBio() {
        viewModelScope.launch {
            _userBio.value = repository.getUserBio()
        }
    }

    fun loadUserStats() {
        viewModelScope.launch {
            try {
                // Suponiendo que en tu repositorio implementas estos métodos
                _songsCount.value = repository.getUserSongsCount()
                // De momento devolvemos listas vacías como pediste
                _followers.value = emptyList()
                _following.value = emptyList()
            } catch (e: Exception) {
                _songsCount.value = 0
            }
        }
    }

    fun signUp(email: String, pass: String, username: String) {
        viewModelScope.launch {
            try {
                repository.signUp(email, pass, username)
                _uiMessage.value = "¡Cuenta creada correctamente!"
                _authState.value = true
                loadUserBio()
                loadUserStats()
            } catch (e: Exception) {
                _uiMessage.value = "Error en el registro"
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            try {
                repository.signIn(email, pass)
                _uiMessage.value = "Sesión iniciada correctamente"
                _authState.value = true
                loadUserBio()
                loadUserStats()
            } catch (e: Exception) {
                _uiMessage.value = "Email o contraseña incorrectos"
            }
        }
    }

    fun updateFullProfile(newName: String, newBio: String, newPass: String?) {
        viewModelScope.launch {
            try {
                repository.updateUsername(newName)
                repository.updateBio(newBio)
                if (!newPass.isNullOrBlank()) {
                    try {
                        repository.updatePassword(newPass)
                    } catch (e: Exception) {
                        _uiMessage.value = "Para cambiar contraseña debes haber iniciado sesión recientemente."
                        return@launch
                    }
                }
                _uiMessage.value = "Perfil actualizado correctamente"
                loadUserBio()
                _updateSuccess.value = true
            } catch (e: Exception) {
                _uiMessage.value = "Error al actualizar datos: ${e.message}"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                repository.signOut()
                _authState.value = false
                _userBio.value = ""
                _songsCount.value = 0
                _uiMessage.value = "Sesión cerrada correctamente"
            } catch (e: Exception) {
                _uiMessage.value = "Error al cerrar sesión"
            }
        }
    }
}