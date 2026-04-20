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

        // Total required skills
        val allRequiredSkills = (job.coreSkills + job.optionalSkills).map { it.lowercase().trim() }.toSet()
        val totalRequired = allRequiredSkills.size

        // Calculate matched skills
        val matchedSkillsList = (job.coreSkills + job.optionalSkills).filter { 
            it.lowercase().trim() in normalizedCandidateSkills 
        }.distinct()
        
        val missingSkillsList = (job.coreSkills + job.optionalSkills).filter { 
            it.lowercase().trim() !in normalizedCandidateSkills 
        }.distinct()

        val matchedCount = matchedSkillsList.size
        
        // Strictly calculate percentage
        val finalScore = if (totalRequired > 0) {
            (matchedCount.toDouble() / totalRequired) * 100.0
        } else {
            0.0
        }

        return Application(
            jobId = job.id,
            candidateSkills = candidateSkills,
            matchScore = finalScore,
            coreMatchCount = matchedCount, // Using this as 'Total Matched' now
            optionalMatchCount = totalRequired, // Using this as 'Total Required' now
            matchedSkills = matchedSkillsList,
            missingSkills = missingSkillsList
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
