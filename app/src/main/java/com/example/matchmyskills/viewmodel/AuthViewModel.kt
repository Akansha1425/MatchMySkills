package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.User
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.util.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<User>>(UiState.Empty)
    val authState: StateFlow<UiState<User>> = _authState

    private val _onboardingState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val onboardingState: StateFlow<UiState<Unit>> = _onboardingState

    private val _profileState = MutableStateFlow<UiState<User>>(UiState.Empty)
    val profileState: StateFlow<UiState<User>> = _profileState

    fun login(email: String, pword: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pword).await()
                val uid = result.user?.uid ?: throw Exception("Login failed")
                val userState = repository.getUserProfile(uid)
                _authState.value = userState
            } catch (e: Exception) {
                _authState.value = UiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun register(name: String, email: String, pword: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, pword).await()
                val uid = result.user?.uid ?: throw Exception("Registration failed")
                val user = User(id = uid, name = name, email = email, role = "recruiter")
                repository.createUserProfile(user)
                _authState.value = UiState.Success(user)
            } catch (e: Exception) {
                _authState.value = UiState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun completeOnboarding(companyName: String, industry: String, website: String) {
        viewModelScope.launch {
            _onboardingState.value = UiState.Loading
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val result = repository.completeOnboarding(currentUser.uid, companyName, industry, website)
                _onboardingState.value = result
            } else {
                _onboardingState.value = UiState.Error("Not authenticated")
            }
        }
    }

    fun observeProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                repository.observeUserProfile(currentUser.uid).collectLatest { state ->
                    _profileState.value = state
                }
            }
        }
    }

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val updateState: StateFlow<UiState<Unit>> = _updateState

    private val _imageUploadState = MutableStateFlow<UiState<String>>(UiState.Empty)
    val imageUploadState: StateFlow<UiState<String>> = _imageUploadState

    fun updateProfile(updates: Map<String, Any?>) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            repository.updateUserProfile(currentUser.uid, updates).collect { state ->
                _updateState.value = state
            }
        }
    }

    fun uploadProfileImage(uri: android.net.Uri) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            repository.uploadProfileImage(currentUser.uid, uri).collect { state ->
                _imageUploadState.value = state
            }
        }
    }

    fun resetState() {
        _authState.value = UiState.Empty
        _onboardingState.value = UiState.Empty
        _updateState.value = UiState.Empty
        _imageUploadState.value = UiState.Empty
    }
}
