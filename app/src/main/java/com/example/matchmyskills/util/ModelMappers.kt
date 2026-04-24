package com.example.matchmyskills.util

import android.util.Log
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.model.JobOpportunity
import com.example.matchmyskills.model.RecruiterNotification
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
        val title = getString("title") ?: getString("jobTitle") ?: ""
        val skills = (get("coreSkills") as? List<String>)
            ?: (get("skills") as? List<String>)
            ?: emptyList()
        val location = getString("location") ?: getString("city") ?: "Remote"
        val stipend = getString("stipend") ?: getString("salary") ?: ""
        val duration = getString("duration") ?: getString("employmentType") ?: ""

        Job(
            id = id,
            recruiterId = getString("recruiterId") ?: "",
            title = title,
            companyName = getString("companyName") ?: "",
            description = getString("description") ?: "",
            coreSkills = skills,
            optionalSkills = (get("optionalSkills") as? List<String>) ?: emptyList(),
            location = location,
            city = getString("city") ?: location,
            duration = duration,
            stipend = stipend,
            isPaid = getBoolean("isPaid") ?: true,
            deadline = getDateSafe("deadline"),
            benefits = get("benefits") as? List<String> ?: emptyList(),
            status = getString("status") ?: "Active",
            opportunityType = getString("opportunityType") ?: "JOB",
            source = getString("source") ?: "FIREBASE",
            applyUrl = getString("applyUrl"),
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
            opportunityType = getString("opportunityType") ?: "HACKATHON",
            source = getString("source") ?: "FIREBASE",
            applyUrl = getString("applyUrl"),
            createdAt = getDateSafe("createdAt")
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping Hackathon: ${e.message}")
        null
    }
}

fun DocumentSnapshot.toJobOpportunity(): JobOpportunity? {
    return try {
        JobOpportunity(
            jobId = getString("jobId") ?: id,
            recruiterId = getString("recruiterId") ?: "",
            jobTitle = getString("jobTitle") ?: getString("title") ?: "",
            companyName = getString("companyName") ?: "",
            description = getString("description") ?: "",
            workMode = getString("workMode") ?: "",
            location = getString("city") ?: getString("location") ?: "",
            experience = getString("experience") ?: "",
            skills = (get("skills") as? List<String>) ?: (get("coreSkills") as? List<String>) ?: emptyList(),
            jobFunction = getString("jobFunction") ?: "",
            employmentType = getString("employmentType") ?: "",
            salary = getString("salary") ?: getString("stipend") ?: "",
            deadline = getDateSafe("deadline"),
            createdAt = getDateSafe("createdAt")
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping JobOpportunity: ${e.message}")
        null
    }
}

fun DocumentSnapshot.toApplication(): Application? {
    return try {
        Application(
            id = id,
            jobId = getString("jobId") ?: "",
            opportunityId = getString("opportunityId") ?: getString("jobId") ?: "",
            opportunityType = getString("opportunityType") ?: "JOB",
            source = getString("source") ?: "FIREBASE",
            recruiterId = getString("recruiterId") ?: "",
            candidateId = getString("candidateId") ?: "",
            candidateName = getString("candidateName") ?: getString("applicantName") ?: "",
            candidateEmail = getString("candidateEmail") ?: getString("applicantEmail") ?: "",
            candidateCollege = getString("candidateCollege") ?: "",
            resumeUrl = getString("resumeUrl") ?: "",
            candidateSkills = get("candidateSkills") as? List<String> ?: emptyList(),
            matchScore = getDouble("matchScore") ?: 0.0,
            coreMatchCount = getLong("coreMatchCount")?.toInt() ?: 0,
            optionalMatchCount = getLong("optionalMatchCount")?.toInt() ?: 0,
            candidateMarks = getString("candidateMarks") ?: "",
            candidateReason = getString("candidateReason") ?: "",
            matchedSkills = get("matchedSkills") as? List<String> ?: emptyList(),
            missingSkills = get("missingSkills") as? List<String> ?: emptyList(),
            status = getString("status") ?: "Pending",
            appliedAt = getDateSafe("appliedAt"),
            createdAt = getDateSafe("createdAt")
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping Application: ${e.message}")
        null
    }
}

fun DocumentSnapshot.toRecruiterNotification(): RecruiterNotification? {
    return try {
        RecruiterNotification(
            id = id,
            recruiterId = getString("recruiterId") ?: "",
            message = getString("message") ?: "",
            jobId = getString("jobId") ?: "",
            timestamp = getDateSafe("timestamp"),
            isRead = getBoolean("isRead") ?: false
        )
    } catch (e: Exception) {
        Log.e("MAPPER_ERROR", "Error mapping RecruiterNotification: ${e.message}")
        null
    }
}
