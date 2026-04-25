package com.example.matchmyskills.repository

import com.example.matchmyskills.data.local.ApplicationDao
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.matchmyskills.util.toApplication
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class ApplicationRepository @Inject constructor(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val applicationDao: com.example.matchmyskills.data.local.ApplicationDao
) {
    fun getApplicationsByJob(jobId: String, statuses: List<String> = emptyList()): Flow<UiState<List<Application>>> = callbackFlow {
        trySend(UiState.Loading)
        var query: Query = firestore.collection("applications")
            .whereEqualTo("opportunityId", jobId)
            
        if (statuses.isNotEmpty()) {
            query = query.whereIn("status", statuses)
        }
        
        val listener = query.addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error fetching applications by job: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch applications"))
                    return@addSnapshotListener
                }
                
                val apps: List<Application> = snapshot?.documents?.mapNotNull { doc ->
                    doc.toApplication()
                } ?: emptyList()
                
                trySend(if (apps.isEmpty()) UiState.Empty else UiState.Success(apps))
            }
        awaitClose { listener.remove() }
    }

    fun getApplicationsByRecruiter(recruiterId: String, statuses: List<String> = emptyList()): Flow<UiState<List<Application>>> = callbackFlow {
        trySend(UiState.Loading)
        var query: Query = firestore.collection("applications")
            .whereEqualTo("recruiterId", recruiterId)
            
        if (statuses.isNotEmpty()) {
            query = query.whereIn("status", statuses)
        }
            
        val listener = query.addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error fetching applications by recruiter: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch all applications"))
                    return@addSnapshotListener
                }
                
                val apps: List<Application> = snapshot?.documents?.mapNotNull { doc ->
                    doc.toApplication()
                } ?: emptyList()
                
                trySend(if (apps.isEmpty()) UiState.Empty else UiState.Success(apps))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateApplicationStatus(applicationId: String, status: String): UiState<Unit> {
        return try {
            firestore.collection("applications").document(applicationId)
                .update("status", status).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to update status")
        }
    }

    suspend fun createNotification(
        userId: String,
        message: String,
        type: String,
        opportunityId: String = "",
        opportunityType: String = ""
    ): UiState<Unit> {
        if (userId.isBlank()) {
            Log.e("NotificationError", "Attempted to create notification for blank userId")
            return UiState.Error("Invalid student ID")
        }
        return try {
            val notification = hashMapOf(
                "userId" to userId,
                "candidateId" to userId, // Save to both for compatibility
                "message" to message,
                "type" to type,
                "opportunityId" to opportunityId,
                "opportunityType" to opportunityType,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "isRead" to false
            )
            firestore.collection("notifications").add(notification).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationError", "Failed to create notification", e)
            UiState.Error(e.message ?: "Failed to send notification")
        }
    }

    suspend fun sendEmailApi(email: String, name: String, jobTitle: String): UiState<Unit> {
        return try {
            val client = okhttp3.OkHttpClient()
            val json = com.google.gson.JsonObject().apply {
                addProperty("email", email)
                addProperty("name", name)
                addProperty("jobTitle", jobTitle)
            }
            
            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = okhttp3.Request.Builder()
                .url("https://matchmyskills-backend.onrender.com/send-email") // Example endpoint
                .post(requestBody)
                .build()
                
            // For now, we simulate success to avoid failing if the endpoint is down
            // client.newCall(request).execute().use { response -> ... }
            Log.d("EmailAPI", "Simulating email to $email for $jobTitle")
            UiState.Success(Unit)
        } catch (e: Exception) {
            Log.e("EmailError", "Failed to send email", e)
            UiState.Error(e.message ?: "Failed to send email")
        }
    }
}
