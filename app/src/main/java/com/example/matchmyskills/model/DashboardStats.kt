package com.example.matchmyskills.model

data class DashboardStats(
    val totalJobs: Int = 0,
    val appliedJobs: Int = 0,
    val totalInternships: Int = 0,
    val appliedInternships: Int = 0,
    val totalHackathons: Int = 0,
    val appliedHackathons: Int = 0
) {
    val totalApplications: Int
        get() = appliedJobs + appliedInternships + appliedHackathons

    val totalOpportunities: Int
        get() = totalJobs + totalInternships + totalHackathons

    val activityProgress: Float
        get() = if (totalOpportunities == 0) 0f else totalApplications.toFloat() / totalOpportunities.toFloat()
}
