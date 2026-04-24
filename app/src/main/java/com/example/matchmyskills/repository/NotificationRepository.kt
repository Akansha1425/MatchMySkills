package com.example.matchmyskills.repository

import android.util.Log
import com.example.matchmyskills.model.RecruiterNotification
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.util.toRecruiterNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getNotificationsByRecruiter(recruiterId: String): Flow<UiState<List<RecruiterNotification>>> = callbackFlow {
        trySend(UiState.Loading)

        val listener = firestore.collection("notifications")
            .whereEqualTo("recruiterId", recruiterId)
            .addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error fetching notifications: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch notifications"))
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents
                    ?.mapNotNull { it.toRecruiterNotification() }
                    ?.sortedByDescending { it.timestamp?.time ?: 0L }
                    ?: emptyList()
                trySend(if (notifications.isEmpty()) UiState.Empty else UiState.Success(notifications))
            }

        awaitClose { listener.remove() }
    }

    fun getUnreadCount(recruiterId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("recruiterId", recruiterId)
            .addSnapshotListener { snapshot, _ ->
                val unreadCount = snapshot?.documents
                    ?.mapNotNull { it.toRecruiterNotification() }
                    ?.count { !it.isRead }
                    ?: 0
                trySend(unreadCount)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): UiState<Unit> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to mark notification as read")
        }
    }

    suspend fun deleteNotification(notificationId: String): UiState<Unit> {
        return try {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to delete notification")
        }
    }

    suspend fun createApplicationNotification(
        recruiterId: String,
        message: String,
        jobId: String
    ): UiState<Unit> {
        return try {
            val notificationData = hashMapOf(
                "recruiterId" to recruiterId,
                "message" to message,
                "jobId" to jobId,
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false
            )

            firestore.collection("notifications")
                .add(notificationData)
                .await()

            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to create notification")
        }
    }
}
