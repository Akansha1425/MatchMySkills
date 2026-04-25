package com.example.matchmyskills.repository

import android.util.Log
import com.example.matchmyskills.model.RecruiterNotification
import com.example.matchmyskills.model.UserNotification
import com.example.matchmyskills.util.UiState
import com.example.matchmyskills.util.toRecruiterNotification
import com.example.matchmyskills.util.toUserNotification
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

    fun getNotificationsByUser(userId: String): Flow<UiState<List<UserNotification>>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(UiState.Empty)
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading)

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) return@addSnapshotListener
                
                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed"))
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { it.toUserNotification() } ?: emptyList()
                
                if (notifications.isEmpty()) {
                    // Try fallback to candidateId once if userId returns empty
                    // Note: Snapshot listener for candidateId would require a separate flow or complex logic
                    // For now, let's just use a one-time fetch for fallback if needed
                    firestore.collection("notifications")
                        .whereEqualTo("candidateId", userId)
                        .get()
                        .addOnSuccessListener { fallbackSnapshot ->
                            val fallbackList = fallbackSnapshot.documents.mapNotNull { it.toUserNotification() }
                                .sortedByDescending { it.timestamp?.time ?: 0L }
                            trySend(if (fallbackList.isEmpty()) UiState.Empty else UiState.Success(fallbackList))
                        }
                } else {
                    trySend(UiState.Success(notifications.sortedByDescending { it.timestamp?.time ?: 0L }))
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserUnreadCount(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val countFromUserId = snapshot?.documents
                    ?.mapNotNull { it.toUserNotification() }
                    ?.count { !it.isRead } ?: 0
                
                // Fallback to candidateId check for unread count
                firestore.collection("notifications")
                    .whereEqualTo("candidateId", userId)
                    .get()
                    .addOnSuccessListener { fallbackSnapshot ->
                        val countFromCandidateId = fallbackSnapshot.documents
                            .mapNotNull { it.toUserNotification() }
                            .count { !it.isRead }
                        
                        // Combine counts (deduplicated by ID if necessary, but here we just want a simple check)
                        // Actually, if we save to both, they might be the same document. 
                        // So we just take the max or better yet, deduplicate.
                        val allUnreadDocs = (snapshot?.documents ?: emptyList()) + fallbackSnapshot.documents
                        val uniqueUnreadCount = allUnreadDocs
                            .mapNotNull { it.toUserNotification() }
                            .distinctBy { it.id }
                            .count { !it.isRead }
                            
                        trySend(uniqueUnreadCount)
                    }
            }
        awaitClose { listener.remove() }
    }
}
