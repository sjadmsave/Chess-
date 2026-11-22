package com.example.chess

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object ChessEngine {

    private const val INF = 1_000_000
    private const val MATE_SCORE = 100_000

    private const val TT_EXACT: Byte = 0
    private const val TT_LOWER: Byte = 1
    private const val TT_UPPER: Byte = 2
    private const val TT_BITS = 14
    private const val TT_SIZE = 1 shl TT_BITS
    private const val TT_MASK = TT_SIZE - 1

    data class SearchResult(
        val bestMove: ChessMove?,
        val evalWhiteCp: Int,
        val depthReached: Int,
        val nodesSearched: Int = 0,
        val pvUci: List<String> = emptyList(),
        val sourceName: String = "المحرك المدمج (TT + PVS + LMR)"
    )

    // Deterministic 64-bit Zobrist tables (Stockfish-style transposition keys)
    private val ZOBRIST_PIECE = Array(12) { LongArray(64) }
    private val ZOBRIST_CASTLING = LongArray(16)
    private val ZOBRIST_EP_FILE = LongArray(8)
    private var ZOBRIST_BLACK_TURN: Long = 0L

    init {
        val rng = Random(0x570CF15AL)
        for (p in 0 until 12) {
            for (sq in 0 until 64) {
                ZOBRIST_PIECE[p][sq] = rng.nextLong()
            }
        }
        for (i in 0 until 16) {
            ZOBRIST_CASTLING[i] = rng.nextLong()
        }
        for (f in 0 until 8) {
            ZOBRIST_EP_FILE[f] = rng.nextLong()
        }
        ZOBRIST_BLACK_TURN = rng.nextLong()
    }

    // Tapered Piece-Square Tables (White's perspective, index 0 = a8 .. 63 = h1)
    private val PAWN_MG_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        55, 55, 55, 55, 55, 55, 55, 55,
        14, 14, 24, 34, 34, 24, 14, 14,
        6,  6, 16, 30, 30, 16,  6,  6,
        2,  2, 10, 26, 26, 10,  2,  2,
        4, -4,-10,  0,  0,-10, -4,  4,
        8, 12, 12,-22,-22, 12, 12,  8,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val PAWN_EG_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        85, 85, 85, 85, 85, 85, 85, 85,
        48, 46, 44, 42, 42, 44, 46, 48,
        26, 24, 22, 20, 20, 22, 24, 26,
        12, 10, 10, 10, 10, 10, 10, 12,
        2,  2,  2,  2,  2,  2,  2,  2,
        -6, -6, -6, -6, -6, -6, -6, -6,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_PST = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  4,  4,  0,-20,-40,
        -30,  6, 16, 20, 20, 16,  6,-30,
        -30,  8, 20, 26, 26, 20,  8,-30,
        -30,  4, 20, 24, 24, 20,  4,-30,
        -30,  8, 14, 18, 18, 14,  8,-30,
        -40,-20,  0,  6,  6,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_PST = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  2,  0,  0,  0,  0,  2,-10,
        -10,  0, 10, 14, 14, 10,  0,-10,
        -10,  8,  8, 16, 16,  8,  8,-10,
        -10,  2, 14, 16, 16, 14,  2,-10,
        -10, 14, 14, 12, 12, 14, 14,-10,
        -10,  8,  0,  0,  0,  0,  8,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_PST = intArrayOf(
        0,  0,  0,  6,  6,  0,  0,  0,
        14, 20, 20, 20, 20, 20, 20, 14,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        0,  0,  4, 10, 10,  4,  0,  0
    )

    private val QUEEN_PST = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  6,  8,  8,  6,  0,-10,
        -5,  0,  8,  8,  8,  8,  0, -5,
        0,  0,  8,  8,  8,  8,  0, -5,
        -10,  6,  6,  8,  8,  6,  0,-10,
        -10,  0,  6,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_MID_PST = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        24, 34, 14,  0,  0, 14, 34, 24
    )

    private val KING_END_PST = intArrayOf(
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 22, 32, 32, 22,-10,-30,
        -30,-10, 32, 46, 46, 32,-10,-30,
        -30,-10, 32, 46, 46, 32,-10,-30,
        -30,-10, 22, 32, 32, 22,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    )

    private class TranspositionTable {
        val keys = LongArray(TT_SIZE)
        val scores = IntArray(TT_SIZE)
        val depths = ByteArray(TT_SIZE)
        val flags = ByteArray(TT_SIZE)
        val bestMoves = IntArray(TT_SIZE) // packed: ((from.index + 1) shl 6) or to.index

        fun packMove(move: ChessMove): Int = ((move.from.index + 1) shl 6) or move.to.index

        fun lookup(hash: Long): Int {
            val idx = (hash.toInt() and TT_MASK)
            return if (keys[idx] == hash) idx else -1
        }

        fun store(hash: Long, depth: Int, score: Int, flag: Byte, move: ChessMove?) {
            val idx = (hash.toInt() and TT_MASK)
            val existingDepth = depths[idx].toInt()
            if (keys[idx] == 0L || keys[idx] != hash || depth >= existingDepth) {
                keys[idx] = hash
                scores[idx] = score
                depths[idx] = depth.coerceIn(0, 127).toByte()
                flags[idx] = flag
                if (move != null) {
                    bestMoves[idx] = packMove(move)
                }
            }
        }
    }

    private class SearchContext(
        val startTimeMs: Long,
        val timeLimitMs: Long,
        val aggression: Float
    ) {
        var nodes = 0
        var timedOut = false
        val tt = TranspositionTable()
        val killerMoves = Array(32) { arrayOfNulls<ChessMove>(2) }
        val history = Array(2) { Array(64) { IntArray(64) } }

        fun checkTime(): Boolean {
            if ((nodes and 1023) == 0) {
                if (System.currentTimeMillis() - startTimeMs >= timeLimitMs) {
                    timedOut = true
                }
            }
            return timedOut
        }
    }

    fun chooseBotMove(
        state: ChessBoardState,
        bot: BotPersona,
        adaptivePlayerElo: Int? = null
    ): SearchResult {
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            val eval = evaluateWhitePerspective(state, 1.0f)
            return SearchResult(null, eval, 0, nodesSearched = 1)
        }
        if (legalMoves.size == 1) {
            val next = state.copyWithMoveFast(legalMoves[0])
            val eval = evaluateWhitePerspective(next, bot.aggressionWeight)
            return SearchResult(
                bestMove = legalMoves[0],
                evalWhiteCp = eval,
                depthReached = 1,
                nodesSearched = 1,
                pvUci = listOf(legalMoves[0].toUci())
            )
        }

        if (state.fullMoveNumber <= 2) {
            val bookMove = pickBookOpeningMove(state, legalMoves, bot)
            if (bookMove != null) {
                val next = state.copyWithMoveFast(bookMove)
                return SearchResult(
                    bestMove = bookMove,
                    evalWhiteCp = evaluateWhitePerspective(next, bot.aggressionWeight),
                    depthReached = 1,
                    nodesSearched = 1,
                    pvUci = listOf(bookMove.toUci()),
                    sourceName = "كتاب الافتتاحيات + المحرك المدمج"
                )
            }
        }

        val effectiveDepth: Int
        val effectiveNoise: Int
        if (adaptivePlayerElo != null) {
            effectiveDepth = when {
                adaptivePlayerElo < 950 -> 2
                adaptivePlayerElo < 1450 -> 3
                else -> 5
            }
            effectiveNoise = when {
                adaptivePlayerElo < 900 -> 85
                adaptivePlayerElo < 1150 -> 45
                adaptivePlayerElo < 1450 -> 20
                adaptivePlayerElo < 1750 -> 8
                else -> 0
            }
        } else {
            effectiveDepth = max(bot.searchDepth, if (bot.elo >= 1900) 5 else bot.searchDepth)
            effectiveNoise = bot.noiseCp
        }

        val timeLimitMs = when {
            effectiveDepth <= 1 -> 250L
            effectiveDepth == 2 -> 450L
            effectiveDepth == 3 -> 900L
            effectiveDepth == 4 -> 1300L
            else -> 1700L
        }

        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, bot.aggressionWeight)
        var bestMove = legalMoves[0]
        var bestScoreSide = 0
        var reached = 1

        for (depth in 1..effectiveDepth) {
            val scoredMoves = scoreRootMoves(state, legalMoves, depth, ctx)
            if (ctx.timedOut && depth > 1) break

            reached = depth
            if (scoredMoves.isNotEmpty()) {
                if (effectiveNoise > 0 && depth == effectiveDepth) {
                    val topScore = scoredMoves[0].second
                    val candidates = scoredMoves.filter { topScore - it.second <= effectiveNoise }
                    val chosen = if (candidates.size > 1 && topScore < MATE_SCORE - 500) {
                        candidates[Random.nextInt(candidates.size)]
                    } else {
                        scoredMoves[0]
                    }
                    bestMove = chosen.first
                    bestScoreSide = chosen.second
                } else {
                    bestMove = scoredMoves[0].first
                    bestScoreSide = scoredMoves[0].second
                }
            }
        }

        val evalWhiteCp = if (state.turn == PieceColor.WHITE) bestScoreSide else -bestScoreSide
        val pv = extractPvLine(state, bestMove, ctx.tt, maxPlies = 6)
        return SearchResult(
            bestMove = bestMove,
            evalWhiteCp = evalWhiteCp,
            depthReached = reached,
            nodesSearched = ctx.nodes,
            pvUci = pv
        )
    }

    fun chooseCustomEngineMove(
        state: ChessBoardState,
        targetElo: Int,
        personality: EnginePersonality,
        humanStyle: Boolean,
        moveTimeMs: Int = 900
    ): SearchResult {
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            val eval = evaluateWhitePerspective(state, personality.aggressionWeight)
            return SearchResult(null, eval, 0, nodesSearched = 1)
        }
        if (legalMoves.size == 1) {
            val next = state.copyWithMoveFast(legalMoves[0])
            return SearchResult(
                bestMove = legalMoves[0],
                evalWhiteCp = evaluateWhitePerspective(next, personality.aggressionWeight),
                depthReached = 1,
                nodesSearched = 1,
                pvUci = listOf(legalMoves[0].toUci())
            )
        }

        if (humanStyle && state.fullMoveNumber <= 2) {
            val openingPool = when (personality) {
                EnginePersonality.FIERCE_WARRIOR, EnginePersonality.SACRIFICE_KING -> listOf("e2e4", "d2d4", "f2f4")
                EnginePersonality.IRON_FORTRESS, EnginePersonality.PATIENT_SERPENT -> listOf("d2d4", "c2c4", "g1f3")
                else -> listOf("e2e4", "d2d4", "c2c4", "g1f3")
            }
            val candidate = if (state.turn == PieceColor.WHITE) {
                openingPool.mapNotNull { u -> legalMoves.find { it.toUci() == u } }.randomOrNull()
            } else {
                listOf("e7e5", "c7c5", "e7e6", "d7d5", "g8f6").mapNotNull { u -> legalMoves.find { it.toUci() == u } }.randomOrNull()
            }
            if (candidate != null) {
                val next = state.copyWithMoveFast(candidate)
                return SearchResult(
                    bestMove = candidate,
                    evalWhiteCp = evaluateWhitePerspective(next, personality.aggressionWeight),
                    depthReached = 1,
                    nodesSearched = 1,
                    pvUci = listOf(candidate.toUci()),
                    sourceName = "المحرك المدمج (كتاب الافتتاحيات)"
                )
            }
        }

        val depth = when {
            targetElo < 700 -> 1
            targetElo < 1200 -> 2
            targetElo < 1750 -> 3
            targetElo < 2250 -> 4
            else -> 6
        }
        val baseNoise = when {
            targetElo < 600 -> 170
            targetElo < 950 -> 105
            targetElo < 1300 -> 55
            targetElo < 1700 -> 28
            targetElo < 2200 -> 12
            else -> 0
        }
        val noise = if (humanStyle) baseNoise else (baseNoise / 2)
        val timeLimitMs = when {
            depth <= 1 -> min(moveTimeMs.toLong(), 250L)
            depth == 2 -> min(moveTimeMs.toLong(), 450L)
            else -> moveTimeMs.coerceIn(300, 3500).toLong()
        }

        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, personality.aggressionWeight)
        var bestMove = legalMoves[0]
        var bestScoreSide = 0
        var reached = 1

        for (d in 1..depth) {
            val scoredMoves = scoreRootMoves(state, legalMoves, d, ctx)
            if (ctx.timedOut && d > 1) break
            reached = d
            if (scoredMoves.isNotEmpty()) {
                if (noise > 0 && d == depth) {
                    val topScore = scoredMoves[0].second
                    val candidates = scoredMoves.filter { topScore - it.second <= noise }
                    val chosen = if (candidates.size > 1 && topScore < MATE_SCORE - 500) {
                        candidates[Random.nextInt(candidates.size)]
                    } else scoredMoves[0]
                    bestMove = chosen.first
                    bestScoreSide = chosen.second
                } else {
                    bestMove = scoredMoves[0].first
                    bestScoreSide = scoredMoves[0].second
                }
            }
        }
        val evalWhiteCp = if (state.turn == PieceColor.WHITE) bestScoreSide else -bestScoreSide
        val pv = extractPvLine(state, bestMove, ctx.tt, maxPlies = 6)
        return SearchResult(
            bestMove = bestMove,
            evalWhiteCp = evalWhiteCp,
            depthReached = reached,
            nodesSearched = ctx.nodes,
            pvUci = pv
        )
    }

    fun evaluatePositionBestMove(
        state: ChessBoardState,
        depth: Int = 4,
        timeLimitMs: Long = 800L
    ): SearchResult {
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return SearchResult(null, evaluateWhitePerspective(state, 1.0f), 0, nodesSearched = 1)
        }
        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, 1.0f)
        var bestMove = legalMoves[0]
        var bestScoreSide = 0
        var reached = 1

        for (d in 1..depth) {
            val scored = scoreRootMoves(state, legalMoves, d, ctx)
            if (ctx.timedOut && d > 1) break
            if (scored.isNotEmpty()) {
                bestMove = scored[0].first
                bestScoreSide = scored[0].second
                reached = d
            }
        }
        val evalWhite = if (state.turn == PieceColor.WHITE) bestScoreSide else -bestScoreSide
        val pv = extractPvLine(state, bestMove, ctx.tt, maxPlies = 6)
        return SearchResult(
            bestMove = bestMove,
            evalWhiteCp = evalWhite,
            depthReached = reached,
            nodesSearched = ctx.nodes,
            pvUci = pv
        )
    }

    fun evaluateMoveScoreWhiteCp(
        stateBefore: ChessBoardState,
        move: ChessMove,
        depth: Int = 2,
        timeLimitMs: Long = 350L
    ): Int {
        val after = stateBefore.copyWithMoveFast(move)
        val legalAfter = after.generateLegalMoves()
        if (legalAfter.isEmpty()) {
            return if (after.isInCheck(after.turn)) {
                if (after.turn == PieceColor.WHITE) -MATE_SCORE else MATE_SCORE
            } else 0
        }
        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, 1.0f)
        val scoreSideToMove = -negamax(after, depth - 1, -INF, INF, 1, allowNull = true, ctx = ctx)
        return if (stateBefore.turn == PieceColor.WHITE) scoreSideToMove else -scoreSideToMove
    }

    private fun scoreRootMoves(
        state: ChessBoardState,
        legalMoves: List<ChessMove>,
        depth: Int,
        ctx: SearchContext
    ): List<Pair<ChessMove, Int>> {
        val hash = computeZobristHash(state)
        val ttIdx = ctx.tt.lookup(hash)
        val ttPackedMove = if (ttIdx >= 0) ctx.tt.bestMoves[ttIdx] else 0

        val ordered = orderMoves(state, legalMoves, 0, ttPackedMove, ctx)
        val results = ArrayList<Pair<ChessMove, Int>>(ordered.size)
        var alpha = -INF
        val beta = INF
        var bestRootMove: ChessMove? = null

        for ((index, move) in ordered.withIndex()) {
            val next = state.copyWithMoveFast(move)
            val givesCheck = next.isInCheck(next.turn)
            val ext = if (givesCheck) 1 else 0
            val score = if (index == 0) {
                -negamax(next, depth - 1 + ext, -beta, -alpha, 1, allowNull = true, ctx = ctx)
            } else {
                var s = -negamax(next, depth - 1 + ext, -alpha - 1, -alpha, 1, allowNull = true, ctx = ctx)
                if (s > alpha && s < beta && !ctx.timedOut) {
                    s = -negamax(next, depth - 1 + ext, -beta, -alpha, 1, allowNull = true, ctx = ctx)
                }
                s
            }
            if (ctx.timedOut) break
            results.add(move to score)
            if (score > alpha) {
                alpha = score
                bestRootMove = move
            }
        }
        if (!ctx.timedOut && bestRootMove != null) {
            ctx.tt.store(hash, depth, alpha, TT_EXACT, bestRootMove)
        }
        return results.sortedByDescending { it.second }
    }

    private fun negamax(
        state: ChessBoardState,
        depth: Int,
        alphaInit: Int,
        beta: Int,
        ply: Int,
        allowNull: Boolean,
        ctx: SearchContext
    ): Int {
        ctx.nodes++
        if (ctx.checkTime()) return 0

        if (state.halfMoveClock >= 100 || state.hasInsufficientMaterial()) return 0

        val hash = computeZobristHash(state)
        val ttIdx = ctx.tt.lookup(hash)
        var ttPackedMove = 0
        if (ttIdx >= 0) {
            ttPackedMove = ctx.tt.bestMoves[ttIdx]
            if (ctx.tt.depths[ttIdx].toInt() >= depth) {
                val ttScore = ctx.tt.scores[ttIdx]
                when (ctx.tt.flags[ttIdx]) {
                    TT_EXACT -> return ttScore
                    TT_LOWER -> if (ttScore >= beta) return ttScore
                    TT_UPPER -> if (ttScore <= alphaInit) return ttScore
                }
            }
        }

        val inCheck = state.isInCheck(state.turn)
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return if (inCheck) -MATE_SCORE + ply else 0
        }

        if (depth <= 0) {
            return quiescence(state, alphaInit, beta, 0, ctx)
        }

        // Null-Move Pruning (Stockfish-inspired forward pruning when non-pawn material exists)
        if (allowNull && !inCheck && depth >= 3 && hasNonPawnMaterial(state, state.turn)) {
            val nullState = state.copyWithNullMove()
            val reduction = if (depth >= 6) 3 else 2
            val nullScore = -negamax(nullState, depth - 1 - reduction, -beta, -beta + 1, ply + 1, allowNull = false, ctx = ctx)
            if (ctx.timedOut) return 0
            if (nullScore >= beta) return beta
        }

        var alpha = alphaInit
        val origAlpha = alphaInit
        val ordered = orderMoves(state, legalMoves, ply, ttPackedMove, ctx)
        var bestScore = -INF
        var bestMove: ChessMove? = null

        for ((index, move) in ordered.withIndex()) {
            val next = state.copyWithMoveFast(move)
            val givesCheck = next.isInCheck(next.turn)
            val isTactical = move.capturedPiece != null || move.promotion != null || inCheck || givesCheck
            val ext = if (givesCheck && ply < 10) 1 else 0

            val score: Int = if (index == 0) {
                -negamax(next, depth - 1 + ext, -beta, -alpha, ply + 1, allowNull = true, ctx = ctx)
            } else {
                // Late Move Reductions (LMR) for quiet late moves
                val lmrReduction = if (depth >= 3 && index >= 4 && !isTactical) {
                    if (index >= 10 && depth >= 5) 2 else 1
                } else 0

                var s = -negamax(
                    next,
                    max(0, depth - 1 + ext - lmrReduction),
                    -alpha - 1,
                    -alpha,
                    ply + 1,
                    allowNull = true,
                    ctx = ctx
                )
                if (lmrReduction > 0 && s > alpha && !ctx.timedOut) {
                    s = -negamax(next, depth - 1 + ext, -alpha - 1, -alpha, ply + 1, allowNull = true, ctx = ctx)
                }
                if (s > alpha && s < beta && !ctx.timedOut) {
                    s = -negamax(next, depth - 1 + ext, -beta, -alpha, ply + 1, allowNull = true, ctx = ctx)
                }
                s
            }
            if (ctx.timedOut) return 0

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
            }
            if (alpha >= beta) {
                if (move.capturedPiece == null && move.promotion == null) {
                    if (ply < ctx.killerMoves.size) {
                        ctx.killerMoves[ply][1] = ctx.killerMoves[ply][0]
                        ctx.killerMoves[ply][0] = move
                    }
                    val colorIdx = state.turn.ordinal
                    ctx.history[colorIdx][move.from.index][move.to.index] =
                        (ctx.history[colorIdx][move.from.index][move.to.index] + depth * depth).coerceAtMost(50_000)
                }
                break
            }
        }

        val flag = when {
            bestScore <= origAlpha -> TT_UPPER
            bestScore >= beta -> TT_LOWER
            else -> TT_EXACT
        }
        ctx.tt.store(hash, depth, bestScore, flag, bestMove)
        return bestScore
    }

    private fun quiescence(
        state: ChessBoardState,
        alphaInit: Int,
        beta: Int,
        qDepth: Int,
        ctx: SearchContext
    ): Int {
        ctx.nodes++
        if (ctx.checkTime()) return 0

        val standPat = evaluateSideToMove(state, ctx.aggression)
        if (qDepth >= 6) return standPat
        if (standPat >= beta) return beta
        var alpha = max(alphaInit, standPat)

        val tacticalMoves = state.generateLegalMoves().filter {
            it.capturedPiece != null || it.promotion != null
        }
        val ordered = orderMoves(state, tacticalMoves, 31, 0, ctx)

        for (move in ordered) {
            val next = state.copyWithMoveFast(move)
            val score = -quiescence(next, -beta, -alpha, qDepth + 1, ctx)
            if (ctx.timedOut) return 0
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderMoves(
        state: ChessBoardState,
        moves: List<ChessMove>,
        ply: Int,
        ttPackedMove: Int,
        ctx: SearchContext
    ): List<ChessMove> {
        if (moves.size <= 1) return moves
        val k0 = if (ply < ctx.killerMoves.size) ctx.killerMoves[ply][0] else null
        val k1 = if (ply < ctx.killerMoves.size) ctx.killerMoves[ply][1] else null
        val colorIdx = state.turn.ordinal

        return moves.sortedByDescending { move ->
            val packed = ctx.tt.packMove(move)
            var score = 0
            if (ttPackedMove != 0 && packed == ttPackedMove) {
                score += 100_000
            }
            if (move.capturedPiece != null) {
                val attackerVal = state.pieceAt(move.from)?.type?.valueCp ?: 100
                score += 20_000 + move.capturedPiece.type.valueCp * 10 - attackerVal
            }
            if (move.promotion != null) {
                score += 18_000 + move.promotion.valueCp
            }
            if (move.isCastleKingside || move.isCastleQueenside) {
                score += 650
            }
            if (move == k0) score += 6_000
            else if (move == k1) score += 4_500
            else score += ctx.history[colorIdx][move.from.index][move.to.index]

            if (move.to.row in 3..4 && move.to.col in 3..4) score += 30
            score
        }
    }

    private fun extractPvLine(
        startState: ChessBoardState,
        rootBestMove: ChessMove,
        tt: TranspositionTable,
        maxPlies: Int
    ): List<String> {
        val pv = mutableListOf(rootBestMove.toUci())
        var curr = startState.copyWithMoveFast(rootBestMove)
        val seenHashes = hashSetOf(computeZobristHash(startState))
        for (i in 1 until maxPlies) {
            val h = computeZobristHash(curr)
            if (!seenHashes.add(h)) break
            val idx = tt.lookup(h)
            if (idx < 0) break
            val packed = tt.bestMoves[idx]
            if (packed == 0) break
            val fromIdx = (packed ushr 6) - 1
            val toIdx = packed and 0x3F
            val legal = curr.generateLegalMoves()
            val matched = legal.find { it.from.index == fromIdx && it.to.index == toIdx } ?: break
            pv.add(matched.toUci())
            curr = curr.copyWithMoveFast(matched)
        }
        return pv
    }

    private fun hasNonPawnMaterial(state: ChessBoardState, color: PieceColor): Boolean {
        for (i in 0..63) {
            val p = state.board[i] ?: continue
            if (p.color == color && p.type != PieceType.PAWN && p.type != PieceType.KING) {
                return true
            }
        }
        return false
    }

    fun computeZobristHash(state: ChessBoardState): Long {
        var h = 0L
        for (sq in 0..63) {
            val p = state.board[sq] ?: continue
            val pieceIndex = p.type.ordinal + if (p.color == PieceColor.BLACK) 6 else 0
            h = h xor ZOBRIST_PIECE[pieceIndex][sq]
        }
        var castlingMask = 0
        if (state.castling.whiteKingside) castlingMask = castlingMask or 1
        if (state.castling.whiteQueenside) castlingMask = castlingMask or 2
        if (state.castling.blackKingside) castlingMask = castlingMask or 4
        if (state.castling.blackQueenside) castlingMask = castlingMask or 8
        h = h xor ZOBRIST_CASTLING[castlingMask]

        state.enPassantTarget?.let { ep ->
            h = h xor ZOBRIST_EP_FILE[ep.col]
        }
        if (state.turn == PieceColor.BLACK) {
            h = h xor ZOBRIST_BLACK_TURN
        }
        return h
    }

    fun evaluateWhitePerspective(state: ChessBoardState, aggression: Float = 1.0f): Int {
        val raw = evaluateSideToMove(state, aggression)
        return if (state.turn == PieceColor.WHITE) raw else -raw
    }

    private fun evaluateSideToMove(state: ChessBoardState, aggression: Float): Int {
        var mgWhite = 0
        var egWhite = 0
        var mgBlack = 0
        var egBlack = 0
        var whiteBishops = 0
        var blackBishops = 0
        var phase = 0 // 0 = endgame, 24 = full opening/middlegame

        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)
        val whitePawnRanks = IntArray(8) { 7 } // closest-to-promotion rank per file (smaller = further up)
        val blackPawnRanks = IntArray(8) { 0 }

        for (idx in 0..63) {
            val p = state.board[idx] ?: continue
            val r = idx / 8
            val c = idx % 8
            val pstIndex = if (p.color == PieceColor.WHITE) idx else (7 - r) * 8 + c

            val baseMg: Int
            val baseEg: Int
            when (p.type) {
                PieceType.PAWN -> {
                    baseMg = 95 + PAWN_MG_PST[pstIndex]
                    baseEg = 115 + PAWN_EG_PST[pstIndex]
                    if (p.color == PieceColor.WHITE) {
                        whitePawnFiles[c]++
                        if (r < whitePawnRanks[c]) whitePawnRanks[c] = r
                    } else {
                        blackPawnFiles[c]++
                        if (r > blackPawnRanks[c]) blackPawnRanks[c] = r
                    }
                }
                PieceType.KNIGHT -> {
                    phase += 1
                    baseMg = 325 + KNIGHT_PST[pstIndex]
                    baseEg = 315 + KNIGHT_PST[pstIndex]
                }
                PieceType.BISHOP -> {
                    phase += 1
                    if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                    baseMg = 335 + BISHOP_PST[pstIndex]
                    baseEg = 345 + BISHOP_PST[pstIndex]
                }
                PieceType.ROOK -> {
                    phase += 2
                    baseMg = 500 + ROOK_PST[pstIndex]
                    baseEg = 525 + ROOK_PST[pstIndex]
                }
                PieceType.QUEEN -> {
                    phase += 4
                    baseMg = 915 + QUEEN_PST[pstIndex]
                    baseEg = 945 + QUEEN_PST[pstIndex]
                }
                PieceType.KING -> {
                    baseMg = 20000 + KING_MID_PST[pstIndex]
                    baseEg = 20000 + KING_END_PST[pstIndex]
                }
            }

            val centerBonus = if (p.type != PieceType.KING && p.type != PieceType.PAWN && r in 2..5 && c in 2..5) {
                (7 * aggression).toInt()
            } else 0

            if (p.color == PieceColor.WHITE) {
                mgWhite += baseMg + centerBonus
                egWhite += baseEg + centerBonus
            } else {
                mgBlack += baseMg + centerBonus
                egBlack += baseEg + centerBonus
            }
        }

        // Bishop pair synergy
        if (whiteBishops >= 2) { mgWhite += 35; egWhite += 45 }
        if (blackBishops >= 2) { mgBlack += 35; egBlack += 45 }

        // Pawn structure, passed pawns & rook open/semi-open file evaluation
        for (c in 0..7) {
            if (whitePawnFiles[c] > 1) {
                val pen = (whitePawnFiles[c] - 1) * 14
                mgWhite -= pen; egWhite -= pen
            }
            if (blackPawnFiles[c] > 1) {
                val pen = (blackPawnFiles[c] - 1) * 14
                mgBlack -= pen; egBlack -= pen
            }

            val wIso = whitePawnFiles[c] > 0 &&
                (c == 0 || whitePawnFiles[c - 1] == 0) &&
                (c == 7 || whitePawnFiles[c + 1] == 0)
            if (wIso) { mgWhite -= 12; egWhite -= 16 }

            val bIso = blackPawnFiles[c] > 0 &&
                (c == 0 || blackPawnFiles[c - 1] == 0) &&
                (c == 7 || blackPawnFiles[c + 1] == 0)
            if (bIso) { mgBlack -= 12; egBlack -= 16 }

            // Passed pawn detection
            if (whitePawnFiles[c] > 0) {
                val wRank = whitePawnRanks[c] // 1..6 (closer to 0 = closer to promotion)
                val blockedByBlack = ((c - 1).coerceAtLeast(0)..(c + 1).coerceAtMost(7)).any { f ->
                    blackPawnFiles[f] > 0 && blackPawnRanks[f] < wRank
                }
                if (!blockedByBlack) {
                    val adv = 7 - wRank
                    mgWhite += adv * 8
                    egWhite += adv * 18
                }
            }
            if (blackPawnFiles[c] > 0) {
                val bRank = blackPawnRanks[c] // 1..6 (closer to 7 = closer to promotion)
                val blockedByWhite = ((c - 1).coerceAtLeast(0)..(c + 1).coerceAtMost(7)).any { f ->
                    whitePawnFiles[f] > 0 && whitePawnRanks[f] > bRank
                }
                if (!blockedByWhite) {
                    val adv = bRank
                    mgBlack += adv * 8
                    egBlack += adv * 18
                }
            }
        }

        // Rook open / semi-open files bonus
        for (idx in 0..63) {
            val p = state.board[idx] ?: continue
            if (p.type != PieceType.ROOK) continue
            val c = idx % 8
            if (p.color == PieceColor.WHITE) {
                if (whitePawnFiles[c] == 0) {
                    val bonus = if (blackPawnFiles[c] == 0) 24 else 12
                    mgWhite += bonus; egWhite += bonus
                }
            } else {
                if (blackPawnFiles[c] == 0) {
                    val bonus = if (whitePawnFiles[c] == 0) 24 else 12
                    mgBlack += bonus; egBlack += bonus
                }
            }
        }

        // Aggression check tension
        if (aggression > 1.05f && state.isInCheck(state.turn)) {
            val penalty = (18 * aggression).toInt()
            if (state.turn == PieceColor.WHITE) mgWhite -= penalty else mgBlack -= penalty
        }

        val clampedPhase = phase.coerceIn(0, 24)
        val mgScore = mgWhite - mgBlack
        val egScore = egWhite - egBlack
        val whiteAdvantage = (mgScore * clampedPhase + egScore * (24 - clampedPhase)) / 24
        return if (state.turn == PieceColor.WHITE) whiteAdvantage else -whiteAdvantage
    }

    private fun pickBookOpeningMove(
        state: ChessBoardState,
        legalMoves: List<ChessMove>,
        bot: BotPersona
    ): ChessMove? {
        if (state.fullMoveNumber == 1 && state.turn == PieceColor.WHITE) {
            val openings = when (bot) {
                BotPersona.TAL, BotPersona.POLGAR, BotPersona.ALEKHINE -> listOf("e2e4", "d2d4")
                BotPersona.CAPABLANCA, BotPersona.CARLSEN -> listOf("d2d4", "e2e4", "c2c4")
                else -> listOf("e2e4", "d2d4", "g1f3")
            }
            val uci = openings[Random.nextInt(openings.size)]
            return legalMoves.find { it.toUci() == uci }
        }
        if (state.fullMoveNumber == 1 && state.turn == PieceColor.BLACK) {
            val e4Pawn = state.pieceAt(4, 4)
            val d4Pawn = state.pieceAt(4, 3)
            val candidates = when {
                e4Pawn?.color == PieceColor.WHITE -> when (bot) {
                    BotPersona.TAL, BotPersona.KASPAROV, BotPersona.POLGAR -> listOf("c7c5", "e7e5")
                    BotPersona.ALEKHINE -> listOf("g8f6", "c7c5")
                    BotPersona.CARLSEN, BotPersona.ANAND -> listOf("c7c5", "e7e5", "e7e6", "c7c6")
                    BotPersona.FISCHER, BotPersona.CAPABLANCA -> listOf("e7e5", "c7c5")
                }
                d4Pawn?.color == PieceColor.WHITE -> listOf("d7d5", "g8f6")
                else -> listOf("e7e5", "d7d5", "g8f6")
            }
            val uci = candidates[Random.nextInt(candidates.size)]
            return legalMoves.find { it.toUci() == uci }
        }
        return null
    }
}
