package com.example.riffrush.data.repository

import com.example.riffrush.data.model.FriendModel
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
                mapOf(
                    "uid" to uid,
                    "name" to username,
                    "bio" to defaultBio,
                    "pfpUrl" to "",
                    "status" to "¡Hola! Estoy usando RiffRush"
                )
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
        
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("name", newUsername).await()
    }

    suspend fun updatePassword(newPass: String) {
        firebaseAuth.currentUser?.updatePassword(newPass)?.await()
    }

    suspend fun updateBio(newBio: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("bio", newBio).await()
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

    suspend fun getUserSongsCount(): Int {
        val uid = firebaseAuth.currentUser?.uid ?: return 0
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("songs")
                .get()
                .await()

            val count = snapshot.size()
            android.util.Log.d("RiffRush", "Canciones encontradas en subcolección: $count")
            count
        } catch (e: Exception) {
            android.util.Log.e("RiffRush", "Error al counting subcolección songs: ${e.message}")
            0
        }
    }

    suspend fun getAllUsers(): List<FriendModel> {
        val currentUid = firebaseAuth.currentUser?.uid
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                if (uid.startsWith("guest") || uid == currentUid) {
                    null
                } else {
                    val nameFromFirestore = doc.getString("name") 
                        ?: doc.getString("username") 
                        ?: doc.getString("displayName") 
                        ?: "Usuario sin nombre"

                    FriendModel(
                        uid = uid,
                        name = nameFromFirestore,
                        pfpUrl = doc.getString("pfpUrl") ?: "",
                        status = doc.getString("status") ?: "¡Hola! Estoy usando RiffRush"
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun followUser(targetUser: FriendModel) {
        val currentUid = firebaseAuth.currentUser?.uid ?: return
        val currentUserName = firebaseAuth.currentUser?.displayName ?: "Usuario"
        
        val batch = firestore.batch()

        // 1. Añadir a mi lista de "siguiendo"
        val myFollowingRef = firestore.collection("users").document(currentUid)
            .collection("following").document(targetUser.uid)
        batch.set(myFollowingRef, targetUser)

        // 2. Añadirme a su lista de "seguidores"
        val targetFollowersRef = firestore.collection("users").document(targetUser.uid)
            .collection("followers").document(currentUid)
        batch.set(targetFollowersRef, mapOf(
            "uid" to currentUid,
            "name" to currentUserName,
            "status" to "Te ha seguido recientemente",
            "pfpUrl" to ""
        ))

        batch.commit().await()
    }

    suspend fun getSocialList(uid: String, type: String): List<FriendModel> {
        val collectionName = if (type == "Seguidores") "followers" else "following"
        return try {
            val snapshot = firestore.collection("users").document(uid)
                .collection(collectionName).get().await()
            snapshot.documents.mapNotNull { doc ->
                FriendModel(
                    uid = doc.getString("uid") ?: doc.id,
                    name = doc.getString("name") ?: "Usuario",
                    pfpUrl = doc.getString("pfpUrl") ?: "",
                    status = doc.getString("status") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}