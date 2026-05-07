package com.example.riffrush.data.repository

import com.example.riffrush.data.local.PlayerIdentityManager
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

    private val playlists = mapOf(
        "REGGAETON" to listOf(
            2223276187L, 143781198L, 3776310102L, 2758436071L, 3498392331L,
            3104740891L, 857827232L, 731498882L, 3179773841L, 1759650657L,
            3298075621L, 3276167871L, 2473410281L, 3422751531L, 1704442217L,
            2322228845L, 2074439067L
        ),
        "RAP FRANCÉS" to listOf(
            600107922L, 122927610L, 132950108L, 783256992L, 3055454281L,
            429975052L, 823093212L, 3242688291L, 450100912L, 132123630L,
            1555328112L, 3023399701L, 2892578472L, 106095086L, 2781673612L,
            1426733732L, 519797812L, 519797762L
        ),
        "ROCK ESPAÑOL" to listOf(
            707777L, 778608L, 9943121L, 3234733081L, 2935686241L,
            988453L, 11746766L, 778615L, 3241426491L, 3912803111L,
            3234733041L, 7701507L, 14645355L, 3539198461L, 3092625L,
            7701520L, 3539198321L, 3234306461L, 2471353181L, 4738765L
        ),
        "TOP 50 ESPAÑA" to listOf(
            3947383241L, 3852034981L, 37027991L, 3843020331L, 3733293352L,
            3677908682L, 3911872941L, 3852034871L, 3380325281L, 3776310102L,
            3852034971L, 3613707422L, 3241972661L, 3640310582L, 3856864621L,
            3407512021L, 3640140952L, 3171002981L
        )
    )

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

    fun getOfflineMatch(category: String): MatchModel {
        val fullList = playlists[category] ?: playlists["REGGAETON"]!!
        val randomSelection = fullList.shuffled().take(5)

        return MatchModel(
            matchId = "offline_${category.lowercase()}",
            player1Id = getCurrentUserId(),
            player2Id = "bot",
            player1Name = "Tú",
            player2Name = "Bot",
            status = MatchStatus.PLAYING,
            staticSongList = randomSelection,
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
        playlist: String
    ): MatchModel {
        val songIds = (playlists[playlist] ?: playlists["REGGAETON"].orEmpty())
            .shuffled()
            .take(5)

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

    suspend fun joinMatchmaking(playlist: String): MatchmakingResult {
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
                playlist = playlist
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
}