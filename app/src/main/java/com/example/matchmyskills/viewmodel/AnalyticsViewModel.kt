package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.Application
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.repository.ApplicationRepository
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appRepository: ApplicationRepository,
    private val jobRepository: com.example.matchmyskills.repository.JobRepository,
    private val hackathonRepository: com.example.matchmyskills.repository.HackathonRepository
) : ViewModel() {

    data class AnalyticsData(
        val applications: List<Application>,
        val jobs: List<Job>,
        val hackathons: List<Hackathon>
    )

    private val _analyticsData = MutableStateFlow<UiState<AnalyticsData>>(UiState.Loading)
    val analyticsData: StateFlow<UiState<AnalyticsData>> = _analyticsData

    fun fetchAnalytics() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            viewModelScope.launch {
                kotlinx.coroutines.flow.combine(
                    appRepository.getApplicationsByRecruiter(currentUser.id),
                    jobRepository.getJobsByRecruiter(currentUser.id),
                    hackathonRepository.getHackathonsByRecruiter(currentUser.id)
                ) { appsState, jobsState, hackState ->
                    when {
                        appsState is UiState.Error -> UiState.Error(appsState.message)
                        jobsState is UiState.Error -> UiState.Error(jobsState.message)
                        hackState is UiState.Error -> UiState.Error(hackState.message)
                        
                        appsState is UiState.Loading || jobsState is UiState.Loading || hackState is UiState.Loading -> UiState.Loading
                        
                        else -> {
                            val apps = (appsState as? UiState.Success)?.data ?: emptyList()
                            val jobs = (jobsState as? UiState.Success)?.data ?: emptyList()
                            val hacks = (hackState as? UiState.Success)?.data ?: emptyList()
                            
                            UiState.Success(AnalyticsData(apps, jobs, hacks))
                        }
                    }
                }.collectLatest { state ->
                    _analyticsData.value = state
                }
            }
        }
    }
}
