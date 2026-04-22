package com.example.riffrush.data.repository

import com.example.riffrush.data.model.FriendModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: android.content.Context,
    private val okHttpClient: okhttp3.OkHttpClient
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

    //Funcion para mandar el mail de reseteo de contraseña, firebase se encarga!
    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

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
        val currentUserDoc = firestore.collection("users").document(currentUid).get().await()
        val currentUserPfp = currentUserDoc.getString("pfpUrl") ?: ""
        val batch = firestore.batch()

        // 1. Añadir a mi lista de "siguiendo"
        val myFollowingRef = firestore.collection("users").document(currentUid)
            .collection("following").document(targetUser.uid)
        batch.set(myFollowingRef, targetUser)

        // 2. Añadirme a su lista de "seguidores"
        val targetFollowersRef = firestore.collection("users").document(targetUser.uid)
            .collection("followers").document(currentUid)
        batch.set(
            targetFollowersRef, mapOf(
                "uid" to currentUid,
                "name" to currentUserName,
                "status" to "Te ha seguido recientemente",
                "pfpUrl" to currentUserPfp
            )
        )

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

    private fun createTempFileFromUri(uri: Uri): File {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType) ?: "jpg"

        val tempFile = File.createTempFile("profile_", ".$extension", context.cacheDir)

        val inputStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir la imagen seleccionada")

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    suspend fun updateProfilePicture(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No hay usuario autenticado")

        val file = createTempFileFromUri(imageUri)

        try {
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, requestFile)
                .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                .addFormDataPart("folder", "profile_pictures")
                .addFormDataPart("public_id", uid)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload")
                .post(requestBody)
                .build()

            android.util.Log.d("CLOUDINARY_DEBUG", "Subiendo imagen...")
            android.util.Log.d("CLOUDINARY_DEBUG", "Cloud name: ${CloudinaryConfig.CLOUD_NAME}")
            android.util.Log.d("CLOUDINARY_DEBUG", "Upload preset: ${CloudinaryConfig.UPLOAD_PRESET}")

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            android.util.Log.d("CLOUDINARY_DEBUG", "Response code: ${response.code}")
            android.util.Log.d("CLOUDINARY_DEBUG", "Response body: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Error Cloudinary ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            val imageUrl = json.getString("secure_url")

            firestore.collection("users")
                .document(uid)
                .update("pfpUrl", imageUrl)
                .await()

            imageUrl
        } finally {
            file.delete()
        }
    }

    suspend fun getUserProfileImageUrl(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return ""
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("pfpUrl") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
