package com.example.chess

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
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

    data class CandidateMoveEval(
        val move: ChessMove,
        val scoreSideCp: Int,
        val evalWhiteCp: Int,
        val mateIn: Int? = null,
        val pvUci: List<String> = emptyList(),
        val depth: Int = 1,
        val nodes: Int = 0
    )

    /** Ruqaa Section 6.2 Strength Translation Formulas **/
    fun computeNodeCap(elo: Int): Int? {
        if (elo <= 0) return null // "بلا حد"
        if (elo < 1320) return 7 * elo + 3000
        return min(240_000, (18000.0 * 2.0.pow((elo - 1320) / 450.0)).toInt())
    }

    fun computeBaseMoveTimeMs(elo: Int): Int {
        if (elo <= 0) return 1100
        return min(1100, elo / 3 + 250)
    }

    fun computeMoveTimeWithClockMs(elo: Int, remainingMs: Long?): Int {
        val base = computeBaseMoveTimeMs(elo)
        if (remainingMs == null || remainingMs <= 0L) return base
        return max(80, min(base, (remainingMs / 12L).toInt()))
    }

    fun computeStockfishProbability(elo: Int): Double {
        if (elo <= 0 || elo >= 1320) return 1.0
        return (((elo - 200).coerceAtLeast(0)) / 1120.0).pow(1.8)
    }

    fun computePersonalityLossCapCp(
        tier: StrengthTier,
        risk: PersonalityRisk,
        intensity: Int
    ): Int {
        val normIntensity = intensity.coerceIn(0, 100) / 100.0
        return (tier.personalityBaseCapCp * risk.multiplier * (0.35 + 0.65 * normIntensity)).roundToInt()
    }

    // Deterministic 64-bit Zobrist tables
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
        val bestMoves = IntArray(TT_SIZE)

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
        val aggression: Float,
        val maxNodes: Int? = null
    ) {
        var nodes = 0
        var timedOut = false
        val tt = TranspositionTable()
        val killerMoves = Array(32) { arrayOfNulls<ChessMove>(2) }
        val history = Array(2) { Array(64) { IntArray(64) } }

        fun checkTime(): Boolean {
            if (maxNodes != null && nodes >= maxNodes) {
                timedOut = true
                return true
            }
            if ((nodes and 511) == 0) {
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
                        candidates[ThreadLocalRandom.current().nextInt(candidates.size)]
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

    /**
     * Full Ruqaa Engine Selection (Sections 6 & 7):
     * - Plays forced single legal move immediately
     * - Under 1320 Elo: probabilistic switch between heuristic player & Stockfish/search
     * - ShortMateProof for OPPORTUNITY_HUNTER & mate delay for LONG_GAME_MAKER
     * - Personality layer (0..100 intensity, Conservative/Balanced/Aggressive risk)
     * - Human Style softmax layer for limited Elo levels
     */
    fun chooseCustomEngineMove(
        state: ChessBoardState,
        targetElo: Int,
        personality: EnginePersonality,
        humanStyle: Boolean,
        moveTimeMs: Int = computeBaseMoveTimeMs(targetElo),
        personalityIntensity: Int = 50,
        personalityRisk: PersonalityRisk = PersonalityRisk.BALANCED,
        gameSeed: Long = 0x52757161L,
        lastMoveFromWing: Int? = null,
        lastMovedPieceType: PieceType? = null
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
                pvUci = listOf(legalMoves[0].toUci()),
                sourceName = "نقلة وحيدة قانونية"
            )
        }

        // Opportunity Hunter short mate proof (mate <= 3, max 150ms, max 65k nodes)
        if (personality == EnginePersonality.OPPORTUNITY_HUNTER && personalityIntensity > 0) {
            findShortMateMove(state, maxMoves = 3, maxTimeMs = 150L, maxNodes = 65_000)?.let { mateMove ->
                val next = state.copyWithMoveFast(mateMove)
                return SearchResult(
                    bestMove = mateMove,
                    evalWhiteCp = if (state.turn == PieceColor.WHITE) MATE_SCORE - 2 else -(MATE_SCORE - 2),
                    depthReached = 5,
                    nodesSearched = 1500,
                    pvUci = listOf(mateMove.toUci()),
                    sourceName = "صائد الفرص (كشف مات قصير)"
                )
            }
        }

        // Sub-1320 heuristic probability check (Ruqaa Section 6.2 & 6.5)
        if (targetElo in 1 until 1320) {
            val pStockfish = computeStockfishProbability(targetElo)
            if (ThreadLocalRandom.current().nextDouble() > pStockfish) {
                return chooseHeuristicMove(
                    state = state,
                    elo = targetElo,
                    personality = personality,
                    intensity = personalityIntensity
                )
            }
        }

        val depth = when {
            targetElo <= 0 -> 5
            targetElo < 700 -> 2
            targetElo < 1320 -> 3
            targetElo < 2000 -> 4
            else -> 5
        }
        val nodeCap = computeNodeCap(targetElo)
        val timeLimitMs = min(moveTimeMs.toLong(), computeBaseMoveTimeMs(targetElo).toLong()).coerceAtLeast(80L)
        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, personality.aggressionWeight, maxNodes = nodeCap)

        var lastScored: List<Pair<ChessMove, Int>> = emptyList()
        var reached = 1
        for (d in 1..depth) {
            val scoredMoves = scoreRootMoves(state, legalMoves, d, ctx)
            if (ctx.timedOut && d > 1) break
            if (scoredMoves.isNotEmpty()) {
                lastScored = scoredMoves
                reached = d
            }
        }

        if (lastScored.isEmpty()) {
            val fallback = legalMoves[0]
            return SearchResult(
                bestMove = fallback,
                evalWhiteCp = evaluateWhitePerspective(state.copyWithMoveFast(fallback), 1.0f),
                depthReached = 1,
                nodesSearched = ctx.nodes,
                pvUci = listOf(fallback.toUci())
            )
        }

        val candidates = lastScored.take(6).map { (mv, sc) ->
            CandidateMoveEval(
                move = mv,
                scoreSideCp = sc,
                evalWhiteCp = if (state.turn == PieceColor.WHITE) sc else -sc,
                pvUci = extractPvLine(state, mv, ctx.tt, maxPlies = 5),
                depth = reached,
                nodes = ctx.nodes
            )
        }

        return selectFromCandidates(
            state = state,
            candidates = candidates,
            targetElo = targetElo,
            personality = personality,
            humanStyle = humanStyle,
            personalityIntensity = personalityIntensity,
            personalityRisk = personalityRisk,
            gameSeed = gameSeed,
            lastMoveFromWing = lastMoveFromWing,
            lastMovedPieceType = lastMovedPieceType,
            sourceLabel = "المحرك المدمج (TT + PVS + LMR)"
        )
    }

    /**
     * Ruqaa Section 6.5: Heuristic player under 1320 Elo.
     */
    fun chooseHeuristicMove(
        state: ChessBoardState,
        elo: Int,
        personality: EnginePersonality = EnginePersonality.CLASSIC,
        intensity: Int = 50
    ): SearchResult {
        val legal = state.generateLegalMoves()
        if (legal.isEmpty()) {
            return SearchResult(null, evaluateWhitePerspective(state, 1.0f), 0, 1)
        }
        if (legal.size == 1) {
            val mv = legal[0]
            return SearchResult(mv, evaluateWhitePerspective(state.copyWithMoveFast(mv), 1.0f), 1, 1, listOf(mv.toUci()))
        }

        val s = ((elo.coerceIn(200, 1320) - 200.0) / 1120.0).coerceIn(0.0, 1.0)
        val backRow = if (state.turn == PieceColor.WHITE) 7 else 0
        val forwardDir = if (state.turn == PieceColor.WHITE) -1 else 1
        val normI = intensity.coerceIn(0, 100) / 100.0

        val scored = legal.map { move ->
            val piece = state.pieceAt(move.from)
            val pieceVal = piece?.type?.valueCp ?: 100
            val after = state.copyWithMoveFast(move)
            var score = 0.0

            // Capture (2/3 value, max 180)
            move.capturedPiece?.let { cap ->
                score += (cap.type.valueCp * 2.0 / 3.0).coerceAtMost(180.0)
            }
            // Promotion
            move.promotion?.let { promo ->
                score += promo.valueCp / 3.0
            }
            // Center c3..f6 (+15)
            if (move.to.row in 2..5 && move.to.col in 2..5) {
                score += 15.0
            }
            // Pawn advance (+5 per rank)
            if (piece?.type == PieceType.PAWN) {
                val rankSteps = (move.to.row - move.from.row) * forwardDir
                if (rankSteps > 0) score += 5.0 * rankSteps
            }
            // Minor piece development off back rank (+18)
            if ((piece?.type == PieceType.KNIGHT || piece?.type == PieceType.BISHOP) &&
                move.from.row == backRow && move.to.row != backRow
            ) {
                score += 18.0
            }
            // Check (+20)
            val givesCheck = after.isInCheck(after.turn)
            if (givesCheck) {
                score += 20.0
            }
            // Hanging penalty after move
            if (after.isSquareAttacked(move.to, after.turn)) {
                val defended = after.isSquareAttacked(move.to, state.turn)
                val baseHang = pieceVal / (if (defended) 7.0 else 3.0)
                score -= baseHang * (0.8 * s + 0.2)
            }
            // Personality feature clamped [-16, +16] * I
            if (personality != EnginePersonality.CLASSIC && intensity > 0) {
                val rawFeature = computePersonalityFeatureScore(
                    stateBefore = state,
                    afterState = after,
                    move = move,
                    givesCheck = givesCheck,
                    personality = personality,
                    evalSideBeforeCp = 0,
                    gameSeed = 0L,
                    lastMoveFromWing = null,
                    lastMovedPieceType = null
                )
                score += rawFeature.coerceIn(-16.0, 16.0) * normI
            }
            move to score
        }.sortedByDescending { it.second }

        val bestScore = scored[0].second
        val cutoff = 290.0 - 180.0 * s
        val eligible = scored.filter { bestScore - it.second <= cutoff }
        val temp = (110.0 - 65.0 * s).coerceAtLeast(10.0)
        val chosen = softmaxSample(eligible, temp) { it.second }

        val next = state.copyWithMoveFast(chosen.first)
        return SearchResult(
            bestMove = chosen.first,
            evalWhiteCp = evaluateWhitePerspective(next, 1.0f),
            depthReached = 1,
            nodesSearched = legal.size,
            pvUci = listOf(chosen.first.toUci()),
            sourceName = "اللاعب الإرشادي (تحت ١٣٢٠ إيلو)"
        )
    }

    /**
     * Applies Ruqaa Section 6.4 Human Style + Section 7 Personality selection on any MultiPV list
     * (whether produced by native Stockfish UCI or built-in search).
     */
    fun selectFromCandidates(
        state: ChessBoardState,
        candidates: List<CandidateMoveEval>,
        targetElo: Int,
        personality: EnginePersonality,
        humanStyle: Boolean,
        personalityIntensity: Int,
        personalityRisk: PersonalityRisk,
        gameSeed: Long = 0x52757161L,
        lastMoveFromWing: Int? = null,
        lastMovedPieceType: PieceType? = null,
        sourceLabel: String = "Stockfish UCI"
    ): SearchResult {
        if (candidates.isEmpty()) {
            val legal = state.generateLegalMoves()
            val mv = legal.firstOrNull()
            return SearchResult(mv, evaluateWhitePerspective(state, 1.0f), 1, 1, sourceName = sourceLabel)
        }
        val sorted = candidates.sortedByDescending { it.scoreSideCp }
        val top = sorted[0]
        val tier = StrengthTier.fromElo(targetElo)
        val isUnlimited = targetElo <= 0

        // Personality layer if enabled and intensity > 0
        if (personality != EnginePersonality.CLASSIC && personalityIntensity > 0) {
            // Immediate mate check (except LONG_GAME_MAKER delaying forced mate when intensity >= 25)
            val immediateMate = sorted.find { it.mateIn != null && it.mateIn == 1 }
            if (immediateMate != null && !(personality == EnginePersonality.LONG_GAME_MAKER && personalityIntensity >= 25)) {
                return SearchResult(
                    bestMove = immediateMate.move,
                    evalWhiteCp = immediateMate.evalWhiteCp,
                    depthReached = immediateMate.depth,
                    nodesSearched = immediateMate.nodes,
                    pvUci = immediateMate.pvUci,
                    sourceName = "$sourceLabel • ${personality.nameAr}"
                )
            }

            val normI = personalityIntensity.coerceIn(0, 100) / 100.0
            val lossCap = computePersonalityLossCapCp(tier, personalityRisk, personalityIntensity)

            val filtered = sorted.filter { cand ->
                val loss = (top.scoreSideCp - cand.scoreSideCp).coerceAtLeast(0)
                if (personality == EnginePersonality.LONG_GAME_MAKER &&
                    personalityIntensity >= 25 &&
                    top.scoreSideCp >= 200 &&
                    cand.mateIn != null && cand.mateIn > 0
                ) {
                    // Prefer delaying forced mate if non-mating move exists in cap
                    false
                } else {
                    loss <= lossCap
                }
            }.ifEmpty { listOf(top) }

            val topComposite = scoreWithPersonality(
                state, top, top.scoreSideCp, personality, normI, gameSeed, lastMoveFromWing, lastMovedPieceType
            )
            var bestPick = top
            var bestComposite = topComposite

            for (cand in filtered) {
                if (cand.move == top.move) continue
                val sc = scoreWithPersonality(
                    state, cand, top.scoreSideCp, personality, normI, gameSeed, lastMoveFromWing, lastMovedPieceType
                )
                if (sc - topComposite > 0.75 && sc > bestComposite) {
                    bestComposite = sc
                    bestPick = cand
                }
            }

            if (bestPick.move != top.move) {
                return SearchResult(
                    bestMove = bestPick.move,
                    evalWhiteCp = bestPick.evalWhiteCp,
                    depthReached = bestPick.depth,
                    nodesSearched = bestPick.nodes,
                    pvUci = bestPick.pvUci,
                    sourceName = "$sourceLabel • ${personality.nameAr}"
                )
            }
        }

        // Human Style Layer (limited levels only)
        if (humanStyle && !isUnlimited && sorted.size > 1) {
            val inCheck = state.isInCheck(state.turn)
            val halve = inCheck || (tier.ordinal >= StrengthTier.MEDIUM.ordinal && top.scoreSideCp >= 150)
            val effTol = if (halve) tier.humanToleranceCp / 2 else tier.humanToleranceCp
            val effTemp = (if (halve) tier.humanTemperature / 2 else tier.humanTemperature).coerceAtLeast(4)

            val window = sorted.filter { (top.scoreSideCp - it.scoreSideCp) <= effTol }
            if (window.size > 1 && top.scoreSideCp < MATE_SCORE - 500) {
                val chosen = softmaxSample(window, effTemp.toDouble()) { cand ->
                    val loss = (top.scoreSideCp - cand.scoreSideCp).coerceAtLeast(0)
                    val bonus = computeHumanStyleBonus(state, cand.move)
                    bonus - loss.toDouble()
                }
                return SearchResult(
                    bestMove = chosen.move,
                    evalWhiteCp = chosen.evalWhiteCp,
                    depthReached = chosen.depth,
                    nodesSearched = chosen.nodes,
                    pvUci = chosen.pvUci,
                    sourceName = "$sourceLabel • أسلوب بشري"
                )
            }
        }

        return SearchResult(
            bestMove = top.move,
            evalWhiteCp = top.evalWhiteCp,
            depthReached = top.depth,
            nodesSearched = top.nodes,
            pvUci = top.pvUci,
            sourceName = sourceLabel
        )
    }

    private fun scoreWithPersonality(
        state: ChessBoardState,
        cand: CandidateMoveEval,
        bestScoreSideCp: Int,
        personality: EnginePersonality,
        normI: Double,
        gameSeed: Long,
        lastMoveFromWing: Int?,
        lastMovedPieceType: PieceType?
    ): Double {
        val after = state.copyWithMoveFast(cand.move)
        val givesCheck = after.isInCheck(after.turn)
        val loss = (bestScoreSideCp - cand.scoreSideCp).coerceAtLeast(0)
        val features = computePersonalityFeatureScore(
            stateBefore = state,
            afterState = after,
            move = cand.move,
            givesCheck = givesCheck,
            personality = personality,
            evalSideBeforeCp = bestScoreSideCp,
            gameSeed = gameSeed,
            lastMoveFromWing = lastMoveFromWing,
            lastMovedPieceType = lastMovedPieceType
        )
        return features * normI - (1.14 - 0.34 * normI) * loss
    }

    private fun computeHumanStyleBonus(state: ChessBoardState, move: ChessMove): Double {
        val piece = state.pieceAt(move.from) ?: return 0.0
        val after = state.copyWithMoveFast(move)
        var bonus = 0.0

        // Center control
        if (move.to.row in 2..5 && move.to.col in 2..5) bonus += 12.0
        if (move.to.row in 3..4 && move.to.col in 3..4) bonus += 8.0

        // First 12 moves: minor development + castling + delay queen
        if (state.fullMoveNumber <= 12) {
            val backRow = if (state.turn == PieceColor.WHITE) 7 else 0
            if ((piece.type == PieceType.KNIGHT || piece.type == PieceType.BISHOP) &&
                move.from.row == backRow && move.to.row != backRow
            ) {
                bonus += 16.0
            }
            if (move.isCastleKingside || move.isCastleQueenside) {
                bonus += 24.0
            }
            if (piece.type == PieceType.QUEEN) {
                bonus -= 14.0
            }
            if (piece.type == PieceType.PAWN && (move.to.col == 3 || move.to.col == 4)) {
                bonus += 10.0
            }
        }

        // Check bonus
        if (after.isInCheck(after.turn)) bonus += 10.0

        // Penalize leaving piece attacked & undefended
        if (after.isSquareAttacked(move.to, after.turn) && !after.isSquareAttacked(move.to, state.turn)) {
            bonus -= (piece.type.valueCp / 18.0).coerceAtMost(35.0)
        }

        // Proximity to enemy king with heavy/minor pieces
        if (piece.type != PieceType.PAWN && piece.type != PieceType.KING) {
            after.findKing(after.turn)?.let { ek ->
                val dist = abs(move.to.row - ek.row) + abs(move.to.col - ek.col)
                if (dist <= 3) bonus += (4 - dist) * 4.5
            }
        }

        // Avoid creating isolated pawns on capture/push
        if (piece.type == PieceType.PAWN) {
            val col = move.to.col
            val hasFriendlyNeighbor = ((col - 1).coerceAtLeast(0)..(col + 1).coerceAtMost(7))
                .filter { it != col }
                .any { adjCol ->
                    (0..7).any { r ->
                        val p = after.pieceAt(r, adjCol)
                        p?.color == state.turn && p.type == PieceType.PAWN
                    }
                }
            if (!hasFriendlyNeighbor) bonus -= 8.0
        }

        return bonus
    }

    private fun computePersonalityFeatureScore(
        stateBefore: ChessBoardState,
        afterState: ChessBoardState,
        move: ChessMove,
        givesCheck: Boolean,
        personality: EnginePersonality,
        evalSideBeforeCp: Int,
        gameSeed: Long,
        lastMoveFromWing: Int?,
        lastMovedPieceType: PieceType?
    ): Double {
        val piece = stateBefore.pieceAt(move.from) ?: return 0.0
        val phase = stateBefore.detectPhase()
        val earlyMove = stateBefore.fullMoveNumber <= 10
        val oppKing = afterState.findKing(afterState.turn)
        val kingDistAfter = oppKing?.let { abs(move.to.row - it.row) + abs(move.to.col - it.col) } ?: 6
        val kingDistBefore = oppKing?.let { abs(move.from.row - it.row) + abs(move.from.col - it.col) } ?: 6
        val approachingKing = kingDistAfter < kingDistBefore
        val oppMobility = afterState.generateLegalMoves().size
        val repetition = (afterState.positionHistory[afterState.positionKey()] ?: 0) >= 2
        val isHangingSacrifice = afterState.isSquareAttacked(move.to, afterState.turn) &&
            !afterState.isSquareAttacked(move.to, stateBefore.turn) &&
            move.capturedPiece == null

        return when (personality) {
            EnginePersonality.CLASSIC -> 0.0
            EnginePersonality.FIERCE_WARRIOR -> {
                var f = 0.0
                if (givesCheck) f += 18.0
                if (approachingKing) f += 12.0
                if (earlyMove && piece.type == PieceType.PAWN && (move.to.col >= 5 || move.to.col <= 2)) f += 10.0
                if (move.capturedPiece != null && givesCheck) f += 12.0
                f
            }
            EnginePersonality.IRON_FORTRESS -> {
                var f = 0.0
                if (move.isCastleKingside || move.isCastleQueenside) f += 26.0
                if (afterState.isSquareAttacked(move.to, stateBefore.turn)) f += 9.0
                if (move.capturedPiece != null) f += 8.0
                if (isHangingSacrifice) f -= 20.0
                f
            }
            EnginePersonality.STRATEGIC_GENIUS -> {
                var f = 0.0
                if (move.to.row in 2..5 && move.to.col in 2..5) f += 14.0
                if (piece.type == PieceType.KNIGHT || piece.type == PieceType.BISHOP) f += 8.0
                if (afterState.isSquareAttacked(move.to, stateBefore.turn)) f += 8.0
                f
            }
            EnginePersonality.PATIENT_SERPENT -> {
                var f = 0.0
                f += (32 - oppMobility).coerceIn(-8, 18) * 0.8
                if (move.to.row in 2..5 && move.to.col in 2..5) f += 10.0
                if (move.capturedPiece != null && earlyMove) f -= 8.0
                if (givesCheck && earlyMove) f -= 7.0
                if (repetition) f -= 14.0
                f
            }
            EnginePersonality.ENDGAME_MASTER -> {
                var f = 0.0
                if (phase == GamePhase.ENDGAME || piece.type == PieceType.PAWN) f += 12.0
                if (piece.type == PieceType.KING && phase == GamePhase.ENDGAME && move.to.row in 2..5 && move.to.col in 2..5) f += 18.0
                if (evalSideBeforeCp >= 80 && move.capturedPiece != null) f += 15.0
                if (afterState.isSquareAttacked(move.to, stateBefore.turn)) f += 6.0
                f
            }
            EnginePersonality.SACRIFICE_KING -> {
                var f = 0.0
                if (isHangingSacrifice || (move.capturedPiece == null && approachingKing)) f += 16.0
                if (givesCheck) f += 16.0
                if (oppKing != null && kingDistAfter <= 2) f += 14.0
                f
            }
            EnginePersonality.BRILLIANCY_CREATOR -> {
                var f = 0.0
                // Quiet creative moves and calculated attacks without pointless blunders
                if (move.capturedPiece == null && !givesCheck && move.to.row in 1..6 && move.to.col in 1..6) f += 11.0
                if (approachingKing) f += 10.0
                if (isHangingSacrifice && !afterState.isInCheck(afterState.turn)) f -= 12.0
                f
            }
            EnginePersonality.MISTAKE_CATCHER -> {
                var f = 0.0
                f += (34 - oppMobility).coerceIn(-6, 16) * 0.9
                if (move.capturedPiece != null) f += 10.0
                if (givesCheck) f += 10.0
                f
            }
            EnginePersonality.TRICKY_GENIUS -> {
                var f = 0.0
                if (move.capturedPiece == null && !givesCheck) f += 9.0
                // Bait / counter-trap potential when opponent has few safe replies
                f += (30 - oppMobility).coerceIn(-5, 15) * 0.85
                if (approachingKing) f += 9.0
                f
            }
            EnginePersonality.OPPORTUNITY_HUNTER -> {
                var f = 0.0
                if (move.capturedPiece != null) f += 14.0
                if (move.promotion != null) f += 16.0
                if (givesCheck) f += 12.0
                f
            }
            EnginePersonality.LONG_GAME_MAKER -> {
                var f = 0.0
                if (evalSideBeforeCp >= 100) {
                    if (move.capturedPiece != null) f -= 14.0
                    if (givesCheck) f -= 12.0
                    if (repetition) f -= 18.0
                    f += 8.0
                } else {
                    if (move.to.row in 2..5 && move.to.col in 2..5) f += 9.0
                    if (afterState.isSquareAttacked(move.to, stateBefore.turn)) f += 8.0
                }
                f
            }
            EnginePersonality.MYSTERY_PLAYER -> {
                var f = 0.0
                val wing = when {
                    move.to.col <= 2 -> 0
                    move.to.col >= 5 -> 2
                    else -> 1
                }
                if (lastMoveFromWing != null && wing == lastMoveFromWing) f -= 9.0 else f += 6.0
                if (lastMovedPieceType != null && piece.type == lastMovedPieceType) f -= 7.0 else f += 5.0
                val hashSeed = gameSeed xor (stateBefore.fullMoveNumber.toLong() * 31L) xor move.toUci().hashCode().toLong()
                val jitter = (Random(hashSeed).nextDouble() - 0.5) * 18.0
                f + jitter
            }
        }
    }

    private fun <T> softmaxSample(items: List<T>, temperature: Double, scoreFn: (T) -> Double): T {
        if (items.size <= 1) return items.first()
        val scores = items.map(scoreFn)
        val maxS = scores.maxOrNull() ?: 0.0
        val safeTemp = temperature.coerceAtLeast(1.0)
        val weights = scores.map { s -> exp(((s - maxS) / safeTemp).coerceIn(-30.0, 0.0)) }
        val sum = weights.sum()
        if (sum <= 0.0) return items.first()
        var r = ThreadLocalRandom.current().nextDouble() * sum
        for (i in items.indices) {
            r -= weights[i]
            if (r <= 0.0) return items[i]
        }
        return items.last()
    }

    /**
     * Short mate detector (mate in <= [maxMoves] full moves) for Opportunity Hunter.
     */
    fun findShortMateMove(
        state: ChessBoardState,
        maxMoves: Int = 3,
        maxTimeMs: Long = 150L,
        maxNodes: Int = 65_000
    ): ChessMove? {
        val legal = state.generateLegalMoves()
        if (legal.isEmpty()) return null
        val ctx = SearchContext(System.currentTimeMillis(), maxTimeMs, 1.0f, maxNodes = maxNodes)
        val targetDepth = (maxMoves * 2 - 1).coerceIn(1, 5)
        for (move in legal) {
            val next = state.copyWithMoveFast(move)
            if (next.generateLegalMoves().isEmpty() && next.isInCheck(next.turn)) {
                return move
            }
        }
        if (targetDepth <= 1) return null
        val scored = scoreRootMoves(state, legal, min(targetDepth, 3), ctx)
        val top = scored.firstOrNull() ?: return null
        return if (top.second >= MATE_SCORE - targetDepth - 2) top.first else null
    }

    /**
     * MultiPV Position Analysis (Ruqaa Section 8.1: up to [multiPv] = 3 lines).
     */
    fun evaluateMultiPv(
        state: ChessBoardState,
        multiPv: Int = 3,
        timeLimitMs: Long = 1700L,
        maxDepth: Int = 5
    ): List<MultiPvLine> {
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) return emptyList()

        val ctx = SearchContext(System.currentTimeMillis(), timeLimitMs, 1.0f)
        var latestScored = scoreRootMoves(state, legalMoves, 1, ctx)
        var reachedDepth = 1
        for (d in 2..maxDepth) {
            val res = scoreRootMoves(state, legalMoves, d, ctx)
            if (ctx.timedOut && res.isEmpty()) break
            if (res.isNotEmpty()) {
                latestScored = res
                reachedDepth = d
            }
            if (ctx.timedOut) break
        }

        return latestScored.take(multiPv).mapIndexed { idx, (move, scoreSide) ->
            val evalWhite = if (state.turn == PieceColor.WHITE) scoreSide else -scoreSide
            val mateIn: Int? = if (abs(scoreSide) >= MATE_SCORE - 500) {
                val plies = (MATE_SCORE - abs(scoreSide)).coerceAtLeast(1)
                val full = (plies + 1) / 2
                if (evalWhite >= 0) full else -full
            } else null

            val san = state.toSan(move, legalMoves)
            val pvUci = extractPvLine(state, move, ctx.tt, maxPlies = 6)
            val pvSan = convertPvUciToSan(state, pvUci)

            MultiPvLine(
                rank = idx + 1,
                move = move,
                san = san,
                evalWhiteCp = evalWhite,
                mateIn = mateIn,
                depth = reachedDepth,
                pvSan = pvSan,
                pvUci = pvUci,
                nodes = ctx.nodes.toLong()
            )
        }
    }

    fun convertPvUciToSan(startState: ChessBoardState, pvUci: List<String>): List<String> {
        val sans = mutableListOf<String>()
        var curr = startState
        for (uci in pvUci) {
            val legal = curr.generateLegalMoves()
            val mv = legal.find { it.toUci().equals(uci, ignoreCase = true) } ?: break
            sans.add(curr.toSan(mv, legal))
            curr = curr.copyWithMoveFast(mv)
        }
        return sans
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
        if (legalMoves.size == 1) {
            val m = legalMoves[0]
            return SearchResult(m, evaluateWhitePerspective(state.copyWithMoveFast(m), 1.0f), 1, 1, listOf(m.toUci()))
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

        if (state.halfMoveClock >= 150 || state.hasInsufficientMaterial()) return 0

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
            if (ttPackedMove != 0 && packed == ttPackedMove) score += 100_000
            if (move.capturedPiece != null) {
                val attackerVal = state.pieceAt(move.from)?.type?.valueCp ?: 100
                score += 20_000 + move.capturedPiece.type.valueCp * 10 - attackerVal
            }
            if (move.promotion != null) score += 18_000 + move.promotion.valueCp
            if (move.isCastleKingside || move.isCastleQueenside) score += 650
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
        var phase = 0

        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)
        val whitePawnRanks = IntArray(8) { 7 }
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

        if (whiteBishops >= 2) { mgWhite += 35; egWhite += 45 }
        if (blackBishops >= 2) { mgBlack += 35; egBlack += 45 }

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

            if (whitePawnFiles[c] > 0) {
                val wRank = whitePawnRanks[c]
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
                val bRank = blackPawnRanks[c]
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
            val uci = openings[ThreadLocalRandom.current().nextInt(openings.size)]
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
            val uci = candidates[ThreadLocalRandom.current().nextInt(candidates.size)]
            return legalMoves.find { it.toUci() == uci }
        }
        return null
    }
}
