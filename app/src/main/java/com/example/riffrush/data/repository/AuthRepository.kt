package com.example.riffrush.data.repository

import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.riffrush.data.model.FriendModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import android.util.Log
import com.example.riffrush.R
import com.google.firebase.auth.AuthResult

/*
 Repositorio para la gestión de Autenticación, Gestión de Perfil de Usuario y Relaciones Sociales.
 Encapsula las operaciones con Firebase Auth, Cloud Firestore (para datos de usuario, amigos y seguidores)
 y el servicio externo de Cloudinary (para subida de imágenes de perfil).
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: android.content.Context,
    private val okHttpClient: okhttp3.OkHttpClient
) {
    // Registra un nuevo usuario con email/contraseña, asigna su nombre de usuario e inicializa su perfil en Firestore
    suspend fun signUp(email: String, pass: String, username: String) {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val defaultBio = context.getString(R.string.default_bio_template, year)

        // Actualiza el perfil interno de Firebase Auth con el nombre visible del usuario
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        result.user?.updateProfile(profileUpdates)?.await()

        // Registra el documento del nuevo usuario en la colección "users" de Firestore
        result.user?.uid?.let { uid ->
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "email" to email,
                    "name" to username,
                    "bio" to defaultBio,
                    "pfpUrl" to "",
                    "status" to context.getString(R.string.default_status),
                    "role" to "user",
                    "isBlocked" to false
                )
            ).await()
        }
    }

    // Inicia sesión de un usuario registrado mediante email y contraseña
    suspend fun signIn(email: String, pass: String): AuthResult {
        return firebaseAuth.signInWithEmailAndPassword(email, pass).await()
    }

    // Inicia sesión anónima heredada (no utilizada por defecto tras la pantalla Welcome)
    suspend fun signInAnonymously(): AuthResult {
        return firebaseAuth.signInAnonymously().await()
    }

    // Envía un correo electrónico automático mediante Firebase para restablecer la contraseña
    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    // Cierra la sesión activa del usuario actual en Firebase Auth
    fun signOut() = firebaseAuth.signOut()

    // Retorna el objeto actual de Firebase User si hay sesión activa
    fun getCurrentUser() = firebaseAuth.currentUser

    // Retorna el nombre del usuario activo, o un fallback de Invitado en su defecto
    fun getCurrentUserName(): String = firebaseAuth.currentUser?.displayName ?: context.getString(R.string.default_guest_name)

    // Actualiza el nombre visible del usuario tanto en Firebase Auth como en su documento de Firestore
    suspend fun updateUsername(newUsername: String) {
        val profileUpdates = userProfileChangeRequest {
            displayName = newUsername
        }
        firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()

        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("name", newUsername).await()
    }

    // Actualiza la biografía (bio) del usuario en Firestore
    suspend fun updateBio(newBio: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("bio", newBio).await()
    }

    // Obtiene la biografía actual del usuario registrado en Firestore
    suspend fun getUserBio(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return context.getString(R.string.msg_login_to_see_bio)
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("bio") ?: context.getString(R.string.default_bio)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo bio del usuario", e)
            context.getString(R.string.default_bio)
        }
    }

    // Cuenta cuántas canciones tiene guardadas el usuario registrado en su subcolección local de favoritos
    suspend fun getUserSongsCount(): Int {
        val uid = firebaseAuth.currentUser?.uid ?: return 0
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("songs")
                .get()
                .await()

            val count = snapshot.size()
            Log.d("RiffRush", "Canciones encontradas en subcolección: $count")
            count
        } catch (e: Exception) {
            Log.e("RiffRush", "Error al counting subcolección songs: ${e.message}")
            0
        }
    }

    // Obtiene todos los usuarios registrados activos (excluyendo invitados, sí mismo o usuarios bloqueados)
    suspend fun getAllUsers(): List<FriendModel> {
        val currentUid = firebaseAuth.currentUser?.uid
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                val uid = doc.id
                val isBlocked = doc.getBoolean("isBlocked") ?: false
                if (uid.startsWith("guest") || uid == currentUid || isBlocked) {
                    null
                } else {
                    val nameFromFirestore = doc.getString("name")
                        ?: doc.getString("username")
                        ?: doc.getString("displayName")
                        ?: context.getString(R.string.default_no_name)

                    FriendModel(
                        uid = uid,
                        name = nameFromFirestore,
                        pfpUrl = doc.getString("pfpUrl") ?: "",
                        status = doc.getString("status") ?: context.getString(R.string.default_status),
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "user",
                        isBlocked = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo usuarios", e)
            emptyList()
        }
    }

    // Lógica social para seguir a otro usuario: agrega al destino a mi lista "following" y me agrega a su "followers" usando un lote atómico (Batch)
    suspend fun followUser(targetUser: FriendModel) {
        val currentUid = firebaseAuth.currentUser?.uid ?: return
        val currentUserName: String = firebaseAuth.currentUser?.displayName ?: "Usuario"
        val currentUserDoc = firestore.collection("users").document(currentUid).get().await()
        val currentUserPfp: String = currentUserDoc.getString("pfpUrl") ?: ""
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
                "status" to context.getString(R.string.msg_followed_recently),
                "pfpUrl" to currentUserPfp
            )
        )

        batch.commit().await()
    }

    // Obtiene de forma estructurada la lista social ("followers" o "following") para un UID específico
    suspend fun getSocialList(uid: String, type: String): List<FriendModel> {
        val collectionName = if (type == "followers") "followers" else "following"
        return try {
            val snapshot = firestore.collection("users").document(uid)
                .collection(collectionName).get().await()
            snapshot.documents.mapNotNull { doc ->
                val blocked = doc.getBoolean("isBlocked") ?: false
                if (blocked) {
                    null
                } else {
                    FriendModel(
                        uid = doc.getString("uid") ?: doc.id,
                        name = doc.getString("name") ?: "Usuario",
                        pfpUrl = doc.getString("pfpUrl") ?: "",
                        status = doc.getString("status") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "user",
                        isBlocked = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo lista social", e)
            emptyList()
        }
    }

    // Crea un archivo temporal local en caché a partir de una Uri para poder enviarla mediante POST Multipart a Cloudinary
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

    // Sube una imagen de perfil a Cloudinary mediante un post MultipartBody y guarda la URL segura retornada en Firestore
    suspend fun updateProfilePicture(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No hay usuario autenticado")

        val file = createTempFileFromUri(imageUri)

        try {
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

            // Cuerpo del POST multipart con los presets obligatorios de Cloudinary
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, requestFile)
                .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                .addFormDataPart("folder", "profile_pictures")
                .addFormDataPart("public_id", uid) // Usamos el uid para sobrescribir fotos anteriores en el mismo slot
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload")
                .post(requestBody)
                .build()

            Log.d("CLOUDINARY_DEBUG", "Subiendo imagen...")
            Log.d("CLOUDINARY_DEBUG", "Cloud name: ${CloudinaryConfig.CLOUD_NAME}")
            Log.d(
                "CLOUDINARY_DEBUG",
                "Upload preset: ${CloudinaryConfig.UPLOAD_PRESET}"
            )

            val response: okhttp3.Response = okHttpClient.newCall(request).execute()
            val responseBody: String = response.body?.string().orEmpty()

            Log.d("CLOUDINARY_DEBUG", "Response code: ${response.code}")
            Log.d("CLOUDINARY_DEBUG", "Response body: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Error Cloudinary ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            val imageUrl: String = json.getString("secure_url")

            // Guardamos la URL de la imagen en Firestore
            firestore.collection("users")
                .document(uid)
                .update("pfpUrl", imageUrl)
                .await()

            imageUrl
        } finally {
            file.delete() // Limpia el almacenamiento de caché temporal obligatoriamente
        }
    }

    // Obtiene la URL de la foto de perfil del usuario logueado directamente desde Firestore
    suspend fun getUserProfileImageUrl(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return ""
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("pfpUrl") ?: ""
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo foto de perfil", e)
            ""
        }
    }

    // Retorna una dupla del Rol y Estado de bloqueo del usuario activo para flujos de moderación y acceso
    suspend fun getCurrentUserModerationData(): Pair<String, Boolean> {
        val uid = firebaseAuth.currentUser?.uid ?: return "user" to false
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val role = doc.getString("role") ?: "user"
            val isBlocked = doc.getBoolean("isBlocked") ?: false
            role to isBlocked
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo datos de moderación", e)
            "user" to false
        }
    }

    // Obtiene la totalidad de los usuarios de Firestore sin omitir baneados o invitados (exclusivo para la consola de Administración)
    suspend fun getAllUsersForAdmin(): List<FriendModel> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.map { doc ->
                FriendModel(
                    uid = doc.getString("uid") ?: doc.id,
                    name = doc.getString("name") ?: "Usuario",
                    pfpUrl = doc.getString("pfpUrl") ?: "",
                    status = doc.getString("status") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "user",
                    isBlocked = doc.getBoolean("isBlocked") ?: false
                )
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo usuarios para admin", e)
            emptyList()
        }
    }

    // Actualiza el estado de baneo de un usuario (administración)
    suspend fun setUserBlocked(targetUid: String, blocked: Boolean) {
        firestore.collection("users")
            .document(targetUid)
            .update("isBlocked", blocked)
            .await()
    }

    // Resetea a vacío la URL de imagen de perfil de un usuario conflictivo (administración)
    suspend fun clearUserProfilePicture(targetUid: String) {
        firestore.collection("users")
            .document(targetUid)
            .update("pfpUrl", "")
            .await()
    }

    // Obtiene los detalles de estadísticas e información global de un usuario por su ID
    suspend fun getUserById(uid: String): FriendModel? {
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) return null

            FriendModel(
                uid = doc.getString("uid") ?: doc.id,
                name = doc.getString("name") ?: "Usuario",
                pfpUrl = doc.getString("pfpUrl") ?: "",
                status = doc.getString("status") ?: "",
                email = doc.getString("email") ?: "",
                role = doc.getString("role") ?: "user",
                isBlocked = doc.getBoolean("isBlocked") ?: false,
                gamesPlayed = (doc.getLong("gamesPlayed") ?: 0L).toInt(),
                songsPlayed = (doc.getLong("songsPlayed") ?: 0L).toInt(),
                maxDailyStreak = (doc.getLong("maxDailyStreak") ?: 0L).toInt()
            )
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo usuario por id: $uid", e)
            null
        }
    }

    // Elimina la foto de perfil del usuario registrado actualizando su valor a vacío
    suspend fun removeMyProfilePicture() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .update("pfpUrl", "")
            .await()
    }
}
