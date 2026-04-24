package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.repository.ApplicationRepository
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.repository.HackathonRepository
import com.example.matchmyskills.repository.JobRepository
import com.example.matchmyskills.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val hackathonRepository: HackathonRepository,
    private val applicationRepository: ApplicationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val dashboardState: StateFlow<UiState<DashboardData>> = _dashboardState

    data class DashboardData(
        val jobs: List<Job>,
        val hackathons: List<Hackathon>,
        val applicationCountByOpportunityId: Map<String, Int>,
        val totalApplicants: Int,
        val shortlistedCount: Int,
        val rejectedCount: Int,
        val pendingCount: Int,
        val hiredCount: Int
    )

    init {
        fetchDashboardData()
    }

    fun fetchDashboardData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _dashboardState.value = UiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            combine(
                jobRepository.getJobsByRecruiter(currentUser.id),
                hackathonRepository.getHackathonsByRecruiter(currentUser.id),
                applicationRepository.getApplicationsByRecruiter(currentUser.id)
            ) { jobsState, hackathonsState, appsState ->
                when {
                    jobsState is UiState.Error -> UiState.Error(jobsState.message)
                    hackathonsState is UiState.Error -> UiState.Error(hackathonsState.message)
                    appsState is UiState.Error -> UiState.Error(appsState.message)
                    
                    jobsState is UiState.Loading || hackathonsState is UiState.Loading || appsState is UiState.Loading -> UiState.Loading
                    
                    else -> {
                        val jobs = (jobsState as? UiState.Success)?.data ?: emptyList()
                        val hackathons = (hackathonsState as? UiState.Success)?.data ?: emptyList()
                        val apps = (appsState as? UiState.Success)?.data ?: emptyList()
                        
                        if (jobs.isEmpty() && hackathons.isEmpty() && apps.isEmpty()) {
                            UiState.Empty
                        } else {
                            val groupedCounts = apps
                                .groupingBy { if (it.opportunityId.isNotBlank()) it.opportunityId else it.jobId }
                                .eachCount()

                            UiState.Success(
                                DashboardData(
                                    jobs = jobs,
                                    hackathons = hackathons,
                                    applicationCountByOpportunityId = groupedCounts,
                                    totalApplicants = apps.size,
                                    shortlistedCount = apps.count { it.status == "Shortlisted" },
                                    rejectedCount = apps.count { it.status == "Rejected" },
                                    pendingCount = apps.count { it.status == "Pending" || it.status == "Applied" },
                                    hiredCount = apps.count { it.status == "Hired" }
                                )
                            )
                        }
                    }
                }
            }.collect { state ->
                _dashboardState.value = state
            }
        }
    }
}
