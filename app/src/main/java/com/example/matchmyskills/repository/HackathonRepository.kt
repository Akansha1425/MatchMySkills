package com.example.matchmyskills.repository

import com.example.matchmyskills.model.Hackathon
import com.example.matchmyskills.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.matchmyskills.util.toHackathon
import com.example.matchmyskills.util.getDateSafe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HackathonRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getHackathonsByRecruiter(recruiterId: String): Flow<UiState<List<Hackathon>>> = callbackFlow {
        trySend(UiState.Loading)
        
        val listener = firestore.collection("hackathons")
            .whereEqualTo("recruiterId", recruiterId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (FirebaseAuth.getInstance().currentUser == null) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e("FIREBASE_ERROR", "Error fetching hackathons: ${error.message}")
                    trySend(UiState.Error(error.message ?: "Failed to fetch hackathons"))
                    return@addSnapshotListener
                }
                
                val hackathons = snapshot?.documents?.mapNotNull { doc ->
                    doc.toHackathon()
                } ?: emptyList()
                
                if (hackathons.isEmpty()) {
                    trySend(UiState.Empty)
                } else {
                    trySend(UiState.Success(hackathons))
                }
            }
            
        awaitClose { listener.remove() }
    }

    suspend fun createHackathon(hackathon: Hackathon): UiState<Unit> {
        return try {
            firestore.collection("hackathons").document(hackathon.id).set(hackathon).await()
            // Add server timestamp explicitly after setting the object to ensure it's picked up
            firestore.collection("hackathons").document(hackathon.id).update("createdAt", FieldValue.serverTimestamp()).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to create hackathon")
        }
    }
}
