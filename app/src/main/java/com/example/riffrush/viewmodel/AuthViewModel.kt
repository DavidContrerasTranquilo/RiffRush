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

    val currentUserName: String get() = repository.getCurrentUserName()

    init {
        if (_authState.value) loadUserBio()
    }

    fun clearMessage() { _uiMessage.value = null }

    fun loadUserBio() {
        viewModelScope.launch {
            _userBio.value = repository.getUserBio()
        }
    }

    fun signUp(email: String, pass: String, username: String) {
        viewModelScope.launch {
            try {
                repository.signUp(email, pass, username)
                _uiMessage.value = "¡Cuenta creada correctamente!"
                _authState.value = true
                loadUserBio()
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
                    repository.updatePassword(newPass)
                }
                _uiMessage.value = "Perfil actualizado correctamente"
                loadUserBio()
            } catch (e: Exception) {
                _uiMessage.value = "Error al actualizar perfil"
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = false
        _userBio.value = ""
    }
}