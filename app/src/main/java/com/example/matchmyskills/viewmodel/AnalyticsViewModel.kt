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
class AnalyticsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appRepository: ApplicationRepository
) : ViewModel() {

    private val _analyticsData = MutableStateFlow<UiState<List<Application>>>(UiState.Empty)
    val analyticsData: StateFlow<UiState<List<Application>>> = _analyticsData

    fun fetchAnalytics() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            viewModelScope.launch {
                // Fetching all applications for the recruiter's jobs (simplified for demo)
                // In production, we'd have a specific analytics query
                appRepository.getApplicationsByRecruiter(currentUser.id).collectLatest { state ->
                    _analyticsData.value = state
                }
            }
        }
    }
}
