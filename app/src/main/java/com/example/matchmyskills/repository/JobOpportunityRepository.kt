package com.example.matchmyskills.repository

import android.util.Log
import com.example.matchmyskills.model.JobOpportunity
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.util.toJobOpportunity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobOpportunityRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private companion object {
        const val JOBS_COLLECTION = "jobs"
    }

    fun getJobOpportunitiesByRecruiter(recruiterId: String): Flow<UiState<List<JobOpportunity>>> = callbackFlow {
        trySend(UiState.Loading)

        val listener = firestore.collection(JOBS_COLLECTION)
            .whereEqualTo("recruiterId", recruiterId)
            .whereEqualTo("opportunityType", "JOB")
            .addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error fetching job opportunities: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch posted jobs"))
                    return@addSnapshotListener
                }

                val jobs = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toJobOpportunity() }
                    ?.sortedByDescending { it.createdAt?.time ?: 0L }
                    ?: emptyList()

                if (jobs.isEmpty()) {
                    trySend(UiState.Empty)
                } else {
                    trySend(UiState.Success(jobs))
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun createJobOpportunity(job: JobOpportunity): UiState<Unit> {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return UiState.Error("Please login to post a job")
        }

        return try {
            val payload = hashMapOf(
                "id" to job.jobId,
                "jobId" to job.jobId,
                "recruiterId" to job.recruiterId,
                "title" to job.jobTitle,
                "jobTitle" to job.jobTitle,
                "companyName" to job.companyName,
                "description" to job.description,
                "workMode" to job.workMode,
                "location" to job.workMode,
                "city" to job.location,
                "experience" to job.experience,
                "skills" to job.skills,
                "coreSkills" to job.skills,
                "jobFunction" to job.jobFunction,
                "employmentType" to job.employmentType,
                "salary" to job.salary,
                "stipend" to job.salary,
                "deadline" to job.deadline,
                "status" to "Active",
                "opportunityType" to "JOB",
                "source" to "FIREBASE"
            )

            firestore.collection(JOBS_COLLECTION).document(job.jobId).set(payload).await()
            firestore.collection(JOBS_COLLECTION).document(job.jobId)
                .update("createdAt", FieldValue.serverTimestamp())
                .await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            when (e) {
                is FirebaseFirestoreException -> {
                    Log.e(
                        "FIREBASE_ERROR",
                        "Failed to post job. code=${e.code}, message=${e.message}",
                        e
                    )
                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        UiState.Error("Permission denied. Please login again and check Firestore rules.")
                    } else {
                        UiState.Error(e.message ?: "Failed to post job")
                    }
                }
                else -> {
                    Log.e("FIREBASE_ERROR", "Failed to post job: ${e.message}", e)
                    UiState.Error(e.message ?: "Failed to post job")
                }
            }
        }
    }
}
