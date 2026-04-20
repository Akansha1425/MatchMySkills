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
import com.example.matchmyskills.util.getDateSafe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val applicationDao: ApplicationDao
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
}
