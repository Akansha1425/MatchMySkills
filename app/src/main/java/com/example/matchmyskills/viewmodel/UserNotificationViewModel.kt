package com.example.matchmyskills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchmyskills.model.UserNotification
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
class UserNotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notificationsState = MutableStateFlow<UiState<List<UserNotification>>>(UiState.Loading)
    val notificationsState: StateFlow<UiState<List<UserNotification>>> = _notificationsState

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    init {
        startNotificationListeners()
    }

    private fun startNotificationListeners() {
        val userId = authRepository.getCurrentUser()?.id
        if (userId.isNullOrBlank()) {
            _notificationsState.value = UiState.Empty
            _unreadCount.value = 0
            return
        }

        viewModelScope.launch {
            notificationRepository.getNotificationsByUser(userId).collectLatest { state ->
                _notificationsState.value = state
            }
        }

        viewModelScope.launch {
            notificationRepository.getUserUnreadCount(userId).collectLatest { count ->
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
