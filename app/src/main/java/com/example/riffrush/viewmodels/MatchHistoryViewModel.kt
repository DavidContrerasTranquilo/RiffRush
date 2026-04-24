package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.MatchHistoryModel
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchHistoryViewModel(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<MatchHistoryModel>>(emptyList())
    val history: StateFlow<List<MatchHistoryModel>> = _history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadHistory(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _history.value = firebaseRepository.getMatchHistory(userId)
            _isLoading.value = false
        }
    }
}