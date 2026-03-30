package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.model.Job
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.repository.HackathonRepository
import com.example.matchmyskills.repository.JobRepository
import com.example.matchmyskills.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OpportunityViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val hackathonRepository: HackathonRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _createState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val createState: StateFlow<UiState<Unit>> = _createState

    fun createInternship(
        title: String,
        description: String,
        skills: String,
        duration: String,
        stipend: String,
        deadlineDays: Int
    ) {
        val user = authRepository.getCurrentUser() ?: run {
            _createState.value = UiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _createState.value = UiState.Loading
            
            val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val deadlineDate = java.util.Date(System.currentTimeMillis() + (deadlineDays * 24 * 60 * 60 * 1000L))
            
            val job = Job(
                id = UUID.randomUUID().toString(),
                recruiterId = user.id,
                title = title,
                companyName = user.companyName ?: "Unknown",
                description = description,
                coreSkills = skillList,
                duration = "$duration Months",
                stipend = stipend,
                deadline = deadlineDate
            )
            
            _createState.value = jobRepository.createJob(job)
        }
    }

    fun createHackathon(
        title: String,
        description: String,
        themes: String,
        prize: String,
        teamSize: String,
        mode: String,
        deadlineDays: Int = 30 // Added for Date support
    ) {
        val user = authRepository.getCurrentUser() ?: run {
            _createState.value = UiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _createState.value = UiState.Loading
            
            val themeList = themes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val deadlineDate = java.util.Date(System.currentTimeMillis() + (deadlineDays * 24 * 60 * 60 * 1000L))
            
            val hackathon = Hackathon(
                id = UUID.randomUUID().toString(),
                recruiterId = user.id,
                title = title,
                organizer = user.companyName ?: "Unknown",
                description = description,
                themes = themeList,
                prizePool = prize,
                teamSize = teamSize,
                mode = mode,
                deadline = deadlineDate
            )
            
            _createState.value = hackathonRepository.createHackathon(hackathon)
        }
    }

    fun resetState() {
        _createState.value = UiState.Empty
    }
}
