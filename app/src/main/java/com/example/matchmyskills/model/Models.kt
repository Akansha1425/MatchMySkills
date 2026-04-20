package com.example.matchmyskills.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.matchmyskills.util.Converters
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "users")
@TypeConverters(Converters::class)
data class User(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "recruiter", // student, recruiter, admin
    val companyName: String? = null,
    val industry: String? = null,
    val website: String? = null,
    val profileImageUrl: String? = null,
    val skills: List<String> = emptyList(),
    // New Professional Recruiter Fields
    val location: String? = null,
    val companySize: String? = null,
    val memberSince: Date? = null,
    val jobTitle: String? = null,
    val bio: String? = null,
    val linkedin: String? = null,
    val phone: String? = null,
    val hiringTags: List<String> = emptyList(),
    val isVerified: Boolean = false
) : Parcelable

@Parcelize
@Entity(tableName = "jobs")
@TypeConverters(Converters::class)
data class Job(
    @PrimaryKey val id: String = "",
    val recruiterId: String = "",
    val title: String = "",
    val companyName: String = "",
    val description: String = "",
    val coreSkills: List<String> = emptyList(),
    val optionalSkills: List<String> = emptyList(),
    val location: String = "", // Remote, Onsite, Hybrid
    val city: String? = null,
    val duration: String = "", // e.g., 6 months
    val stipend: String = "", // e.g., 20000
    val isPaid: Boolean = true,
    val deadline: Date? = null, // Changed from Long
    val benefits: List<String> = emptyList(),
    val status: String = "Active", // Active, Closed
    val opportunityType: String = "JOB", // JOB, INTERNSHIP
    val source: String = "FIREBASE", // FIREBASE, EXTERNAL
    val applyUrl: String? = null,
    @ServerTimestamp val createdAt: Date? = null
) : Parcelable

@Parcelize
@TypeConverters(Converters::class)
data class JobOpportunity(
    val jobId: String = "",
    val recruiterId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val description: String = "",
    val workMode: String = "",
    val location: String = "",
    val experience: String = "",
    val skills: List<String> = emptyList(),
    val jobFunction: String = "",
    val employmentType: String = "",
    val salary: String = "",
    val deadline: Date? = null,
    @ServerTimestamp val createdAt: Date? = null
) : Parcelable

@Parcelize
@Entity(tableName = "hackathons")
@TypeConverters(Converters::class)
data class Hackathon(
    @PrimaryKey val id: String = "",
    val recruiterId: String = "",
    val title: String = "",
    val organizer: String = "",
    val description: String = "",
    val themes: List<String> = emptyList(),
    val eligibility: String = "",
    val mode: String = "Online", // Online, Offline
    val platformOrLocation: String = "",
    val prizePool: String = "",
    val teamSize: String = "",
    val deadline: Date? = null, // Changed from Long
    val status: String = "Active",
    val opportunityType: String = "HACKATHON",
    val source: String = "FIREBASE", // FIREBASE, EXTERNAL
    val applyUrl: String? = null,
    @ServerTimestamp val createdAt: Date? = null
) : Parcelable

@Parcelize
@Entity(tableName = "applications")
@TypeConverters(Converters::class)
data class Application(
    @PrimaryKey val id: String = "",
    val jobId: String = "",
    val opportunityId: String = "",
    val opportunityType: String = "JOB",
    val source: String = "FIREBASE",
    val recruiterId: String = "",
    val candidateId: String = "",
    val candidateName: String = "",
    val candidateEmail: String = "",
    val candidateCollege: String = "",
    val resumeUrl: String = "",
    val candidateSkills: List<String> = emptyList(),
    val matchScore: Double = 0.0,
    val coreMatchCount: Int = 0,
    val optionalMatchCount: Int = 0,
    val candidateMarks: String = "",
    val candidateReason: String = "",
    val matchedSkills: List<String> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val status: String = "Pending", // Pending, Shortlisted, Rejected, Hired
    val appliedAt: Date? = null, // Changed from Long
    @ServerTimestamp val createdAt: Date? = null
) : Parcelable
