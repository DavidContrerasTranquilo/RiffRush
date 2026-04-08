package com.example.riffrush.data.repository

import com.example.riffrush.data.model.MatchModel
import com.example.riffrush.data.model.MatchStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MatchRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val matchesCollection = firestore.collection("matches")

    suspend fun createMatch(match: MatchModel) {
        firestore.collection("matches")
            .document(match.matchId)
            .set(match)
            .await()
    }

    fun listenToMatch(matchId: String): Flow<MatchModel?> = callbackFlow {
        val registration = matchesCollection.document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val match = snapshot?.toObject(MatchModel::class.java)
                trySend(match)
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateScore(matchId: String, userId: String) {
        val matchRef = firestore.collection("matches").document(matchId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(matchRef)

            // Comparamos el ID para saber a quién sumar el punto
            val p1Id = snapshot.getString("player1Id") ?: ""

            if (userId == p1Id) {
                val currentScore = snapshot.getLong("scoreP1") ?: 0
                transaction.update(matchRef, "scoreP1", currentScore + 1)
            } else {
                val currentScore = snapshot.getLong("scoreP2") ?: 0
                transaction.update(matchRef, "scoreP2", currentScore + 1)
            }
        }.await()
    }

    suspend fun updateMatchStatus(matchId: String, status: MatchStatus) {
        matchesCollection.document(matchId).update("status", status).await()
    }
    fun getCurrentUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
}