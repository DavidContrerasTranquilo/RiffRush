package com.example.riffrush.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun signUp(email: String, pass: String, username: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val defaultBio = "Explorando nuevos ritmos desde $year"

        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        result.user?.updateProfile(profileUpdates)?.await()

        result.user?.uid?.let { uid ->
            firestore.collection("users").document(uid).set(
                mapOf("bio" to defaultBio)
            ).await()
        }
    }

    suspend fun signIn(email: String, pass: String) =
        firebaseAuth.signInWithEmailAndPassword(email, pass).await()

    fun signOut() = firebaseAuth.signOut()

    fun getCurrentUser() = firebaseAuth.currentUser

    fun getCurrentUserName(): String = firebaseAuth.currentUser?.displayName ?: "Invitado"

    suspend fun updateUsername(newUsername: String) {
        val profileUpdates = userProfileChangeRequest {
            displayName = newUsername
        }
        firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
    }

    suspend fun updatePassword(newPass: String) {
        firebaseAuth.currentUser?.updatePassword(newPass)?.await()
    }

    suspend fun updateBio(newBio: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("bio", newBio).await()
    }

    suspend fun getUserBio(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return "Inicia sesión para ver tu bio"
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("bio") ?: "Explorando nuevos ritmos"
        } catch (e: Exception) {
            "Explorando nuevos ritmos"
        }
    }
}