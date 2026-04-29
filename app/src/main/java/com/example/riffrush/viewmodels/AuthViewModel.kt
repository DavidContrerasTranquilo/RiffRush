package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.first


class AuthViewModel(
    private val repository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private val _authState = MutableStateFlow<Boolean>(repository.getCurrentUser()?.isAnonymous == false)
    val authState: StateFlow<Boolean> = _authState

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private val _userBio = MutableStateFlow("")
    val userBio: StateFlow<String> = _userBio

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount

    private val _followers = MutableStateFlow<List<String>>(emptyList())
    val followers: StateFlow<List<String>> = _followers

    private val _following = MutableStateFlow<List<String>>(emptyList())
    val following: StateFlow<List<String>> = _following

    private val _allUsers = MutableStateFlow<List<FriendModel>>(emptyList())
    val allUsers: StateFlow<List<FriendModel>> = _allUsers

    private val _socialList = MutableStateFlow<List<FriendModel>>(emptyList())
    val socialList: StateFlow<List<FriendModel>> = _socialList

    val currentUserName: String get() = repository.getCurrentUserName()
    private val _profileImageUrl = MutableStateFlow("")
    val profileImageUrl: StateFlow<String> = _profileImageUrl
    private val _userRole = MutableStateFlow("user")
    val userRole: StateFlow<String> = _userRole

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked
    private val _compatibility = MutableStateFlow<Int?>(null)
    val compatibility: StateFlow<Int?> = _compatibility
    private val _selectedUser = MutableStateFlow<FriendModel?>(null)
    val selectedUser: StateFlow<FriendModel?> = _selectedUser

    private val _compatibilityPercent = MutableStateFlow<Int?>(null)
    val compatibilityPercent: StateFlow<Int?> = _compatibilityPercent

    init {
        if (_authState.value) {
            loadUserBio()
            loadUserStats()
            loadAllUsers()
            loadUserProfileImage()
            loadModerationState()
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
        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                _songsCount.value = repository.getUserSongsCount()
                val followersList = repository.getSocialList(uid, "Seguidores")
                val followingList = repository.getSocialList(uid, "Siguiendo")
                
                _followers.value = followersList.map { it.uid }
                _following.value = followingList.map { it.uid }
            } catch (e: Exception) {
                _songsCount.value = 0
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsers.value = repository.getAllUsers()
        }
    }

    fun loadSocialList(type: String) {
        val uid = repository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _socialList.value = repository.getSocialList(uid, type)
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
                loadAllUsers()
                loadUserProfileImage()
                loadModerationState()
            } catch (e: Exception) {
                _uiMessage.value = "Error en el registro: ${e.localizedMessage}"
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
                loadAllUsers()
                loadUserProfileImage()
                loadModerationState()
            } catch (e: Exception) {
                _uiMessage.value = "Error: ${e.localizedMessage}"
            }
        }
    }
    //Inicio como invitado, no tienes cuenta pero puedes jugar
    fun signInAnonymously(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.signInAnonymously()
                _authState.value = false // Invitado no está full logged in
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Error al entrar como invitado: ${e.localizedMessage}"
            }
        }
    }
    //Añado funcion para recuperar la contraseña, firestore lo hace el solo
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiMessage.value = "Por favor, introduce tu email primero."
            return
        }
        viewModelScope.launch {
            try {
                repository.sendPasswordResetEmail(email)
                _uiMessage.value = "Se ha enviado un correo de recuperación a $email"
            } catch (e: Exception) {
                _uiMessage.value = "Error: ${e.localizedMessage}"
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
                _allUsers.value = emptyList()
                _socialList.value = emptyList()
                _followers.value = emptyList()
                _following.value = emptyList()
                _profileImageUrl.value = ""
                _uiMessage.value = "Sesión cerrada correctamente"
                _userRole.value = "user"
                _isBlocked.value = false
                _selectedUser.value = null
                _compatibilityPercent.value = null
            } catch (e: Exception) {
                _uiMessage.value = "Error al cerrar sesión"
            }
        }
    }
    
    fun addFriend(friend: FriendModel) {
        viewModelScope.launch {
            try {
                repository.followUser(friend)
                _uiMessage.value = "¡Ahora sigues a ${friend.name}!"
                loadUserStats()
            } catch (e: Exception) {
                _uiMessage.value = "Error al seguir al usuario"
            }
        }
    }

    fun loadUserProfileImage() {
        viewModelScope.launch {
            _profileImageUrl.value = repository.getUserProfileImageUrl()
        }
    }

    fun updateProfilePicture(imageUri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val newUrl = repository.updateProfilePicture(imageUri)
                _profileImageUrl.value = newUrl
                _uiMessage.value = "Foto de perfil actualizada"
                loadAllUsers()
                loadUserStats()
            } catch (e: Exception) {
                android.util.Log.e("PFP_DEBUG", "Error al subir imagen", e)
                _uiMessage.value = "Error al subir la imagen: ${e.message ?: "sin detalles"}"
            }
        }
    }

    fun loadModerationState() {
        viewModelScope.launch {
            val (role, blocked) = repository.getCurrentUserModerationData()
            _userRole.value = role
            _isBlocked.value = blocked
        }
    }

    fun getCurrentUserId(): String {
        return repository.getCurrentUser()?.uid ?: ""
    }

    fun loadSelectedUser(userId: String) {
        viewModelScope.launch {
            _selectedUser.value = repository.getUserById(userId)
        }
    }

    fun clearCompatibility() {
        _compatibilityPercent.value = null
    }

    fun calculateCompatibility(otherUserId: String) {
        viewModelScope.launch {
            try {
                val myId = getCurrentUserId()

                if (myId.isBlank() || otherUserId.isBlank() || myId == otherUserId) {
                    _compatibilityPercent.value = null
                    return@launch
                }

                val myFavs = firebaseRepository.getFavorites(myId).first()
                val otherFavs = firebaseRepository.getFavorites(otherUserId).first()

                val myIds = myFavs.map { it.id }.toSet()
                val otherIds = otherFavs.map { it.id }.toSet()

                val intersection = myIds.intersect(otherIds).size
                val union = myIds.union(otherIds).size

                val percent = if (union == 0) {
                    0
                } else {
                    (intersection * 100) / union
                }

                _compatibilityPercent.value = percent

            } catch (e: Exception) {
                _compatibilityPercent.value = 0
            }
        }
    }

    fun removeProfilePicture() {
        viewModelScope.launch {
            try {
                repository.removeMyProfilePicture()
                _profileImageUrl.value = ""
                _uiMessage.value = "Foto de perfil eliminada"
                loadAllUsers()
                loadUserStats()
            } catch (e: Exception) {
                _uiMessage.value = "Error al eliminar la foto: ${e.message}"
            }
        }
    }
}