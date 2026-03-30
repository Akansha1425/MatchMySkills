package com.example.matchmyskills

import com.example.matchmyskills.model.Job
import com.example.matchmyskills.util.MatchingEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchingEngineTest {

    @Test
    fun testPerfectMatch() {
        val job = Job(
            id = "1",
            coreSkills = listOf("Kotlin", "Android"),
            optionalSkills = listOf("Firebase")
        )
        val candidateSkills = listOf("Kotlin", "Android", "Firebase")
        
        val result = MatchingEngine.calculateMatchScore(candidateSkills, job)
        assertEquals(100.0, result.matchScore, 0.01)
    }

    @Test
    fun testCoreMatchOnly() {
        val job = Job(
            id = "1",
            coreSkills = listOf("Kotlin", "Android"),
            optionalSkills = listOf("Firebase")
        )
        val candidateSkills = listOf("Kotlin", "Android")
        
        val result = MatchingEngine.calculateMatchScore(candidateSkills, job)
        assertEquals(70.0, result.matchScore, 0.01)
    }

    @Test
    fun testOptionalMatchOnly() {
        val job = Job(
            id = "1",
            coreSkills = listOf("Kotlin", "Android"),
            optionalSkills = listOf("Firebase")
        )
        val candidateSkills = listOf("Firebase")
        
        val result = MatchingEngine.calculateMatchScore(candidateSkills, job)
        assertEquals(30.0, result.matchScore, 0.01)
    }

    @Test
    fun testCaseInsensitiveAndTrim() {
        val job = Job(
            id = "1",
            coreSkills = listOf("Kotlin", "Android"),
            optionalSkills = listOf("Firebase")
        )
        val candidateSkills = listOf("  KOTLIN  ", "android", "fIrEbAsE")
        
        val result = MatchingEngine.calculateMatchScore(candidateSkills, job)
        assertEquals(100.0, result.matchScore, 0.01)
    }

    @Test
    fun testNoSkillsInJob() {
        val job = Job(id = "1", coreSkills = emptyList(), optionalSkills = emptyList())
        val candidateSkills = listOf("Any")
        
        val result = MatchingEngine.calculateMatchScore(candidateSkills, job)
        assertEquals(100.0, result.matchScore, 0.01)
    }
}
