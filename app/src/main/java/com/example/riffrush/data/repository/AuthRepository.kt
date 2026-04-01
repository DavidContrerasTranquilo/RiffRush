package com.example.riffrush.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(private val firebaseAuth: FirebaseAuth) {
    suspend fun signUp(email: String, pass: String, username: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        result.user?.updateProfile(profileUpdates)?.await()
    }

    suspend fun signIn(email: String, pass: String) =
        firebaseAuth.signInWithEmailAndPassword(email, pass).await()

    fun signOut() = firebaseAuth.signOut()

    fun getCurrentUser() = firebaseAuth.currentUser

    fun getCurrentUserName(): String {
        return firebaseAuth.currentUser?.displayName ?: "Invitado"
    }

    suspend fun updateUsername(newUsername: String) {
        val profileUpdates = userProfileChangeRequest {
            displayName = newUsername
        }
        firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
    }
}