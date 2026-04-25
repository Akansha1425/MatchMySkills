package com.example.matchmyskills.repository

import com.example.matchmyskills.data.remote.ExternalOpportunityDataSource
import com.example.matchmyskills.model.DashboardStats
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class StudentDashboardProfile(
    val name: String,
    val location: String?,
    val profileImageUrl: String?
)

interface StudentDashboardDataSource {
    suspend fun fetchStats(userId: String): DashboardStats
    suspend fun fetchProfile(userId: String): StudentDashboardProfile
    suspend fun updateLocation(userId: String, location: String)
}

@Singleton
class StudentDashboardRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : StudentDashboardDataSource {

    override suspend fun fetchStats(userId: String): DashboardStats = coroutineScope {
        val firebaseCountsDeferred = async {
            val jobsSnapshot = firestore.collection("jobs")
                .whereEqualTo("status", "Active")
                .get()
                .await()

            var jobs = 0
            var internships = 0
            jobsSnapshot.documents.forEach { doc ->
                if (isInternship(doc.getString("opportunityType"), doc.getString("title"), doc.getString("description"))) {
                    internships += 1
                } else {
                    jobs += 1
                }
            }
            Pair(jobs, internships)
        }

        val hackathonsDeferred = async {
            firestore.collection("hackathons")
                .whereEqualTo("status", "Active")
                .get()
                .await()
                .size()
        }

        val applicationsDeferred = async {
            firestore.collection("applications")
                .whereEqualTo("candidateId", userId)
                .get()
                .await()
                .documents
        }

        val externalJobsDeferred = async {
            runCatching { ExternalOpportunityDataSource.fetchJobs("software", "JOB").size }
                .getOrDefault(0)
        }

        val externalInternshipsDeferred = async {
            runCatching { ExternalOpportunityDataSource.fetchJobs("internship", "INTERNSHIP").size }
                .getOrDefault(0)
        }

        val externalHackathonsDeferred = async {
            runCatching { ExternalOpportunityDataSource.fetchHackathons().size }
                .getOrDefault(0)
        }

        val (firebaseJobs, firebaseInternships) = firebaseCountsDeferred.await()
        val firebaseHackathons = hackathonsDeferred.await()
        val applicationDocs = applicationsDeferred.await()

        val appliedJobs = applicationDocs.count {
            val type = it.getString("type")?.lowercase()
            val legacyType = it.getString("opportunityType").orEmpty()
            type == "job" || legacyType.equals("JOB", ignoreCase = true)
        }

        val appliedInternships = applicationDocs.count {
            val type = it.getString("type")?.lowercase()
            val legacyType = it.getString("opportunityType").orEmpty()
            type == "internship" || legacyType.equals("INTERNSHIP", ignoreCase = true)
        }

        val appliedHackathons = applicationDocs.count {
            val type = it.getString("type")?.lowercase()
            val legacyType = it.getString("opportunityType").orEmpty()
            type == "hackathon" || legacyType.equals("HACKATHON", ignoreCase = true)
        }

        DashboardStats(
            totalJobs = firebaseJobs + externalJobsDeferred.await(),
            appliedJobs = appliedJobs,
            totalInternships = firebaseInternships + externalInternshipsDeferred.await(),
            appliedInternships = appliedInternships,
            totalHackathons = firebaseHackathons + externalHackathonsDeferred.await(),
            appliedHackathons = appliedHackathons
        )
    }

    override suspend fun fetchProfile(userId: String): StudentDashboardProfile {
        val snapshot = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        return StudentDashboardProfile(
            name = snapshot.getString("name") ?: "Student",
            location = snapshot.getString("location"),
            profileImageUrl = snapshot.getString("profileImage") ?: snapshot.getString("profileImageUrl")
        )
    }

    override suspend fun updateLocation(userId: String, location: String) {
        firestore.collection("users")
            .document(userId)
            .update("location", location)
            .await()
    }

    private fun isInternship(opportunityType: String?, title: String?, description: String?): Boolean {
        if (opportunityType.equals("INTERNSHIP", ignoreCase = true)) {
            return true
        }
        val text = "${title.orEmpty()} ${description.orEmpty()}".lowercase(Locale.getDefault())
        return text.contains("intern") || text.contains("internship")
    }
}
