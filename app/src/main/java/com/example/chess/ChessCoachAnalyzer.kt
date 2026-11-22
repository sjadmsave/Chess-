package com.example.chess

import kotlin.math.abs
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

    fun analyzePlayedMove(
        stateBefore: ChessBoardState,
        playedMove: ChessMove,
        legalBefore: List<ChessMove>,
        depth: Int = 2
    ): MoveEvaluationResult {
        val bestSearch = ChessEngine.evaluatePositionBestMove(stateBefore, depth = depth, timeLimitMs = 420L)
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
            (bestEvalWhite - playedEvalWhite).coerceAtLeast(0)
        } else {
            (playedEvalWhite - bestEvalWhite).coerceAtLeast(0)
        }

        val afterState = stateBefore.copyWithMoveFast(playedMove)
        val isMateDelivered = afterState.generateLegalMoves().isEmpty() && afterState.isInCheck(afterState.turn)
        val movedPiece = stateBefore.pieceAt(playedMove.from)

        val quality = when {
            isMateDelivered -> MoveQuality.BRILLIANT
            bestMove == playedMove && movedPiece != null && movedPiece.type != PieceType.PAWN &&
                afterState.isSquareAttacked(playedMove.to, afterState.turn) && cpLoss == 0 -> MoveQuality.BRILLIANT
            cpLoss <= 15 -> MoveQuality.BEST
            cpLoss <= 45 -> MoveQuality.EXCELLENT
            cpLoss <= 95 -> MoveQuality.GOOD
            cpLoss <= 185 -> MoveQuality.INACCURACY
            cpLoss <= 360 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
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

    fun generateHint(state: ChessBoardState): MoveHint? {
        val legal = state.generateLegalMoves()
        if (legal.isEmpty()) return null
        val search = ChessEngine.evaluatePositionBestMove(state, depth = 3, timeLimitMs = 550L)
        val best = search.bestMove ?: legal.first()
        val san = state.toSan(best, legal)
        val piece = state.pieceAt(best.from)
        val after = state.copyWithMoveFast(best)

        val reason = buildString {
            when {
                after.generateLegalMoves().isEmpty() && after.isInCheck(after.turn) ->
                    append("هذه النقلة تنهي المباراة بـ كش مات مباشرة!")
                best.isCastleKingside || best.isCastleQueenside ->
                    append("التبييت يؤمّن ملكك ويُدخل القلعة في اللعب.")
                best.promotion != null ->
                    append("ترقية البيدق تمنحك تفوقاً حاسماً في القوة المادية.")
                best.capturedPiece != null ->
                    append("أسر ${best.capturedPiece.type.nameAr} يكسبك تفوقاً مادياً مهماً.")
                after.isInCheck(after.turn) ->
                    append("توجيه كش للملك يجبر الخصم على الدفاع ويكسبك المبادرة.")
                best.to.row in 3..4 && best.to.col in 3..4 ->
                    append("تحريك ${piece?.type?.nameAr ?: "القطعة"} إلى ${best.to.toAlgebraic()} يعزز السيطرة على وسط الرقعة.")
                else ->
                    append("نقل ${piece?.type?.nameAr ?: "القطعة"} إلى ${best.to.toAlgebraic()} يحسّن تنسيق قطعك وتوازن الموقف.")
            }
        }
        return MoveHint(move = best, san = san, explanationAr = reason)
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
            MoveQuality.EXCELLENT -> "نقلة قوية ومتوازنة تدعم خطتك الاستراتيجية."
            MoveQuality.GOOD -> "نقلة مقبولة، رغم وجود خيار أدق مثل ($bestMoveSan)."
            MoveQuality.INACCURACY -> {
                if (stateBefore.detectPhase() == GamePhase.OPENING && !playedMove.isCastleKingside &&
                    (if (mover == PieceColor.WHITE) stateBefore.castling.whiteKingside else stateBefore.castling.blackKingside) &&
                    stateBefore.fullMoveNumber >= 6
                ) {
                    "عدم دقة: تأخرت في التبييت وحماية الملك. كان ($bestMoveSan) أفضل."
                } else {
                    "عدم دقة طفيفة (خسارة ${(cpLoss / 100.0).format1()} بيدق). النقلة ($bestMoveSan) كانت تمنحك موقفاً أقوى."
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
                        "خطأ تكتيكي أفقدك جزءاً من الأفضلية. راجع النقلة البديلة ($bestMoveSan)."
                }
            }
            MoveQuality.BLUNDER -> {
                val leftHanging = piece != null && piece.type != PieceType.KING &&
                    afterState.isSquareAttacked(playedMove.to, opponent)
                when {
                    leftHanging ->
                        "خطأ فادح! تركت ${piece?.type?.nameAr} عرضة للأسر أو الهجوم المضاد. كان يجب لعب ($bestMoveSan)."
                    else ->
                        "خطأ فادح قلب تقييم المباراة! تأكد دائماً من فحص الكشّات والتهديدات قبل تحريك القطعة. الأفضل: ($bestMoveSan)."
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

        val openingAcc = computeAccuracy(playerMoves.filter { it.phase == GamePhase.OPENING })
        val midAcc = computeAccuracy(playerMoves.filter { it.phase == GamePhase.MIDDLEGAME })
        val endAcc = computeAccuracy(playerMoves.filter { it.phase == GamePhase.ENDGAME })

        val qualityCounts = MoveQuality.entries.associateWith { q ->
            playerMoves.count { it.quality == q }
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
            strengths.add("انضباط دفاعي رائع: أنهيت المباراة دون أي أخطاء فادحة (Blunders)!")
        }
        if (strengths.isEmpty() && bestCount > 0) {
            strengths.add("قدمت $bestCount نقلة ممتازة وتطويراً جيداً للقطع في بداية اللعب.")
        } else if (strengths.isEmpty()) {
            strengths.add("شجاعة في خوض التحدي واكتساب خبرة عملية أمام ${bot.nameAr}.")
        }

        if (blunders > 0) {
            weaknesses.add("ارتكبت $blunders أخطاء فادحة أدت إلى خسارة قطع أو أفضلية حاسمة.")
            actionPlan.add("طبّق قاعدة (فحص التهديدات الثلاث): قبل كل نقلة افحص الكشّات، الأخذ، والقطع المهددة.")
        }
        if (mistakes >= 2) {
            weaknesses.add("تكرار الأخطاء التكتيكية المتوسطة ($mistakes أخطاء) أثناء تبادل القطع.")
            actionPlan.add("حلّ 5 ألغاز تكتيكية يومياً في قسم الألغاز لرفع سرعة التقاط الشوكة والتثبيت.")
        }
        if (openingAcc < 72 && playerMoves.any { it.phase == GamePhase.OPENING }) {
            weaknesses.add("انخفاض الدقة في مرحلة الافتتاح (${openingAcc}%) مما صعّب الموقف مبكراً.")
            actionPlan.add("التزم بقاعدة الافتتاح الذهبية: السيطرة على الوسط، إخراج الأحصنة والأفيال، والتبييت خلال أول 8 نقلات.")
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
            actionPlan.add("مستواك متقدم! جرّب تحدي مستوى أعلى أو التحدي التكيفي لصقل الدقة أكثر.")
        }

        val diagnosis = when {
            blunders >= 2 -> "التشخيص الرئيسي: التسرع في اللعب دون التحقق من القطع غير المحمية وتهديدات الخصم المباشرة."
            openingAcc < midAcc - 12 -> "التشخيص الرئيسي: تبدأ المباراة بفقدان تمبو في الافتتاح ثم تحاول التعويض في وسط اللعب."
            playerMoves.any { it.phase == GamePhase.ENDGAME } && endAcc < midAcc - 10 ->
                "التشخيص الرئيسي: تلعب الافتتاح والوسط بقوة، لكن الدقة تنخفض عند الانتقال لنهاية المباراة."
            acpl <= 35 -> "التشخيص الرئيسي: أسلوب لعب متوازن وعالي الانضباط؛ ركّز على الخطط العميقة لانتزاع الفوز من الأساتذة."
            else -> "التشخيص الرئيسي: أساسك جيد وتحتاج فقط لتثبيت الحسابات التكتيكية لخطوتين للأمام."
        }

        // Compute ELO delta
        val actualScore = when {
            (result == GameResult.WHITE_WINS_CHECKMATE || result == GameResult.BLACK_RESIGNED) && playerColor == PieceColor.WHITE -> 1.0
            (result == GameResult.BLACK_WINS_CHECKMATE || result == GameResult.WHITE_RESIGNED) && playerColor == PieceColor.BLACK -> 1.0
            result == GameResult.DRAW_STALEMATE || result == GameResult.DRAW_INSUFFICIENT_MATERIAL ||
                result == GameResult.DRAW_FIFTY_MOVE || result == GameResult.DRAW_REPETITION -> 0.5
            else -> 0.0
        }
        val expected = 1.0 / (1.0 + Math.pow(10.0, (bot.elo - currentPlayerElo) / 400.0))
        val rawDelta = (32.0 * (actualScore - expected)).roundToInt()
        val eloDelta = if (playerMoves.size < 4 && actualScore == 0.0) 0 else rawDelta

        return PostGameReport(
            playerAccuracy = playerAccuracy,
            opponentAccuracy = opponentAccuracy,
            acpl = acpl,
            openingNameAr = openingName,
            openingAccuracy = openingAcc,
            middlegameAccuracy = midAcc,
            endgameAccuracy = endAcc,
            qualityCounts = qualityCounts,
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

    private fun Double.format1(): String = String.format(java.util.Locale.US, "%.1f", this)
}
