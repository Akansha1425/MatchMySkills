package com.example.matchmyskills.repository

import android.util.Log
import com.example.matchmyskills.data.local.JobDao
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.util.toJob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import com.example.matchmyskills.util.getDateSafe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val jobDao: JobDao
) {
    fun getJobsByRecruiter(recruiterId: String): Flow<UiState<List<Job>>> = callbackFlow {
        trySend(UiState.Loading)

        val listener = firestore.collection("jobs")
            .whereEqualTo("recruiterId", recruiterId)
            .addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error fetching jobs: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch jobs"))
                    return@addSnapshotListener
                }

                val remoteJobs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toJob()
                } ?: emptyList()

                // Sync with local Room if needed
                launch {
                    jobDao.deleteJobsByRecruiter(recruiterId)
                    jobDao.insertJobs(remoteJobs)
                }

                if (remoteJobs.isEmpty()) {
                    trySend(UiState.Empty)
                } else {
                    trySend(UiState.Success(remoteJobs))
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun createJob(job: Job): UiState<Unit> {
        return try {
            firestore.collection("jobs").document(job.id).set(job).await()
            // Add server timestamp explicitly after setting the object to ensure it's picked up
            firestore.collection("jobs").document(job.id).update("createdAt", FieldValue.serverTimestamp()).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to create job")
        }
    }
}
