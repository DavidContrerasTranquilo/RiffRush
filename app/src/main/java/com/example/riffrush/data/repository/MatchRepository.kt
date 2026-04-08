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

    suspend fun createMatch(opponentId: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        
        // Lista estática de IDs de canciones de Deezer (ejemplo)
        val staticSongs = listOf(
            1109731L,  // Daft Punk - One More Time
            3135556L,  // Queen - Bohemian Rhapsody
            1434890L,  // Michael Jackson - Billie Jean
            653448L,   // Nirvana - Smells Like Teen Spirit
            2107382L   // Gorillaz - Feel Good Inc.
        )

        val matchDoc = matchesCollection.document()
        val match = MatchModel(
            matchId = matchDoc.id,
            player1Id = currentUserId,
            player2Id = opponentId,
            status = MatchStatus.WAITING,
            staticSongList = staticSongs,
            scoreP1 = 0,
            scoreP2 = 0
        )

        matchDoc.set(match).await()
        return matchDoc.id
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
}