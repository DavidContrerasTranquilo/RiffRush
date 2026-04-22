package com.example.riffrush.data.repository

import com.example.riffrush.data.model.MatchModel
import com.example.riffrush.data.model.MatchStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.riffrush.data.local.PlayerIdentityManager
import com.example.riffrush.data.model.MatchmakingResult
import com.example.riffrush.data.model.MatchmakingQueueEntry

class MatchRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val playerIdentityManager: PlayerIdentityManager
) {
    private val matchesCollection = firestore.collection("matches")
    private val queueCollection = firestore.collection("matchmaking_queue")

    private val playlists = mapOf(
        "REGGAETON" to listOf(
            2223276187L, 143781198L, 3776310102L, 2758436071L, 3498392331L, 3104740891L, 857827232L, 731498882L, 3179773841L, 1759650657L, 3298075621L, 3276167871L, 2473410281L, 3422751531L, 1704442217L, 2322228845L, 2074439067L
        ),
        "RAP FRANCÉS" to listOf(
            600107922L, 122927610L, 132950108L, 783256992L, 3055454281L, 429975052L, 823093212L, 3242688291L, 450100912L, 132123630L, 1555328112L, 3023399701L, 2892578472L, 106095086L, 2781673612L, 1426733732L, 519797812L, 519797762L
        ),
        "ROCK ESPAÑOL" to listOf(
            707777L, 778608L, 9943121L, 3234733081L, 2935686241L, 988453L, 11746766L, 778615L, 3241426491L, 3912803111L, 3234733041L, 7701507L, 14645355L, 3539198461L, 3092625L, 7701520L, 3539198321L, 3234306461L, 2471353181L, 4738765L
        ),
        "TOP 50 ESPAÑA" to listOf(
            3947383241L, 3852034981L, 37027991L, 3843020331L, 3733293352L, 3677908682L, 3911872941L, 3852034871L, 3380325281L, 3776310102L, 3852034971L, 3613707422L, 3241972661L, 3640310582L, 3856864621L, 3407512021L, 3640140952L, 3171002981L
        )
    )

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

    fun getOfflineMatch(category: String): MatchModel {
        val fullList = playlists[category] ?: playlists["REGGAETON"]!!
        val randomSelection = fullList.shuffled().take(5)

        return MatchModel(
            matchId = "offline_${category.lowercase()}",
            player1Id = "me",
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
        return auth.currentUser?.uid ?: "guest_user"
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
            answeredP2 = false
        )
    }

    suspend fun joinMatchmaking(playlist: String): MatchmakingResult {
        val playerId = playerIdentityManager.getPlayerId()
        val playerName = playerIdentityManager.getPlayerName()
        val isGuest = playerIdentityManager.isGuest()

        val waitingSnapshot = queueCollection
            .whereEqualTo("playlist", playlist)
            .whereEqualTo("status", "WAITING")
            .get()
            .await()

        val rivalDoc = waitingSnapshot.documents.firstOrNull {
            it.getString("playerId") != playerId
        }

        return if (rivalDoc != null) {
            val rivalPlayerId = rivalDoc.getString("playerId") ?: ""
            val rivalPlayerName = rivalDoc.getString("playerName") ?: "Invitado"
            val matchId = queueCollection.document().id

            val match = buildMatch(
                matchId = matchId,
                player1Id = rivalPlayerId,
                player1Name = rivalPlayerName,
                player2Id = playerId,
                player2Name = playerName,
                playlist = playlist
            )

            firestore.runBatch { batch ->
                batch.set(matchesCollection.document(matchId), match)
                batch.update(queueCollection.document(rivalDoc.id), mapOf(
                    "status" to "MATCHED",
                    "matchId" to matchId
                ))
            }.await()

            MatchmakingResult.Matched(matchId)
        } else {
            val queueId = queueCollection.document().id
            val entry = MatchmakingQueueEntry(
                queueId = queueId,
                playerId = playerId,
                playerName = playerName,
                playlist = playlist,
                status = "WAITING",
                matchId = null,
                isGuest = isGuest
            )

            queueCollection.document(queueId).set(entry).await()
            MatchmakingResult.Waiting(queueId)
        }
    }

    fun listenToQueueEntry(queueId: String): Flow<MatchmakingQueueEntry?> = callbackFlow {
        val registration = queueCollection.document(queueId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entry = snapshot?.toObject(MatchmakingQueueEntry::class.java)
                trySend(entry)
            }

        awaitClose { registration.remove() }
    }

    suspend fun cancelMatchmaking(queueId: String) {
        queueCollection.document(queueId).delete().await()
    }

    suspend fun submitAnswer(matchId: String, isCorrect: Boolean) {
        val playerId = getCurrentUserId()
        val matchRef = matchesCollection.document(matchId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(matchRef)
            val match = snapshot.toObject(MatchModel::class.java) ?: return@runTransaction

            if (match.status != MatchStatus.PLAYING) return@runTransaction

            val isPlayer1 = match.player1Id == playerId
            val isPlayer2 = match.player2Id == playerId

            if (!isPlayer1 && !isPlayer2) return@runTransaction

            var scoreP1 = match.scoreP1
            var scoreP2 = match.scoreP2
            var answeredP1 = match.answeredP1
            var answeredP2 = match.answeredP2
            var currentRound = match.currentRound
            var status = match.status

            // Si ya había respondido, no hacer nada
            if (isPlayer1 && answeredP1) return@runTransaction
            if (isPlayer2 && answeredP2) return@runTransaction

            // Registrar respuesta
            if (isPlayer1) {
                answeredP1 = true
                if (isCorrect) scoreP1 += 1
            }

            if (isPlayer2) {
                answeredP2 = true
                if (isCorrect) scoreP2 += 1
            }

            // Solo avanzar si ambos respondieron
            if (answeredP1 && answeredP2) {
                val isLastRound = currentRound >= match.staticSongList.size - 1
                if (isLastRound) {
                    status = MatchStatus.FINISHED
                } else {
                    currentRound += 1
                    answeredP1 = false
                    answeredP2 = false
                }
            }

            transaction.update(matchRef, mapOf(
                "scoreP1" to scoreP1,
                "scoreP2" to scoreP2,
                "answeredP1" to answeredP1,
                "answeredP2" to answeredP2,
                "currentRound" to currentRound,
                "status" to status
            ))
        }.await()
    }
    private suspend fun getCurrentPlayerProfileData(): Triple<String, String, String> {
        val playerId = getCurrentUserId()
        val userDoc = firestore.collection("users").document(playerId).get().await()

        val name = userDoc.getString("name")
            ?: userDoc.getString("username")
            ?: userDoc.getString("displayName")
            ?: "Invitado"

        val pfpUrl = userDoc.getString("pfpUrl") ?: ""

        return Triple(playerId, name, pfpUrl)
    }
}