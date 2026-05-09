package com.example.riffrush.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riffrush.data.model.FriendModel
import com.example.riffrush.data.repository.AuthRepository
import com.example.riffrush.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.util.Log
import com.example.riffrush.R

class AuthViewModel(
    private val context: Context,
    private val repository: AuthRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private val _authState =
        MutableStateFlow(repository.getCurrentUser()?.isAnonymous == false)
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

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun resetUpdateStatus() {
        _updateSuccess.value = false
    }

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
                val followersList = repository.getSocialList(uid, "followers")
                val followingList = repository.getSocialList(uid, "following")

                _followers.value = followersList.map { it.uid }
                _following.value = followingList.map { it.uid }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error cargando estadísticas del usuario", e)
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
                // Borrar usuario anónimo previo si existe
                val currentUser = repository.getCurrentUser()
                if (currentUser != null && currentUser.isAnonymous) {
                    try { currentUser.delete().await() } catch (_: Exception) {}
                }

                repository.signUp(email.trim(), pass, username.trim())
                _uiMessage.value = context.getString(R.string.msg_signup_success)
                _authState.value = true
                loadUserBio()
                loadUserStats()
                loadAllUsers()
                loadUserProfileImage()
                loadModerationState()
            } catch (e: Exception) {
                _uiMessage.value = context.getString(R.string.msg_signup_error, e.localizedMessage)
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            try {
                // Borrar usuario anónimo previo si existe
                val currentUser = repository.getCurrentUser()
                if (currentUser != null && currentUser.isAnonymous) {
                    try { currentUser.delete().await() } catch (_: Exception) {}
                }

                repository.signIn(email.trim(), pass)
                _uiMessage.value = context.getString(R.string.msg_login_success)
                _authState.value = true
                loadUserBio()
                loadUserStats()
                loadAllUsers()
                loadUserProfileImage()
                loadModerationState()
            } catch (e: Exception) {
                _uiMessage.value = context.getString(R.string.msg_login_error, e.localizedMessage)
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
                _uiMessage.value = context.getString(R.string.msg_guest_error, e.localizedMessage)
            }
        }
    }

    //Añado funcion para recuperar la contraseña, firestore lo hace el solo
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiMessage.value = context.getString(R.string.msg_enter_email_first)
            return
        }
        viewModelScope.launch {
            try {
                repository.sendPasswordResetEmail(email)
                _uiMessage.value = context.getString(R.string.msg_reset_email_sent, email)
            } catch (e: Exception) {
                _uiMessage.value = context.getString(R.string.msg_login_error, e.localizedMessage)
            }
        }
    }

    //Funcion par resetear la contraseña a mano
    fun sendPasswordResetToCurrentUser() {
        val email = repository.getCurrentUser()?.email
        if (email.isNullOrBlank()) {
            _uiMessage.value = context.getString(R.string.msg_no_email_found)
            return
        }
        viewModelScope.launch {
            try {
                repository.sendPasswordResetEmail(email)
                _uiMessage.value = context.getString(R.string.msg_reset_email_sent, email)
            } catch (e: Exception) {
                _uiMessage.value = context.getString(R.string.msg_login_error, e.localizedMessage)
            }
        }
    }

    fun updateFullProfile(newName: String, newBio: String) {
        viewModelScope.launch {
            try {
                repository.updateUsername(newName)
                repository.updateBio(newBio)
                _uiMessage.value = context.getString(R.string.msg_profile_updated)
                loadUserBio()
                _updateSuccess.value = true
            } catch (e: Exception) {
                _uiMessage.value = context.getString(R.string.msg_profile_update_error, e.message)
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
                _uiMessage.value = context.getString(R.string.msg_logout_success)
                _userRole.value = "user"
                _isBlocked.value = false
                _selectedUser.value = null
                _compatibilityPercent.value = null
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error cerrando sesión", e)
                _uiMessage.value = context.getString(R.string.msg_logout_error)
            }
        }
    }

    fun addFriend(friend: FriendModel) {
        viewModelScope.launch {
            try {
                repository.followUser(friend)
                _uiMessage.value = context.getString(R.string.msg_now_following, friend.name)
                loadUserStats()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error siguiendo al usuario: ${friend.uid}", e)
                _uiMessage.value = context.getString(R.string.msg_follow_error)
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
                _uiMessage.value = context.getString(R.string.msg_pfp_updated)
                loadAllUsers()
                loadUserStats()
            } catch (e: Exception) {
                Log.e("PFP_DEBUG", "Error al subir imagen", e)
                _uiMessage.value = context.getString(R.string.msg_pfp_upload_error, e.message ?: "")
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
                Log.e("AuthViewModel", "Error calculando compatibilidad con usuario: $otherUserId", e)
                _compatibilityPercent.value = 0
            }
        }
    }

    fun removeProfilePicture() {
        viewModelScope.launch {
            try {
                repository.removeMyProfilePicture()
                _profileImageUrl.value = ""
                _uiMessage.value = context.getString(R.string.msg_pfp_removed)
                loadAllUsers()
                loadUserStats()
            } catch (e: Exception) {
                _uiMessage.value = context.getString(R.string.msg_pfp_remove_error, e.message ?: "")
            }
        }
    }
}
