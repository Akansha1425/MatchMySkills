package com.example.matchmyskills.ai

import com.example.matchmyskills.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class CandidateAnalysisInput(
    val jobDescription: String,
    val requiredSkills: List<String>,
    val candidateSkills: List<String>,
    val resumeText: String
)

data class CandidateAnalysisResult(
    val matchPercentage: Int,
    val strengths: List<String>,
    val missingSkills: List<String>,
    val recommendation: String,
    val fitLabel: String
)

object CandidateAiAnalyzer {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyze(input: CandidateAnalysisInput): Result<CandidateAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = BuildConfig.AI_ANALYSIS_ENDPOINT
            require(endpoint.isNotBlank()) { "AI analysis service is not configured" }

            val payload = mapOf(
                "prompt" to "Analyze this candidate for the job.",
                "job_description" to input.jobDescription,
                "required_skills" to input.requiredSkills,
                "candidate_skills" to input.candidateSkills,
                "resume_text" to input.resumeText,
                "jobDescription" to input.jobDescription,
                "requiredSkills" to input.requiredSkills,
                "candidateSkills" to input.candidateSkills,
                "resumeText" to input.resumeText
            )

            val request = Request.Builder()
                .url(endpoint)
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("AI request failed with code ${response.code}")
                }

                val body = response.body?.string().orEmpty()
                val json = JsonParser.parseString(body).asJsonObject
                parseResult(json)
            }
        }
    }

    private fun parseResult(json: JsonObject): CandidateAnalysisResult {
        val matchPercentage = pickInt(json, "matchPercentage", "match_percentage", "match", "score")
            .coerceIn(0, 100)

        val strengths = pickStringList(json, "strengths")
        val missingSkills = pickStringList(json, "missingSkills", "missing_skills", "missing")
        val recommendation = pickString(json, "recommendation", "finalRecommendation", "final_recommendation")
            .ifBlank { "No recommendation available" }

        return CandidateAnalysisResult(
            matchPercentage = matchPercentage,
            strengths = strengths,
            missingSkills = missingSkills,
            recommendation = recommendation,
            fitLabel = toFitLabel(matchPercentage)
        )
    }

    private fun toFitLabel(score: Int): String {
        return when {
            score >= 80 -> "Strong Fit"
            score >= 55 -> "Moderate Fit"
            else -> "Needs Improvement"
        }
    }

    private fun pickInt(json: JsonObject, vararg keys: String): Int {
        for (key in keys) {
            val element = json.get(key)
            if (element != null && !element.isJsonNull) {
                return element.asInt
            }
        }
        return 0
    }

    private fun pickString(json: JsonObject, vararg keys: String): String {
        for (key in keys) {
            val element = json.get(key)
            if (element != null && !element.isJsonNull) {
                return element.asString
            }
        }
        return ""
    }

    private fun pickStringList(json: JsonObject, vararg keys: String): List<String> {
        for (key in keys) {
            val element = json.get(key) ?: continue
            if (element.isJsonNull) continue
            return when {
                element.isJsonArray -> toStringList(element.asJsonArray)
                element.isJsonPrimitive -> element.asString.split("\n", ",")
                    .map { it.trim().trimStart('-', '•', '*') }
                    .filter { it.isNotBlank() }
                else -> emptyList()
            }
        }
        return emptyList()
    }

    private fun toStringList(array: JsonArray): List<String> {
        val result = mutableListOf<String>()
        array.forEach { item: JsonElement ->
            val value = item.asString.trim()
            if (value.isNotBlank()) {
                result.add(value)
            }
        }
        return result
    }
}
