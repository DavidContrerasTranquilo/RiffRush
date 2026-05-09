package com.example.riffrush.data.repository

import com.example.riffrush.data.local.PlayerIdentityManager
import com.example.riffrush.data.model.ChallengeModel
import com.example.riffrush.data.model.MatchModel
import com.example.riffrush.data.model.MatchStatus
import com.example.riffrush.data.model.MatchmakingQueueEntry
import com.example.riffrush.data.model.MatchmakingResult
import com.example.riffrush.data.model.RoundResultModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MatchRepository(
    database: FirebaseDatabase,
    private val playerIdentityManager: PlayerIdentityManager
) {
    private val matchesRef: DatabaseReference = database.getReference("matches")
    private val queueRef: DatabaseReference = database.getReference("matchmaking_queue")
    private val challengesRef: DatabaseReference = database.getReference("challenges")


    suspend fun createMatch(match: MatchModel) {
        if (match.matchId == "offline_test_id") return
        matchesRef.child(match.matchId).setValue(match).await()
    }

    fun listenToMatch(matchId: String): Flow<MatchModel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val match = snapshot.getValue(MatchModel::class.java)
                trySend(match)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = matchesRef.child(matchId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateMatchStatus(matchId: String, status: MatchStatus) {
        matchesRef.child(matchId).child("status").setValue(status.name).await()
    }

    fun getOfflineMatch(category: String, songIds: List<Long>): MatchModel {
        return MatchModel(
            matchId = "offline_${category.lowercase()}",
            player1Id = getCurrentUserId(),
            player2Id = "bot",
            player1Name = "Tú",
            player2Name = "Bot",
            status = MatchStatus.PLAYING,
            staticSongList = songIds,
            currentRound = 0,
            scoreP1 = 0,
            scoreP2 = 0,
            answeredP1 = false,
            answeredP2 = false
        )
    }

    fun getCurrentUserId(): String {
        return playerIdentityManager.getPlayerId()
    }

    private fun buildMatch(
        matchId: String,
        player1Id: String,
        player1Name: String,
        player2Id: String,
        player2Name: String,
        playlist: String,
        songIds: List<Long>
    ): MatchModel {

        return MatchModel(
            matchId = matchId,
            player1Id = player1Id,
            player2Id = player2Id,
            player1Name = player1Name.ifBlank { "Invitado" },
            player2Name = player2Name.ifBlank { "Invitado" },
            playlist = playlist,
            status = MatchStatus.PLAYING,
            staticSongList = songIds,
            currentRound = 0,
            scoreP1 = 0,
            scoreP2 = 0,
            answeredP1 = false,
            answeredP2 = false,
            player1LastSeen = System.currentTimeMillis(),
            player2LastSeen = System.currentTimeMillis()
        )
    }

    suspend fun joinMatchmaking(playlist: String, songIds: List<Long>): MatchmakingResult {
        val playerId = playerIdentityManager.getPlayerId()
        val playerName = playerIdentityManager.getPlayerName()
        val isGuest = playerIdentityManager.isGuest()

        val snapshot = queueRef.orderByChild("status").equalTo("WAITING").get().await()

        val rivalDoc = snapshot.children.firstOrNull {
            it.child("playlist").getValue(String::class.java) == playlist &&
                    it.child("playerId").getValue(String::class.java) != playerId
        }

        return if (rivalDoc != null) {
            val rivalPlayerId = rivalDoc.child("playerId").getValue(String::class.java) ?: ""
            val rivalPlayerName =
                rivalDoc.child("playerName").getValue(String::class.java) ?: "Invitado"
            val matchId = matchesRef.push().key ?: "unknown_match"

            val match = buildMatch(
                matchId = matchId,
                player1Id = rivalPlayerId,
                player1Name = rivalPlayerName,
                player2Id = playerId,
                player2Name = playerName,
                playlist = playlist,
                songIds = songIds
            )

            matchesRef.child(matchId).setValue(match).await()
            val updates = mapOf(
                "status" to "MATCHED",
                "matchId" to matchId
            )
            rivalDoc.ref.updateChildren(updates).await()

            MatchmakingResult.Matched(matchId)
        } else {
            val queueId = queueRef.push().key ?: return MatchmakingResult.Waiting("")
            val entry = MatchmakingQueueEntry(
                queueId = queueId,
                playerId = playerId,
                playerName = playerName,
                playlist = playlist,
                status = "WAITING",
                matchId = null,
                isGuest = isGuest
            )

            queueRef.child(queueId).setValue(entry).await()
            MatchmakingResult.Waiting(queueId)
        }
    }

    fun listenToQueueEntry(queueId: String): Flow<MatchmakingQueueEntry?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entry = snapshot.getValue(MatchmakingQueueEntry::class.java)
                trySend(entry)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = queueRef.child(queueId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun cancelMatchmaking(queueId: String) {
        queueRef.child(queueId).removeValue().await()
    }

    suspend fun submitAnswer(matchId: String, isCorrect: Boolean) =
        suspendCancellableCoroutine { continuation ->
            val currentUserId = getCurrentUserId()
            val matchRef = matchesRef.child(matchId)

            android.util.Log.d("MATCH_DEBUG", "submitAnswer START matchId=$matchId")

            matchRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val match = currentData.getValue(MatchModel::class.java)
                        ?: return Transaction.abort()

                    val amIPlayer1 = currentUserId == match.player1Id
                    val amIPlayer2 = currentUserId == match.player2Id

                    if (!amIPlayer1 && !amIPlayer2) return Transaction.abort()
                    if (match.status != MatchStatus.PLAYING) return Transaction.success(currentData)

                    val alreadyAnswered = if (amIPlayer1) match.answeredP1 else match.answeredP2
                    if (alreadyAnswered) return Transaction.success(currentData)

                    var newScoreP1 = match.scoreP1
                    var newScoreP2 = match.scoreP2

                    if (isCorrect) {
                        if (amIPlayer1) newScoreP1 += 1 else newScoreP2 += 1
                    }

                    val newAnsweredP1 = if (amIPlayer1) true else match.answeredP1
                    val newAnsweredP2 = if (amIPlayer2) true else match.answeredP2

                    val bothAnswered = newAnsweredP1 && newAnsweredP2
                    val isLastRound = match.currentRound >= match.staticSongList.size - 1

                    if (bothAnswered) {
                        val currentSongId = match.staticSongList.getOrNull(match.currentRound)
                        val newRoundResults = match.roundResults.toMutableList()
                        newRoundResults.add(
                            RoundResultModel(
                                round = match.currentRound + 1,
                                songTitle = currentSongId?.toString() ?: "Canción desconocida",
                                songArtist = "",
                                player1Correct = newScoreP1 > match.scoreP1,
                                player2Correct = newScoreP2 > match.scoreP2
                            )
                        )

                        if (isLastRound) {
                            currentData.value = match.copy(
                                scoreP1 = newScoreP1,
                                scoreP2 = newScoreP2,
                                answeredP1 = true,
                                answeredP2 = true,
                                status = MatchStatus.FINISHED,
                                roundResults = newRoundResults
                            )
                        } else {
                            currentData.value = match.copy(
                                scoreP1 = newScoreP1,
                                scoreP2 = newScoreP2,
                                answeredP1 = false,
                                answeredP2 = false,
                                currentRound = match.currentRound + 1,
                                roundResults = newRoundResults
                            )
                        }
                    } else {
                        currentData.value = match.copy(
                            scoreP1 = newScoreP1,
                            scoreP2 = newScoreP2,
                            answeredP1 = newAnsweredP1,
                            answeredP2 = newAnsweredP2
                        )
                    }

                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                    if (error != null) {
                        android.util.Log.e("MATCH_DEBUG", "submitAnswer error: ${error.message}")
                        continuation.resumeWithException(error.toException())
                    } else {
                        continuation.resume(Unit)
                    }
                }
            })
        }

    suspend fun abandonMatch(matchId: String) = suspendCancellableCoroutine{ continuation ->
        val currentUserId = getCurrentUserId()
        val matchRef = matchesRef.child(matchId)

        matchRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val match = currentData.getValue(MatchModel::class.java)
                    ?: return Transaction.abort()

                if (match.status != MatchStatus.PLAYING) return Transaction.success(currentData)

                val winnerId = when (currentUserId) {
                    match.player1Id -> match.player2Id
                    match.player2Id -> match.player1Id
                    else -> ""
                }

                currentData.value = match.copy(
                    status = MatchStatus.FINISHED,
                    abandonedBy = currentUserId,
                    winnerId = winnerId,
                    endReason = "ABANDONED"
                )

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (error != null) {
                    continuation.resumeWithException(error.toException())
                } else {
                    continuation.resume(Unit)
                }
            }
        })
    }

    suspend fun updatePresence(matchId: String, isPlayer1: Boolean) {
        val field = if (isPlayer1) "player1LastSeen" else "player2LastSeen"
        try {
            matchesRef.child(matchId).child(field).setValue(System.currentTimeMillis()).await()
        } catch (e: Exception) {
            android.util.Log.e("MATCH_DEBUG", "Error updating presence: ${e.message}")
        }
    }

    suspend fun endMatchByConnectionLoss(
        matchId: String,
        disconnectedPlayerId: String,
        winnerId: String
    ) = suspendCancellableCoroutine { continuation ->
        val matchRef = matchesRef.child(matchId)

        matchRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val match = currentData.getValue(MatchModel::class.java)
                    ?: return Transaction.abort()

                if (match.status != MatchStatus.PLAYING) return Transaction.success(currentData)

                currentData.value = match.copy(
                    status = MatchStatus.FINISHED,
                    endReason = "CONNECTION_LOST",
                    abandonedBy = disconnectedPlayerId,
                    winnerId = winnerId
                )

                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (error != null) {
                    continuation.resumeWithException(error.toException())
                } else {
                    continuation.resume(Unit)
                }
            }
        })
    }

    // ── Challenge System ──

    suspend fun createChallenge(challenge: ChallengeModel) {
        challengesRef.child(challenge.challengeId).setValue(challenge).await()
    }

    fun listenToIncomingChallenges(myUid: String): Flow<List<ChallengeModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val challenges = snapshot.children.mapNotNull {
                    it.getValue(ChallengeModel::class.java)
                }.filter {
                    it.toUid == myUid && it.status == "PENDING"
                }
                trySend(challenges)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        challengesRef.orderByChild("toUid").equalTo(myUid).addValueEventListener(listener)
        awaitClose { challengesRef.removeEventListener(listener) }
    }

    suspend fun acceptChallenge(challengeId: String, songIds: List<Long>): String {
        val snapshot = challengesRef.child(challengeId).get().await()
        val challenge = snapshot.getValue(ChallengeModel::class.java)
            ?: throw Exception("Challenge not found")

        val matchId = matchesRef.push().key ?: throw Exception("Could not create match ID")

        val match = buildMatch(
            matchId = matchId,
            player1Id = challenge.fromUid,
            player1Name = challenge.fromName,
            player2Id = challenge.toUid,
            player2Name = challenge.toName,
            playlist = challenge.playlist,
            songIds = songIds
        )

        matchesRef.child(matchId).setValue(match).await()

        challengesRef.child(challengeId).updateChildren(
            mapOf("status" to "ACCEPTED", "matchId" to matchId)
        ).await()

        return matchId
    }

    suspend fun declineChallenge(challengeId: String) {
        challengesRef.child(challengeId).child("status").setValue("DECLINED").await()
    }

    fun listenToChallenge(challengeId: String): Flow<ChallengeModel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(ChallengeModel::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = challengesRef.child(challengeId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun cleanupChallenge(challengeId: String) {
        challengesRef.child(challengeId).removeValue().await()
    }
}