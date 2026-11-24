package com.example.chess

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToInt

object ChessCoachAnalyzer {

    data class MoveEvaluationResult(
        val evalBeforeWhiteCp: Int,
        val evalAfterWhiteCp: Int,
        val centipawnLoss: Int,
        val quality: MoveQuality,
        val bestMoveSan: String,
        val bestMove: ChessMove?,
        val coachNoteAr: String
    )

    /**
     * Ruqaa Section 8.3 thresholds:
     * < 50: جيدة, 50..99: عدم دقة, 100..199: خطأ, >= 200: خطأ كبير
     */
    fun classifyLossRuqaa(cpLoss: Int, isMateDelivered: Boolean = false): MoveQuality {
        return when {
            isMateDelivered -> MoveQuality.BRILLIANT
            cpLoss < 15 -> MoveQuality.BEST
            cpLoss < 30 -> MoveQuality.EXCELLENT
            cpLoss < 50 -> MoveQuality.GOOD
            cpLoss < 100 -> MoveQuality.INACCURACY
            cpLoss < 200 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }
    }

    fun toFigurineSan(san: String): String {
        if (san.isBlank()) return san
        val sb = StringBuilder(san.length)
        for (ch in san) {
            when (ch) {
                'K' -> sb.append('♔')
                'Q' -> sb.append('♕')
                'R' -> sb.append('♖')
                'B' -> sb.append('♗')
                'N' -> sb.append('♘')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Detects immediate tactical threats created by the side NOT to move
     * against the current side to move's pieces (or undefended / high-value captures).
     */
    fun detectThreats(state: ChessBoardState): List<ThreatArrow> {
        val defenderColor = state.turn
        val attackerColor = defenderColor.opposite()
        val attackerState = if (state.turn == attackerColor) state else state.copyWithNullMove()
        val attackerMoves = attackerState.generatePseudoLegalMoves(attackerColor)
        val threats = mutableListOf<ThreatArrow>()
        val seenTargets = mutableSetOf<Square>()

        for (mv in attackerMoves) {
            val target = state.pieceAt(mv.to) ?: continue
            if (target.color != defenderColor || target.type == PieceType.KING) continue
            val attackerPiece = state.pieceAt(mv.from) ?: continue
            val isUndefended = !state.isSquareAttacked(mv.to, defenderColor)
            val winningExchange = attackerPiece.type.valueCp < target.type.valueCp
            if (isUndefended || winningExchange) {
                if (seenTargets.add(mv.to)) {
                    threats.add(ThreatArrow(from = mv.from, to = mv.to, targetPiece = target))
                }
            }
        }
        return threats.sortedByDescending { it.targetPiece.type.valueCp }.take(4)
    }

    fun gradeLabelFromLoss(cpLoss: Int?): String {
        if (cpLoss == null) return "تحليل غير مكتمل"
        return when {
            cpLoss < 50 -> "جيدة"
            cpLoss < 100 -> "عدم دقة"
            cpLoss < 200 -> "خطأ"
            else -> "خطأ كبير"
        }
    }

    fun computeReviewCacheKey(
        engineId: String,
        startFen: String,
        uciMoves: List<String>
    ): String {
        val raw = "$engineId|$startFen|${uciMoves.joinToString(" ")}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun analyzePlayedMove(
        stateBefore: ChessBoardState,
        playedMove: ChessMove,
        legalBefore: List<ChessMove>,
        depth: Int = 2
    ): MoveEvaluationResult {
        val bestSearch = ChessEngine.evaluatePositionBestMove(stateBefore, depth = depth, timeLimitMs = 220L)
        val bestMove = bestSearch.bestMove
        val bestEvalWhite = bestSearch.evalWhiteCp
        val bestMoveSan = bestMove?.let { stateBefore.toSan(it, legalBefore) } ?: ""

        val playedEvalWhite = if (bestMove == playedMove) {
            bestEvalWhite
        } else {
            ChessEngine.evaluateMoveScoreWhiteCp(stateBefore, playedMove, depth = depth, timeLimitMs = 320L)
        }

        val moverIsWhite = stateBefore.turn == PieceColor.WHITE
        val cpLoss = if (moverIsWhite) {
            (bestEvalWhite - playedEvalWhite).coerceIn(0, 1000)
        } else {
            (playedEvalWhite - bestEvalWhite).coerceIn(0, 1000)
        }

        val afterState = stateBefore.copyWithMoveFast(playedMove)
        val isMateDelivered = afterState.generateLegalMoves().isEmpty() && afterState.isInCheck(afterState.turn)
        val movedPiece = stateBefore.pieceAt(playedMove.from)

        val quality = when {
            isMateDelivered -> MoveQuality.BRILLIANT
            bestMove == playedMove && movedPiece != null && movedPiece.type != PieceType.PAWN &&
                afterState.isSquareAttacked(playedMove.to, afterState.turn) && cpLoss == 0 -> MoveQuality.BRILLIANT
            else -> classifyLossRuqaa(cpLoss, isMateDelivered = false)
        }

        val note = buildArabicMoveNote(
            stateBefore = stateBefore,
            afterState = afterState,
            playedMove = playedMove,
            bestMove = bestMove,
            bestMoveSan = bestMoveSan,
            quality = quality,
            cpLoss = cpLoss
        )

        return MoveEvaluationResult(
            evalBeforeWhiteCp = bestEvalWhite,
            evalAfterWhiteCp = playedEvalWhite,
            centipawnLoss = cpLoss,
            quality = quality,
            bestMoveSan = bestMoveSan,
            bestMove = bestMove,
            coachNoteAr = note
        )
    }

    /**
     * Ruqaa Section 8.2 3-Stage Progressive Hint:
     * Stage 1: General tactical/strategic concept
     * Stage 2: Piece & square focus
     * Stage 3: Full SAN + from -> to arrow
     */
    fun generateProgressiveHint(
        state: ChessBoardState,
        precomputedBest: ChessMove? = null
    ): ProgressiveHint? {
        val legal = state.generateLegalMoves()
        if (legal.isEmpty()) return null
        val best = precomputedBest
            ?: ChessEngine.evaluatePositionBestMove(state, depth = 4, timeLimitMs = 320L).bestMove
            ?: legal.first()

        val san = state.toSan(best, legal)
        val piece = state.pieceAt(best.from)
        val after = state.copyWithMoveFast(best)
        val givesCheck = after.isInCheck(after.turn)

        val conceptAr = when {
            givesCheck -> "ابحث عن نقلة تُعطي كشًا لملك الخصم."
            best.promotion != null -> "لديك بيدق يستطيع الترقية. افحص نقلة ترقيته."
            best.capturedPiece != null -> "افحص فرص الأخذ المتاحة، وتحقّق من الردود بعدها."
            best.isCastleKingside || best.isCastleQueenside -> "فكّر في تأمين الملك بالتبييت."
            else -> "ابحث عن تحسين تمركز إحدى قطعك؛ قارن المربعات المتاحة لها."
        }

        val pieceName = piece?.type?.nameAr ?: "القطعة"
        val pieceHintAr = "ركّز على $pieceName الموجود في ${best.from.toAlgebraic()}."
        val exactMoveAr = "$san (${best.from.toAlgebraic()} ← ${best.to.toAlgebraic()})"

        return ProgressiveHint(
            fen = state.toFen(),
            move = best,
            san = san,
            conceptAr = conceptAr,
            pieceHintAr = pieceHintAr,
            exactMoveAr = exactMoveAr
        )
    }

    fun generateHint(state: ChessBoardState, precomputedBest: ChessMove? = null): MoveHint? {
        val prog = generateProgressiveHint(state, precomputedBest) ?: return null
        return MoveHint(
            move = prog.move,
            san = prog.san,
            explanationAr = "${prog.conceptAr} (${prog.exactMoveAr})"
        )
    }

    private fun buildArabicMoveNote(
        stateBefore: ChessBoardState,
        afterState: ChessBoardState,
        playedMove: ChessMove,
        bestMove: ChessMove?,
        bestMoveSan: String,
        quality: MoveQuality,
        cpLoss: Int
    ): String {
        val piece = stateBefore.pieceAt(playedMove.from)
        val mover = stateBefore.turn
        val opponent = mover.opposite()

        return when (quality) {
            MoveQuality.BRILLIANT -> "نقلة مذهلة! جمعت بين الدقة التكتيكية والجرأة في الضغط على الخصم."
            MoveQuality.BEST -> when {
                playedMove.isCastleKingside || playedMove.isCastleQueenside ->
                    "تبييت في التوقيت المثالي لحماية الملك وتفعيل القلعة."
                playedMove.capturedPiece != null ->
                    "أسر دقيق لـ ${playedMove.capturedPiece.type.nameAr} يحسم التبادل لصالحك."
                else -> "أفضل نقلة متاحة! حافظت على أعلى دقة في الموقف."
            }
            MoveQuality.EXCELLENT -> "نقلة جيدة ومتوازنة تدعم خطتك الاستراتيجية."
            MoveQuality.GOOD -> "نقلة جيدة (خسارة $cpLoss سنتيبيدق)، والخيار الأدق كان ($bestMoveSan)."
            MoveQuality.INACCURACY -> {
                if (stateBefore.detectPhase() == GamePhase.OPENING && !playedMove.isCastleKingside &&
                    (if (mover == PieceColor.WHITE) stateBefore.castling.whiteKingside else stateBefore.castling.blackKingside) &&
                    stateBefore.fullMoveNumber >= 6
                ) {
                    "عدم دقة: تأخرت في التبييت وحماية الملك. كان ($bestMoveSan) أفضل."
                } else {
                    "عدم دقة (خسارة ${(cpLoss / 100.0).format1()} بيدق). النقلة ($bestMoveSan) كانت تمنحك موقفاً أقوى."
                }
            }
            MoveQuality.MISTAKE -> {
                val leftHanging = piece != null && piece.type != PieceType.KING &&
                    afterState.isSquareAttacked(playedMove.to, opponent) &&
                    !afterState.isSquareAttacked(playedMove.to, mover)
                when {
                    leftHanging ->
                        "خطأ تكتيكي! وضعت ${piece?.type?.nameAr} في مربع مهدد دون حماية كافية. الأفضل كان ($bestMoveSan)."
                    bestMove?.capturedPiece != null ->
                        "أضعت فرصة أسر ${bestMove.capturedPiece.type.nameAr} عبر ($bestMoveSan)."
                    else ->
                        "خطأ أفقدك أفضلية ملموسة (خسارة ${(cpLoss / 100.0).format1()} بيدق). الأفضل كان ($bestMoveSan)."
                }
            }
            MoveQuality.BLUNDER -> {
                val leftHanging = piece != null && piece.type != PieceType.KING &&
                    afterState.isSquareAttacked(playedMove.to, opponent)
                when {
                    leftHanging ->
                        "خطأ كبير! تركت ${piece?.type?.nameAr} عرضة للأسر أو الهجوم المضاد. كان يجب لعب ($bestMoveSan)."
                    else ->
                        "خطأ كبير قلب تقييم المباراة! الأفضل كان ($bestMoveSan)."
                }
            }
        }
    }

    fun buildPostGameReport(
        moves: List<RecordedMove>,
        playerColor: PieceColor,
        result: GameResult,
        bot: BotPersona,
        currentPlayerElo: Int
    ): PostGameReport {
        val playerMoves = moves.filter { it.color == playerColor }
        val opponentMoves = moves.filter { it.color != playerColor }

        val playerAccuracy = computeAccuracy(playerMoves)
        val opponentAccuracy = computeAccuracy(opponentMoves)
        val acpl = if (playerMoves.isEmpty()) 0 else playerMoves.map { it.centipawnLoss }.average().roundToInt()
        val opponentAcpl = if (opponentMoves.isEmpty()) 0 else opponentMoves.map { it.centipawnLoss }.average().roundToInt()

        val openingAcc = computeAccuracy(playerMoves.filter { it.phase == GamePhase.OPENING })
        val midAcc = computeAccuracy(playerMoves.filter { it.phase == GamePhase.MIDDLEGAME })
        val endAcc = computeAccuracy(playerMoves.filter { it.phase == GamePhase.ENDGAME })

        val qualityCounts = MoveQuality.entries.associateWith { q ->
            playerMoves.count { it.quality == q }
        }
        val opponentQualityCounts = MoveQuality.entries.associateWith { q ->
            opponentMoves.count { it.quality == q }
        }

        val uciList = moves.map { it.move.toUci() }
        val openingName = ChessBoardState.recognizeOpeningArabic(uciList)

        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val actionPlan = mutableListOf<String>()

        val blunders = qualityCounts[MoveQuality.BLUNDER] ?: 0
        val mistakes = qualityCounts[MoveQuality.MISTAKE] ?: 0
        val brilliants = qualityCounts[MoveQuality.BRILLIANT] ?: 0
        val bestCount = (qualityCounts[MoveQuality.BEST] ?: 0) + (qualityCounts[MoveQuality.EXCELLENT] ?: 0)

        if (playerAccuracy >= 82) {
            strengths.add("دقة لعب مرتفعة جداً (${playerAccuracy}%) تعكس تركيزاً ممتازاً.")
        }
        if (openingAcc >= 80 && playerMoves.any { it.phase == GamePhase.OPENING }) {
            strengths.add("افتتاح منظم وقوي بنسبة دقة ${openingAcc}% في $openingName.")
        }
        if (midAcc >= 78 && playerMoves.any { it.phase == GamePhase.MIDDLEGAME }) {
            strengths.add("أداء تكتيكي متماسك في وسط اللعب (${midAcc}%).")
        }
        if (endAcc >= 80 && playerMoves.any { it.phase == GamePhase.ENDGAME }) {
            strengths.add("إدارة ذكية لنهاية المباراة ودقة عالية في تحريك الملك والبيادق.")
        }
        if (brilliants > 0) {
            strengths.add("اكتشاف $brilliants نقلة عبقرية أظهرت رؤية تكتيكية حادة.")
        }
        if (blunders == 0 && playerMoves.size >= 6) {
            strengths.add("انضباط دفاعي رائع: أنهيت المباراة دون أي أخطاء كبيرة!")
        }
        if (strengths.isEmpty() && bestCount > 0) {
            strengths.add("قدمت $bestCount نقلة جيدة وتطويراً منظماً للقطع في بداية اللعب.")
        } else if (strengths.isEmpty()) {
            strengths.add("شجاعة في خوض التحدي واكتساب خبرة عملية أمام ${bot.nameAr}.")
        }

        if (blunders > 0) {
            weaknesses.add("ارتكبت $blunders أخطاء كبيرة أدت إلى خسارة قطع أو أفضلية حاسمة.")
            actionPlan.add("طبّق قاعدة (فحص التهديدات الثلاث): قبل كل نقلة افحص الكشّات، الأخذ، والقطع المهددة.")
        }
        if (mistakes >= 2) {
            weaknesses.add("تكرار الأخطاء التكتيكية ($mistakes أخطاء) أثناء تبادل القطع.")
            actionPlan.add("حلّ 5 ألغاز تكتيكية يومياً لرفع سرعة التقاط الشوكة والتثبيت.")
        }
        if (openingAcc < 72 && playerMoves.any { it.phase == GamePhase.OPENING }) {
            weaknesses.add("انخفاض الدقة في مرحلة الافتتاح (${openingAcc}%) مما صعّب الموقف مبكراً.")
            actionPlan.add("التزم بقاعدة الافتتاح: السيطرة على الوسط، إخراج الأحصنة والأفيال، والتبييت مبكراً.")
        }
        if (midAcc < 72 && playerMoves.any { it.phase == GamePhase.MIDDLEGAME }) {
            weaknesses.add("تذبذب الحسابات في وسط اللعب (${midAcc}%) عند تعقد التفاعلات بين القطع.")
            actionPlan.add("لا تتسرع في التبادل؛ اسأل نفسك ما هي أسوأ قطعة لدي وكيف أحسّن موقعها؟")
        }
        if (endAcc < 75 && playerMoves.any { it.phase == GamePhase.ENDGAME }) {
            weaknesses.add("فقدان نقاط مهمة في نهاية اللعب (${endAcc}%).")
            actionPlan.add("تدرّب على تفعيل الملك نحو الوسط في النهايات وحماية البيادق السالكة.")
        }

        if (weaknesses.isEmpty()) {
            weaknesses.add("بعض التنازلات الطفيفة فقط التي يمكن صقلها بمواجهة مستوى أعلى.")
        }
        if (actionPlan.isEmpty()) {
            actionPlan.add("مستواك متقدم! جرّب تحدي مستوى أعلى أو شخصيات محرك متقدمة.")
        }

        val diagnosis = when {
            blunders >= 2 -> "التشخيص الرئيسي: التسرع في اللعب دون التحقق من القطع غير المحمية وتهديدات الخصم المباشرة."
            openingAcc < midAcc - 12 -> "التشخيص الرئيسي: تبدأ المباراة بفقدان تمبو في الافتتاح ثم تحاول التعويض في وسط اللعب."
            playerMoves.any { it.phase == GamePhase.ENDGAME } && endAcc < midAcc - 10 ->
                "التشخيص الرئيسي: تلعب الافتتاح والوسط بقوة، لكن الدقة تنخفض عند الانتقال لنهاية المباراة."
            acpl <= 35 -> "التشخيص الرئيسي: أسلوب لعب متوازن وعالي الانضباط؛ ركّز على الخطط العميقة."
            else -> "التشخيص الرئيسي: أساسك جيد وتحتاج فقط لتثبيت الحسابات التكتيكية لخطوتين للأمام."
        }

        val actualScore = when {
            (result == GameResult.WHITE_WINS_CHECKMATE || result == GameResult.BLACK_RESIGNED || result == GameResult.BLACK_TIMEOUT) && playerColor == PieceColor.WHITE -> 1.0
            (result == GameResult.BLACK_WINS_CHECKMATE || result == GameResult.WHITE_RESIGNED || result == GameResult.WHITE_TIMEOUT) && playerColor == PieceColor.BLACK -> 1.0
            result.isDraw -> 0.5
            else -> 0.0
        }
        val expected = 1.0 / (1.0 + Math.pow(10.0, (bot.elo - currentPlayerElo) / 400.0))
        val rawDelta = (32.0 * (actualScore - expected)).roundToInt()
        val eloDelta = if (playerMoves.size < 4 && actualScore == 0.0) 0 else rawDelta

        return PostGameReport(
            playerAccuracy = playerAccuracy,
            opponentAccuracy = opponentAccuracy,
            acpl = acpl,
            opponentAcpl = opponentAcpl,
            openingNameAr = openingName,
            openingAccuracy = openingAcc,
            middlegameAccuracy = midAcc,
            endgameAccuracy = endAcc,
            qualityCounts = qualityCounts,
            opponentQualityCounts = opponentQualityCounts,
            strengthsAr = strengths,
            weaknessesAr = weaknesses,
            diagnosisAr = diagnosis,
            actionPlanAr = actionPlan,
            eloDelta = eloDelta
        )
    }

    private fun computeAccuracy(moves: List<RecordedMove>): Int {
        if (moves.isEmpty()) return 85
        val avgLoss = moves.map { it.centipawnLoss.coerceAtMost(600) }.average()
        val acc = (103.0 * Math.exp(-0.0045 * avgLoss)).roundToInt()
        return acc.coerceIn(15, 99)
    }

    private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
}
