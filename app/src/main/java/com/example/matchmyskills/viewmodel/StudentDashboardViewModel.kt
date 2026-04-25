package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.DashboardStats
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.repository.StudentDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentDashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStats = DashboardStats(),
    val userName: String = "Student",
    val location: String = "Fetching location...",
    val profileImageUrl: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = stats.totalOpportunities == 0
}

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dashboardRepository: StudentDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "User not logged in"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val profileDeferred = async { dashboardRepository.fetchProfile(currentUser.id) }
                val statsDeferred = async { dashboardRepository.fetchStats(currentUser.id) }

                val profile = profileDeferred.await()
                val stats = statsDeferred.await()

                _uiState.value = StudentDashboardUiState(
                    isLoading = false,
                    stats = stats,
                    userName = profile.name,
                    location = profile.location?.ifBlank { "Location unavailable" } ?: "Location unavailable",
                    profileImageUrl = profile.profileImageUrl,
                    errorMessage = null
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }

    fun updateLocation(location: String) {
        val currentUser = authRepository.getCurrentUser() ?: return
        _uiState.update { it.copy(location = location) }

        viewModelScope.launch {
            runCatching {
                dashboardRepository.updateLocation(currentUser.id, location)
            }
        }
    }
}
