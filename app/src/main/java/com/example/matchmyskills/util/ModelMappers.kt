package com.example.matchmyskills.util

import android.util.Log
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.model.User
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Manual mappers for Firestore documents to avoid reflection-based deserialization crashes.
 * Handles both legacy (Long) and new (Timestamp) date formats safely.
 */

fun DocumentSnapshot.toUser(): User? {
    return try {
        User(
            id = id,
            name = getString("name") ?: "",
            email = getString("email") ?: "",
            role = getString("role") ?: "student",
            companyName = getString("companyName"),
            industry = getString("industry"),
            website = getString("website"),
            profileImageUrl = getString("profileImageUrl"),
            location = getString("location"),
            companySize = getString("companySize"),
            memberSince = getDateSafe("memberSince"),
            jobTitle = getString("jobTitle"),
            bio = getString("bio"),
            linkedin = getString("linkedin"),
            phone = getString("phone"),
            hiringTags = get("hiringTags") as? List<String> ?: emptyList(),
            isVerified = getBoolean("isVerified") ?: false
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping User: ${e.message}")
        null
    }
}

fun DocumentSnapshot.toJob(): Job? {
    return try {
        Job(
            id = id,
            recruiterId = getString("recruiterId") ?: "",
            title = getString("title") ?: "",
            companyName = getString("companyName") ?: "",
            description = getString("description") ?: "",
            coreSkills = get("coreSkills") as? List<String> ?: emptyList(),
            optionalSkills = get("optionalSkills") as? List<String> ?: emptyList(),
            location = getString("location") ?: "Remote",
            city = getString("city"),
            duration = getString("duration") ?: "",
            stipend = getString("stipend") ?: "",
            isPaid = getBoolean("isPaid") ?: true,
            deadline = getDateSafe("deadline"),
            benefits = get("benefits") as? List<String> ?: emptyList(),
            status = getString("status") ?: "Active",
            createdAt = getDateSafe("createdAt")
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping Job: ${e.message}")
        null
    }
}

fun DocumentSnapshot.toHackathon(): Hackathon? {
    return try {
        Hackathon(
            id = id,
            recruiterId = getString("recruiterId") ?: "",
            title = getString("title") ?: "",
            organizer = getString("organizer") ?: "",
            description = getString("description") ?: "",
            themes = get("themes") as? List<String> ?: emptyList(),
            eligibility = getString("eligibility") ?: "",
            mode = getString("mode") ?: "Online",
            platformOrLocation = getString("platformOrLocation") ?: "",
            prizePool = getString("prizePool") ?: "",
            teamSize = getString("teamSize") ?: "",
            deadline = getDateSafe("deadline"),
            status = getString("status") ?: "Active",
            createdAt = getDateSafe("createdAt")
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping Hackathon: ${e.message}")
        null
    }
}

fun DocumentSnapshot.toApplication(): Application? {
    return try {
        Application(
            id = id,
            jobId = getString("jobId") ?: "",
            recruiterId = getString("recruiterId") ?: "",
            candidateId = getString("candidateId") ?: "",
            candidateName = getString("candidateName") ?: "",
            candidateEmail = getString("candidateEmail") ?: "",
            candidateCollege = getString("candidateCollege") ?: "",
            resumeUrl = getString("resumeUrl") ?: "",
            candidateSkills = get("candidateSkills") as? List<String> ?: emptyList(),
            matchScore = getDouble("matchScore") ?: 0.0,
            coreMatchCount = getLong("coreMatchCount")?.toInt() ?: 0,
            optionalMatchCount = getLong("optionalMatchCount")?.toInt() ?: 0,
            status = getString("status") ?: "Pending",
            appliedAt = getDateSafe("appliedAt"),
            createdAt = getDateSafe("createdAt")
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping Application: ${e.message}")
        null
    }
}
