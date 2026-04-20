package com.example.matchmyskills.viewmodel

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

    fun resetUpdateState() {
        _updateState.value = UiState.Empty
    }
}
