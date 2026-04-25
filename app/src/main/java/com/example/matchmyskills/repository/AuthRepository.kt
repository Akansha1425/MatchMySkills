package com.example.matchmyskills.repository

import com.example.matchmyskills.model.User
import com.example.matchmyskills.util.UiState
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.matchmyskills.BuildConfig
import com.example.matchmyskills.util.toUser
import com.example.matchmyskills.util.getDateSafe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: android.content.SharedPreferences
) {
    fun getCurrentUser(): User? {
        val fbUser = auth.currentUser ?: return null
        return User(id = fbUser.uid, email = fbUser.email ?: "")
    }

    fun observeUserProfile(userId: String): Flow<UiState<User>> = callbackFlow {
        trySend(UiState.Loading)
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error observing user profile: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch profile"))
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toUser()
                if (user != null) {
                    trySend(UiState.Success(user))
                } else {
                    trySend(UiState.Empty)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getUserRole(): String? {
        return prefs.getString("user_role", null)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
        auth.signOut()
    }

    suspend fun getUserProfile(userId: String): UiState<User> {
        return try {
            // Using a flow to get the first value from the real-time listener
            // to fulfill the 'Replace all .get()' requirement
            observeUserProfile(userId).filter { it !is UiState.Loading }.first()
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to fetch profile")
        }
    }

    suspend fun completeOnboarding(userId: String, companyName: String, industry: String, website: String): UiState<Unit> {
        return try {
            val updates = mapOf(
                "companyName" to companyName,
                "industry" to industry,
                "website" to website,
                "role" to "recruiter"
            )
            firestore.collection("users").document(userId).update(updates).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            // If document doesn't exist, update will fail. Fallback to set with merge.
            val data = mapOf(
                "id" to userId,
                "companyName" to companyName,
                "industry" to industry,
                "website" to website,
                "role" to "recruiter"
            )
            firestore.collection("users").document(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            UiState.Success(Unit)
        }
    }

    suspend fun createUserProfile(user: User): UiState<Unit> {
        return try {
            firestore.collection("users").document(user.id).set(user).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to create profile")
        }
    }

    fun updateUserProfile(userId: String, updates: Map<String, Any?>): Flow<UiState<Unit>> = callbackFlow {
        trySend(UiState.Loading)
        try {
            firestore.collection("users").document(userId).update(updates).await()
            trySend(UiState.Success(Unit))
        } catch (e: Exception) {
            trySend(UiState.Error(e.message ?: "Failed to update profile"))
        }
        awaitClose { }
    }

    fun uploadProfileImage(userId: String, uri: android.net.Uri): Flow<UiState<String>> = callbackFlow {
        trySend(UiState.Loading)
        try {
            val preset = BuildConfig.CLOUDINARY_UNSIGNED_PRESET
            if (preset.isBlank()) {
                trySend(UiState.Error("Cloudinary upload preset is not configured"))
                awaitClose { }
                return@callbackFlow
            }

            MediaManager.get().upload(uri)
                .unsigned(preset)
                .option("public_id", "profile_$userId")
                .option("folder", "matchmyskills/profiles")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String?) = Unit

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) = Unit

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val downloadUrl = resultData?.get("secure_url")?.toString().orEmpty()
                        if (downloadUrl.isBlank()) {
                            trySend(UiState.Error("Upload succeeded but image URL is missing"))
                            return
                        }

                        firestore.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "profileImage" to downloadUrl,
                                    "profileImageUrl" to downloadUrl
                                )
                            )
                            .addOnSuccessListener {
                                trySend(UiState.Success(downloadUrl))
                            }
                            .addOnFailureListener { error ->
                                trySend(UiState.Error(error.message ?: "Failed to save profile image URL"))
                            }
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        trySend(UiState.Error(error?.description ?: "Failed to upload image"))
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) = Unit
                })
                .dispatch()
        } catch (e: Exception) {
            trySend(UiState.Error(e.message ?: "Failed to upload image"))
        }
        awaitClose { }
    }

    fun signOut() {
        clearUserData()
    }
}
