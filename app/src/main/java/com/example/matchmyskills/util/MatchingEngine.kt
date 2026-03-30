package com.example.matchmyskills.util

import com.example.matchmyskills.model.Application
import com.example.matchmyskills.model.Job

object MatchingEngine {

    /**
     * Logic:
     * coreScore = (matchedCore / totalCore) * 0.7
     * optionalScore = (matchedOptional / totalOptional) * 0.3
     * totalMatch = (coreScore + optionalScore) * 100
     */
    fun calculateMatchScore(candidateSkills: List<String>, job: Job): Application {
        val normalizedCandidateSkills = candidateSkills.map { it.lowercase().trim() }.toSet()
        val normalizedCoreSkills = job.coreSkills.map { it.lowercase().trim() }.toSet()
        val normalizedOptionalSkills = job.optionalSkills.map { it.lowercase().trim() }.toSet()

        val matchedCore = normalizedCoreSkills.count { it in normalizedCandidateSkills }
        val matchedOptional = normalizedOptionalSkills.count { it in normalizedCandidateSkills }

        val coreWeight = 0.7
        val optionalWeight = 0.3

        val coreScore = if (normalizedCoreSkills.isNotEmpty()) {
            (matchedCore.toDouble() / normalizedCoreSkills.size) * coreWeight
        } else {
            coreWeight // If no core skills required, give full weight
        }

        val optionalScore = if (normalizedOptionalSkills.isNotEmpty()) {
            (matchedOptional.toDouble() / normalizedOptionalSkills.size) * optionalWeight
        } else {
            optionalWeight // If no optional skills required, give full weight
        }

        val totalScore = (coreScore + optionalScore) * 100.0
        val finalScore = totalScore.coerceIn(0.0, 100.0)

        return Application(
            jobId = job.id,
            candidateSkills = candidateSkills,
            matchScore = finalScore,
            coreMatchCount = matchedCore,
            optionalMatchCount = matchedOptional
        )
    }

    fun getScoreColor(score: Double): String {
        return when {
            score >= 90.0 -> "#4CAF50" // Green (Top Match)
            score >= 70.0 -> "#FFEB3B" // Yellow/Amber (Medium)
            else -> "#F44336" // Red (Low)
        }
    }
}
