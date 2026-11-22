package com.example.chess

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object ChessEngine {

    private const val INF = 1_000_000
    private const val MATE_SCORE = 100_000

    data class SearchResult(
        val bestMove: ChessMove?,
        val evalWhiteCp: Int,
        val depthReached: Int
    )

    // Piece-Square Tables (White's perspective, index 0 = a8 .. 63 = h1)
    private val PAWN_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        12, 12, 22, 32, 32, 22, 12, 12,
        6,  6, 14, 28, 28, 14,  6,  6,
        2,  2,  8, 24, 24,  8,  2,  2,
        4, -4, -8,  0,  0, -8, -4,  4,
        6, 10, 10,-22,-22, 10, 10,  6,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_PST = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  2,  2,  0,-20,-40,
        -30,  4, 14, 18, 18, 14,  4,-30,
        -30,  6, 18, 24, 24, 18,  6,-30,
        -30,  2, 18, 22, 22, 18,  2,-30,
        -30,  6, 12, 16, 16, 12,  6,-30,
        -40,-20,  0,  6,  6,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_PST = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  8, 12, 12,  8,  0,-10,
        -10,  6,  6, 14, 14,  6,  6,-10,
        -10,  0, 12, 14, 14, 12,  0,-10,
        -10, 12, 12, 10, 10, 12, 12,-10,
        -10,  6,  0,  0,  0,  0,  6,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_PST = intArrayOf(
        0,  0,  0,  4,  4,  0,  0,  0,
        10, 16, 16, 16, 16, 16, 16, 10,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        -6,  0,  0,  0,  0,  0,  0, -6,
        0,  0,  2,  8,  8,  2,  0,  0
    )

    private val QUEEN_PST = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  6,  6,  6,  6,  0,-10,
        -5,  0,  6,  6,  6,  6,  0, -5,
        0,  0,  6,  6,  6,  6,  0, -5,
        -10,  6,  6,  6,  6,  6,  0,-10,
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
        18, 18,  0,  0,  0,  0, 18, 18,
        22, 32, 12,  0,  0, 12, 32, 22
    )

    private val KING_END_PST = intArrayOf(
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 42, 42, 30,-10,-30,
        -30,-10, 30, 42, 42, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    )

    private class SearchContext(
        val startTimeMs: Long,
        val timeLimitMs: Long,
        val aggression: Float
    ) {
        var nodes = 0
        var timedOut = false
        val killerMoves = Array(16) { arrayOfNulls<ChessMove>(2) }

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
            return SearchResult(null, eval, 0)
        }
        if (legalMoves.size == 1) {
            val next = state.copyWithMoveFast(legalMoves[0])
            val eval = evaluateWhitePerspective(next, bot.aggressionWeight)
            return SearchResult(legalMoves[0], eval, 1)
        }

        // Opening variety for move 1-2 so matches feel fresh
        if (state.fullMoveNumber <= 2) {
            val bookMove = pickBookOpeningMove(state, legalMoves, bot)
            if (bookMove != null) {
                val next = state.copyWithMoveFast(bookMove)
                return SearchResult(bookMove, evaluateWhitePerspective(next, bot.aggressionWeight), 1)
            }
        }

        // Compute effective depth & noise (if Adaptive Mode is active, scale with player's ELO)
        val effectiveDepth: Int
        val effectiveNoise: Int
        if (adaptivePlayerElo != null) {
            effectiveDepth = when {
                adaptivePlayerElo < 950 -> 2
                adaptivePlayerElo < 1450 -> 3
                else -> 4
            }
            effectiveNoise = when {
                adaptivePlayerElo < 900 -> 85
                adaptivePlayerElo < 1150 -> 45
                adaptivePlayerElo < 1450 -> 20
                adaptivePlayerElo < 1750 -> 8
                else -> 0
            }
        } else {
            effectiveDepth = bot.searchDepth
            effectiveNoise = bot.noiseCp
        }

        val timeLimitMs = when (effectiveDepth) {
            1 -> 250L
            2 -> 450L
            3 -> 900L
            else -> 1350L
        }

        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, bot.aggressionWeight)
        var bestMove = legalMoves[0]
        var bestScoreSide = 0
        var reached = 1

        // Iterative deepening with aspiration/ordering
        for (depth in 1..effectiveDepth) {
            val scoredMoves = scoreRootMoves(state, legalMoves, depth, ctx)
            if (ctx.timedOut && depth > 1) break

            reached = depth
            if (scoredMoves.isNotEmpty()) {
                if (effectiveNoise > 0 && depth == effectiveDepth) {
                    // Add controlled human-like noise for lower-tier bots without blundering mate
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
        return SearchResult(bestMove, evalWhiteCp, reached)
    }

    fun evaluatePositionBestMove(
        state: ChessBoardState,
        depth: Int = 3,
        timeLimitMs: Long = 650L
    ): SearchResult {
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return SearchResult(null, evaluateWhitePerspective(state, 1.0f), 0)
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
        return SearchResult(bestMove, evalWhite, reached)
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
        val scoreSideToMove = -negamax(after, depth - 1, -INF, INF, 1, ctx)
        // scoreSideToMove is from stateBefore.turn's perspective
        return if (stateBefore.turn == PieceColor.WHITE) scoreSideToMove else -scoreSideToMove
    }

    private fun scoreRootMoves(
        state: ChessBoardState,
        legalMoves: List<ChessMove>,
        depth: Int,
        ctx: SearchContext
    ): List<Pair<ChessMove, Int>> {
        val ordered = orderMoves(state, legalMoves, 0, ctx)
        val results = ArrayList<Pair<ChessMove, Int>>(ordered.size)
        var alpha = -INF
        val beta = INF

        for (move in ordered) {
            val next = state.copyWithMoveFast(move)
            val score = -negamax(next, depth - 1, -beta, -alpha, 1, ctx)
            if (ctx.timedOut) break
            results.add(move to score)
            if (score > alpha) alpha = score
        }
        return results.sortedByDescending { it.second }
    }

    private fun negamax(
        state: ChessBoardState,
        depth: Int,
        alphaInit: Int,
        beta: Int,
        ply: Int,
        ctx: SearchContext
    ): Int {
        ctx.nodes++
        if (ctx.checkTime()) return 0

        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return if (state.isInCheck(state.turn)) {
                -MATE_SCORE + ply
            } else {
                0
            }
        }
        if (state.halfMoveClock >= 100 || state.hasInsufficientMaterial()) return 0

        if (depth <= 0) {
            return quiescence(state, alphaInit, beta, 0, ctx)
        }

        var alpha = alphaInit
        val ordered = orderMoves(state, legalMoves, ply, ctx)
        var best = -INF

        for (move in ordered) {
            val next = state.copyWithMoveFast(move)
            val score = -negamax(next, depth - 1, -beta, -alpha, ply + 1, ctx)
            if (ctx.timedOut) return 0

            if (score > best) best = score
            if (score > alpha) {
                alpha = score
            }
            if (alpha >= beta) {
                if (move.capturedPiece == null && ply < ctx.killerMoves.size) {
                    ctx.killerMoves[ply][1] = ctx.killerMoves[ply][0]
                    ctx.killerMoves[ply][0] = move
                }
                break
            }
        }
        return best
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
        if (qDepth >= 4) return standPat
        if (standPat >= beta) return beta
        var alpha = max(alphaInit, standPat)

        val tacticalMoves = state.generateLegalMoves().filter {
            it.capturedPiece != null || it.promotion != null
        }
        val ordered = orderMoves(state, tacticalMoves, 15, ctx)

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
        ctx: SearchContext
    ): List<ChessMove> {
        if (moves.size <= 1) return moves
        val k0 = if (ply < ctx.killerMoves.size) ctx.killerMoves[ply][0] else null
        val k1 = if (ply < ctx.killerMoves.size) ctx.killerMoves[ply][1] else null

        return moves.sortedByDescending { move ->
            var score = 0
            if (move.capturedPiece != null) {
                val attackerVal = state.pieceAt(move.from)?.type?.valueCp ?: 100
                score += 10_000 + move.capturedPiece.type.valueCp * 10 - attackerVal
            }
            if (move.promotion != null) {
                score += 9_000 + move.promotion.valueCp
            }
            if (move.isCastleKingside || move.isCastleQueenside) {
                score += 450
            }
            if (move == k0) score += 3_500
            else if (move == k1) score += 2_500
            // Slight center preference
            if (move.to.row in 3..4 && move.to.col in 3..4) score += 35
            score
        }
    }

    fun evaluateWhitePerspective(state: ChessBoardState, aggression: Float = 1.0f): Int {
        val raw = evaluateSideToMove(state, aggression)
        return if (state.turn == PieceColor.WHITE) raw else -raw
    }

    private fun evaluateSideToMove(state: ChessBoardState, aggression: Float): Int {
        var whiteScore = 0
        var blackScore = 0
        var whiteBishops = 0
        var blackBishops = 0
        var nonPawnMat = 0

        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)

        for (i in 0..63) {
            val p = state.board[i] ?: continue
            if (p.type != PieceType.KING && p.type != PieceType.PAWN) {
                nonPawnMat += p.type.valueCp
            }
        }
        val isEndgame = nonPawnMat <= 2600

        for (idx in 0..63) {
            val p = state.board[idx] ?: continue
            val r = idx / 8
            val c = idx % 8
            val pstIndex = if (p.color == PieceColor.WHITE) idx else (7 - r) * 8 + c

            val base = p.type.valueCp
            val pst = when (p.type) {
                PieceType.PAWN -> {
                    if (p.color == PieceColor.WHITE) whitePawnFiles[c]++ else blackPawnFiles[c]++
                    PAWN_PST[pstIndex]
                }
                PieceType.KNIGHT -> KNIGHT_PST[pstIndex]
                PieceType.BISHOP -> {
                    if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                    BISHOP_PST[pstIndex]
                }
                PieceType.ROOK -> ROOK_PST[pstIndex]
                PieceType.QUEEN -> QUEEN_PST[pstIndex]
                PieceType.KING -> if (isEndgame) KING_END_PST[pstIndex] else KING_MID_PST[pstIndex]
            }

            val bonus = if (p.type != PieceType.KING && p.type != PieceType.PAWN && r in 2..5 && c in 2..5) {
                (6 * aggression).toInt()
            } else 0

            val total = base + pst + bonus
            if (p.color == PieceColor.WHITE) whiteScore += total else blackScore += total
        }

        // Bishop pair bonus
        if (whiteBishops >= 2) whiteScore += 35
        if (blackBishops >= 2) blackScore += 35

        // Doubled & isolated pawn penalties
        for (c in 0..7) {
            if (whitePawnFiles[c] > 1) whiteScore -= (whitePawnFiles[c] - 1) * 14
            if (blackPawnFiles[c] > 1) blackScore -= (blackPawnFiles[c] - 1) * 14

            val wIso = whitePawnFiles[c] > 0 &&
                (c == 0 || whitePawnFiles[c - 1] == 0) &&
                (c == 7 || whitePawnFiles[c + 1] == 0)
            if (wIso) whiteScore -= 12

            val bIso = blackPawnFiles[c] > 0 &&
                (c == 0 || blackPawnFiles[c - 1] == 0) &&
                (c == 7 || blackPawnFiles[c + 1] == 0)
            if (bIso) blackScore -= 12
        }

        // Aggression bonus for checks / king proximity
        if (aggression > 1.05f && state.isInCheck(state.turn)) {
            val penalty = (18 * aggression).toInt()
            if (state.turn == PieceColor.WHITE) whiteScore -= penalty else blackScore -= penalty
        }

        val whiteAdvantage = whiteScore - blackScore
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
