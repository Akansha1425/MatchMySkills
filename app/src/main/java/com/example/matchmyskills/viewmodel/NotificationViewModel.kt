package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.RecruiterNotification
import com.example.matchmyskills.repository.AuthRepository
import com.example.matchmyskills.repository.NotificationRepository
import com.example.matchmyskills.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notificationsState = MutableStateFlow<UiState<List<RecruiterNotification>>>(UiState.Loading)
    val notificationsState: StateFlow<UiState<List<RecruiterNotification>>> = _notificationsState

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    init {
        startNotificationListeners()
    }

    private fun startNotificationListeners() {
        val recruiterId = authRepository.getCurrentUser()?.id
        if (recruiterId.isNullOrBlank()) {
            _notificationsState.value = UiState.Empty
            _unreadCount.value = 0
            return
        }

        viewModelScope.launch {
            notificationRepository.getNotificationsByRecruiter(recruiterId).collectLatest { state ->
                _notificationsState.value = state
            }
        }

        viewModelScope.launch {
            notificationRepository.getUnreadCount(recruiterId).collectLatest { count ->
                _unreadCount.value = count
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }
}
