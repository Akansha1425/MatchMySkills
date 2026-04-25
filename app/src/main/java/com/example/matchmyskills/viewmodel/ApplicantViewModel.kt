package com.example.matchmyskills.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.Application
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
class ApplicantViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _applicantsState = MutableStateFlow<UiState<List<Application>>>(UiState.Empty)
    val applicantsState: StateFlow<UiState<List<Application>>> = _applicantsState

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val updateState: StateFlow<UiState<Unit>> = _updateState

    private var allApplicants: List<Application> = emptyList()
    private var lastQuery: String = ""

    fun fetchApplicants(jobId: String?, statuses: List<String> = emptyList()) {
        viewModelScope.launch {
            if (jobId != null && jobId != "null") {
                repository.getApplicationsByJob(jobId, statuses).collectLatest { state ->
                    if (state is UiState.Success) {
                        allApplicants = state.data.sortedByDescending { it.createdAt?.time ?: 0L }
                        applySearchFilter(lastQuery)
                    } else {
                        _applicantsState.value = state
                    }
                }
            } else {
                val recruiterId = authRepository.getCurrentUser()?.id
                if (recruiterId != null) {
                    repository.getApplicationsByRecruiter(recruiterId, statuses).collectLatest { state ->
                        if (state is UiState.Success) {
                            allApplicants = state.data.sortedByDescending { it.createdAt?.time ?: 0L }
                            applySearchFilter(lastQuery)
                        } else {
                            _applicantsState.value = state
                        }
                    }
                } else {
                    _applicantsState.value = UiState.Error("User not authenticated")
                }
            }
        }
    }

    fun searchCandidates(query: String) {
        lastQuery = query
        applySearchFilter(query)
    }

    private fun applySearchFilter(query: String) {
        if (query.isBlank()) {
            _applicantsState.value = if (allApplicants.isEmpty()) UiState.Empty else UiState.Success(allApplicants)
            return
        }
        val filtered = allApplicants.filter { 
            it.candidateName.contains(query, ignoreCase = true) || 
            it.candidateCollege.contains(query, ignoreCase = true)
        }
        _applicantsState.value = if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
    }

    fun updateStatus(applicationId: String, status: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            val result = repository.updateApplicationStatus(applicationId, status)
            _updateState.value = result
        }
    }

    fun shortlistCandidate(
        applicationId: String,
        candidateId: String,
        jobTitle: String,
        candidateEmail: String,
        candidateName: String,
        opportunityId: String = "",
        opportunityType: String = ""
    ) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            
            // 1. Update Application Status
            val statusResult = repository.updateApplicationStatus(applicationId, "Shortlisted")
            if (statusResult is UiState.Error) {
                _updateState.value = statusResult
                return@launch
            }

            // 2. Create In-App Notification
            val notificationMsg = "You have been shortlisted for $jobTitle"
            val notifyResult = repository.createNotification(
                userId = candidateId,
                message = notificationMsg,
                type = "shortlisted",
                opportunityId = opportunityId,
                opportunityType = opportunityType
            )
            
            if (notifyResult is UiState.Error) {
                Log.w("RecruiterAction", "Notification failed but status updated: ${notifyResult.message}")
            }

            // 3. Send Email Notification
            repository.sendEmailApi(candidateEmail, candidateName, jobTitle)

            _updateState.value = UiState.Success(Unit)
        }
    }

    fun rejectCandidate(applicationId: String, candidateId: String, jobTitle: String, opportunityId: String = "", opportunityType: String = "") {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            val result = repository.updateApplicationStatus(applicationId, "Rejected")
            if (result is UiState.Success) {
                val msg = "Application status update for $jobTitle: Rejected"
                repository.createNotification(
                    userId = candidateId, 
                    message = msg, 
                    type = "rejected",
                    opportunityId = opportunityId,
                    opportunityType = opportunityType
                )
            }
            _updateState.value = result
        }
    }

    fun resetUpdateState() {
        _updateState.value = UiState.Empty
    }
}
