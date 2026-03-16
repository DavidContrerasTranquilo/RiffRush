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

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            try {
                repository.signUp(email, pass)
                _authState.value = true
            } catch (e: Exception) {
                _authState.value = false
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            try {
                repository.signIn(email, pass)
                _authState.value = true
            } catch (e: Exception) {
                _authState.value = false
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _authState.value = false
    }
}
