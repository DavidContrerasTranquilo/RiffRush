package com.example.riffrush.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(private val firebaseAuth: FirebaseAuth) {
    suspend fun signUp(email: String, pass: String) = 
        firebaseAuth.createUserWithEmailAndPassword(email, pass).await()

    suspend fun signIn(email: String, pass: String) = 
        firebaseAuth.signInWithEmailAndPassword(email, pass).await()

    fun signOut() = firebaseAuth.signOut()
    
    fun getCurrentUser() = firebaseAuth.currentUser
}
