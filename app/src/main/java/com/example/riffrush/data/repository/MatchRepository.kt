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
        if (match.matchId == "offline_test_id") return
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")

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

    suspend fun updateScore(matchId: String, playerNumber: Int, newScore: Int) {
        val field = if (playerNumber == 1) "scoreP1" else "scoreP2"
        matchesCollection.document(matchId).update(field, newScore).await()
    }

    suspend fun updateMatchStatus(matchId: String, status: MatchStatus) {
        matchesCollection.document(matchId).update("status", status).await()
    }

    fun getOfflineMatch(): MatchModel {
        return MatchModel(
            matchId = "offline_test_id",
            player1Id = "me",
            player2Id = "bot",
            status = MatchStatus.PLAYING,
            staticSongList = listOf(1109152L, 2109522L, 3109522L),
            scoreP1 = 0,
            scoreP2 = 0
        )
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "guest_user"
    }
}