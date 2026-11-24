package com.example

import com.example.chess.BoardTheme
import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.ChessCoachAnalyzer
import com.example.chess.ChessEngine
import com.example.chess.EngineBackendPreference
import com.example.chess.EnginePersonality
import com.example.chess.GamePhase
import com.example.chess.GameResult
import com.example.chess.MoveQuality
import com.example.chess.PersonalityRisk
import com.example.chess.PgnFenCodec
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PuzzleCategory
import com.example.chess.PuzzleRepository
import com.example.chess.RecordedMove
import com.example.chess.SoundProfile
import com.example.chess.StockfishUciEngine
import com.example.chess.StrengthTier
import com.example.chess.SyzygyManager
import com.example.chess.TimeControl
import com.example.ui.components.formatEvalPawns
import com.example.ui.components.groupMovesIntoTurns
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineAndPuzzlesTest {

    @Test
    fun verifyAllPuzzleSolutionsAreLegalAndMatesDeliverCheckmate() {
        assertTrue("Expected at least 295 puzzles, found ${PuzzleRepository.puzzles.size}", PuzzleRepository.puzzles.size >= 295)
        val ids = PuzzleRepository.puzzles.map { it.id }
        assertEquals("Duplicate puzzle IDs detected", ids.size, ids.toSet().size)
        val fens = PuzzleRepository.puzzles.map { it.fen.trim() }
        assertEquals("Duplicate puzzle FENs detected", fens.size, fens.toSet().size)

        for (puzzle in PuzzleRepository.puzzles) {
            val initialState = ChessBoardState.fromFen(puzzle.fen)
            var state = initialState
            assertTrue("Puzzle ${puzzle.id} has empty solution", puzzle.solutionUci.isNotEmpty())
            for (uci in puzzle.solutionUci) {
                val legal = state.generateLegalMoves()
                val move = legal.find { it.toUci() == uci }
                assertNotNull("Puzzle ${puzzle.id} illegal move $uci at FEN ${state.toFen()}", move)
                state = state.copyWithMove(move!!)
            }
            if (puzzle.category == PuzzleCategory.MATE_IN_1 || puzzle.category == PuzzleCategory.MATE_IN_2) {
                val expectedWinner = if (initialState.turn == PieceColor.WHITE) {
                    GameResult.WHITE_WINS_CHECKMATE
                } else {
                    GameResult.BLACK_WINS_CHECKMATE
                }
                val result = state.evaluateGameResult()
                assertEquals(
                    "Puzzle ${puzzle.id} did not end in checkmate (got $result)",
                    expectedWinner,
                    result
                )
            }
        }
    }

    @Test
    fun verifyEngineChoosesLegalMoveAndCoachAnalyzes() {
        val state = ChessBoardState.initial()
        val legal = state.generateLegalMoves()
        assertEquals(20, legal.size)

        val e4 = legal.first { it.toUci() == "e2e4" }
        val analysis = ChessCoachAnalyzer.analyzePlayedMove(state, e4, legal, depth = 2)
        assertTrue(analysis.centipawnLoss >= 0)
        assertTrue(analysis.coachNoteAr.isNotBlank())

        val afterE4 = state.copyWithMove(e4)
        val botResult = ChessEngine.chooseBotMove(afterE4, BotPersona.TAL)
        assertNotNull(botResult.bestMove)
        assertTrue(afterE4.generateLegalMoves().contains(botResult.bestMove))
    }

    @Test
    fun verifyExpandedPersonasPieceStylesAndBoardThemes() {
        assertEquals(8, BotPersona.entries.size)
        assertEquals(16, PieceStyle.entries.size)
        assertEquals(12, BoardTheme.entries.size)
        assertEquals(8, SoundProfile.entries.size)
        assertEquals(4, TimeControl.entries.size)
        assertEquals(50, EnginePersonality.entries.size)
        assertEquals(5, StrengthTier.entries.size)

        val state = ChessBoardState.initial()
        for (bot in BotPersona.entries) {
            val res = ChessEngine.chooseBotMove(state, bot)
            assertNotNull("Bot ${bot.id} failed to produce move", res.bestMove)
        }
    }

    @Test
    fun verifyPgnFenCodecAndAll13EnginePersonalities() {
        val samplePgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6"
        val parsed = PgnFenCodec.importAutoDetect(samplePgn)
        assertTrue(parsed.isSuccess)
        val data = parsed.getOrNull()!!
        assertEquals(6, data.moves.size)
        assertEquals("e4", data.moves[0].san)

        val fenStr = data.finalBoard.toFen()
        val parsedFen = PgnFenCodec.parseFenStrict(fenStr)
        assertTrue(parsedFen.isSuccess)

        val exportedPgn = PgnFenCodec.exportPgn(data.moves)
        assertTrue(exportedPgn.contains("1. e4 e5"))

        for (personality in EnginePersonality.entries) {
            val moveRes = ChessEngine.chooseCustomEngineMove(
                state = data.finalBoard,
                targetElo = 1400,
                personality = personality,
                humanStyle = true
            )
            assertNotNull("Personality ${personality.id} failed to produce move", moveRes.bestMove)
            assertTrue(data.finalBoard.generateLegalMoves().contains(moveRes.bestMove))
        }

        val multiPv = ChessEngine.evaluateMultiPv(data.finalBoard, multiPv = 3, timeLimitMs = 300L, maxDepth = 2)
        assertTrue(multiPv.isNotEmpty())
        assertTrue(multiPv.size <= 3)
        assertEquals(1, multiPv.first().rank)
    }

    @Test
    fun verifyRuqaaStrengthTranslationFormulas() {
        assertEquals(-1, StrengthTier.snapElo(-1))
        assertEquals(200, StrengthTier.snapElo(120))
        assertEquals(700, StrengthTier.snapElo(712))
        assertEquals(3000, StrengthTier.snapElo(3100))

        assertEquals(StrengthTier.BEGINNER, StrengthTier.fromElo(300))
        assertEquals(StrengthTier.EASY, StrengthTier.fromElo(700))
        assertEquals(StrengthTier.MEDIUM, StrengthTier.fromElo(1200))
        assertEquals(StrengthTier.STRONG, StrengthTier.fromElo(1800))
        assertEquals(StrengthTier.MAX, StrengthTier.fromElo(-1))

        assertNull(ChessEngine.computeNodeCap(-1))
        assertEquals(5100, ChessEngine.computeNodeCap(300))
        assertEquals(7900, ChessEngine.computeNodeCap(700))
        assertEquals(18000, ChessEngine.computeNodeCap(1320))
        assertTrue((ChessEngine.computeNodeCap(2800) ?: 0) in 175_000..177_000)

        assertEquals(350, ChessEngine.computeBaseMoveTimeMs(300))
        assertEquals(1100, ChessEngine.computeBaseMoveTimeMs(-1))
        assertEquals(80, ChessEngine.computeMoveTimeWithClockMs(1800, 600L))

        assertEquals(1.0, ChessEngine.computeStockfishProbability(1320), 0.0001)
        assertEquals(1.0, ChessEngine.computeStockfishProbability(-1), 0.0001)
        assertTrue(ChessEngine.computeStockfishProbability(300) < 0.05)

        val balancedCap = ChessEngine.computePersonalityLossCapCp(StrengthTier.EASY, PersonalityRisk.BALANCED, 100)
        assertEquals(82, balancedCap)
        val aggressiveCap = ChessEngine.computePersonalityLossCapCp(StrengthTier.EASY, PersonalityRisk.AGGRESSIVE, 100)
        assertEquals(119, aggressiveCap)
    }

    @Test
    fun verifyRuqaaDrawRulesAndTimeoutInsufficientMaterial() {
        // 50-move claimable vs 75-move mandatory draw
        val fen50 = "8/8/4k3/8/8/4K3/4Q3/8 w - - 100 60"
        val state50 = ChessBoardState.fromFen(fen50)
        assertNotNull(state50.canClaimDraw())
        assertEquals(GameResult.ONGOING, state50.evaluateGameResult())

        val fen75 = "8/8/4k3/8/8/4K3/4Q3/8 w - - 150 90"
        val state75 = ChessBoardState.fromFen(fen75)
        assertEquals(GameResult.DRAW_SEVENTY_FIVE_MOVE, state75.evaluateGameResult())

        // Timeout when opponent has only King + Knight (cannot mate except weird cases, FIDE treats K+N vs K as insufficient)
        val knVsK = ChessBoardState.fromFen("8/8/4k3/8/4N3/4K3/8/8 b - - 0 40")
        assertEquals(GameResult.DRAW_TIMEOUT_INSUFFICIENT, knVsK.evaluateTimeoutResult(PieceColor.BLACK))

        val kqVsK = ChessBoardState.fromFen("8/8/4k3/8/4Q3/4K3/8/8 b - - 0 40")
        assertEquals(GameResult.BLACK_TIMEOUT, kqVsK.evaluateTimeoutResult(PieceColor.BLACK))
    }

    @Test
    fun verifyCoachRuqaaThresholdsCacheKeyAnd3StageHint() {
        assertEquals("تحليل غير مكتمل", ChessCoachAnalyzer.gradeLabelFromLoss(null))
        assertEquals("جيدة", ChessCoachAnalyzer.gradeLabelFromLoss(0))
        assertEquals("جيدة", ChessCoachAnalyzer.gradeLabelFromLoss(49))
        assertEquals("عدم دقة", ChessCoachAnalyzer.gradeLabelFromLoss(50))
        assertEquals("عدم دقة", ChessCoachAnalyzer.gradeLabelFromLoss(99))
        assertEquals("خطأ", ChessCoachAnalyzer.gradeLabelFromLoss(100))
        assertEquals("خطأ", ChessCoachAnalyzer.gradeLabelFromLoss(199))
        assertEquals("خطأ كبير", ChessCoachAnalyzer.gradeLabelFromLoss(200))

        val k1 = ChessCoachAnalyzer.computeReviewCacheKey("sf19", ChessBoardState.INITIAL_FEN, listOf("e2e4", "e7e5"))
        val k2 = ChessCoachAnalyzer.computeReviewCacheKey("sf19", ChessBoardState.INITIAL_FEN, listOf("e2e4", "e7e5"))
        val k3 = ChessCoachAnalyzer.computeReviewCacheKey("sf19", ChessBoardState.INITIAL_FEN, listOf("d2d4", "d7d5"))
        assertEquals(k1, k2)
        assertNotEquals(k1, k3)

        val hint = ChessCoachAnalyzer.generateProgressiveHint(ChessBoardState.initial())
        assertNotNull(hint)
        assertTrue(hint!!.conceptAr.isNotBlank())
        assertTrue(hint.pieceHintAr.isNotBlank())
        assertTrue(hint.exactMoveAr.isNotBlank())
    }

    @Test
    fun verifySyzygyValidationRules() {
        val validWdlHeader = byteArrayOf(0x71.toByte(), 0xE8.toByte(), 0x23.toByte(), 0x5D.toByte())
        val validDtzHeader = byteArrayOf(0xD7.toByte(), 0x66.toByte(), 0x0C.toByte(), 0xA5.toByte())
        val badHeader = byteArrayOf(0x00, 0x01, 0x02, 0x03)

        assertNull(SyzygyManager.validateMetadataAndMagic("KQvK.rtbw", validWdlHeader, 16L + 64L))
        assertNull(SyzygyManager.validateMetadataAndMagic("KPvKP.rtbz", validDtzHeader, 16L + 128L))
        assertNotNull(SyzygyManager.validateMetadataAndMagic("KQvK.rtbw", badHeader, 16L + 64L))
        assertNotNull(SyzygyManager.validateMetadataAndMagic("KQvK.rtbw", validWdlHeader, 64L)) // % 64 != 16
        assertNotNull(SyzygyManager.validateMetadataAndMagic("INVALID.rtbw", validWdlHeader, 80L))
        assertNotNull(SyzygyManager.validateMetadataAndMagic("KQRBNvKQRBN.rtbw", validWdlHeader, 80L))
    }

    @Test
    fun verifyMoveHistoryGroupingAndEvalFormatting() {
        var state = ChessBoardState.initial()
        val ucis = listOf("e2e4", "e7e5", "g1f3")
        val recorded = mutableListOf<RecordedMove>()

        ucis.forEachIndexed { idx, uci ->
            val legal = state.generateLegalMoves()
            val move = legal.first { it.toUci() == uci }
            val san = state.toSan(move, legal)
            val color = state.turn
            val moveNum = state.fullMoveNumber
            state = state.copyWithMove(move)
            recorded.add(
                RecordedMove(
                    moveNumber = moveNum,
                    color = color,
                    move = move,
                    san = san,
                    fenAfter = state.toFen(),
                    evalWhiteCp = 25 + idx * 10,
                    evalBeforeWhiteCp = 15,
                    centipawnLoss = 0,
                    quality = MoveQuality.BEST,
                    phase = GamePhase.OPENING
                )
            )
        }

        val turns = groupMovesIntoTurns(recorded)
        assertEquals(2, turns.size)
        assertEquals(1, turns[0].turnNumber)
        assertEquals("e4", turns[0].whiteMove?.san)
        assertEquals("e5", turns[0].blackMove?.san)
        assertEquals(0, turns[0].whiteIndex)
        assertEquals(1, turns[0].blackIndex)

        assertEquals(2, turns[1].turnNumber)
        assertEquals("Nf3", turns[1].whiteMove?.san)
        assertNull(turns[1].blackMove)
        assertEquals(2, turns[1].whiteIndex)

        assertEquals("+1.5", formatEvalPawns(150))
        assertEquals("-0.8", formatEvalPawns(-80))
        assertEquals("+M", formatEvalPawns(95000))
    }

    @Test
    fun verifyStockfishUciParsingAndUpgradedEngineTelemetry() = runBlocking {
        val cpInfo = StockfishUciEngine.parseUciInfoLine(
            "info depth 14 seldepth 20 multipv 1 score cp 42 nodes 128450 nps 850000 time 151 pv e2e4 e7e5 g1f3"
        )
        assertNotNull(cpInfo)
        assertEquals(14, cpInfo!!.depth)
        assertEquals(42, cpInfo.scoreCp)
        assertEquals(128450, cpInfo.nodes)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), cpInfo.pv)

        val mateInfo = StockfishUciEngine.parseUciInfoLine(
            "info depth 9 score mate 2 nodes 4500 pv d1h5 g7g6 h5e5"
        )
        assertNotNull(mateInfo)
        assertEquals(2, mateInfo!!.mateIn)
        assertTrue(mateInfo.scoreCp > 30_000)

        val start = ChessBoardState.initial()
        assertEquals(ChessEngine.computeZobristHash(start), ChessEngine.computeZobristHash(ChessBoardState.initial()))

        val scholarsPreMate = ChessBoardState.fromFen("r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        val mateResult = StockfishUciEngine.computeMove(
            context = null,
            state = scholarsPreMate,
            backendPreference = EngineBackendPreference.BUILTIN_ENGINE,
            targetElo = 2600,
            personality = EnginePersonality.CLASSIC,
            humanStyle = false,
            moveTimeMs = 800,
            threads = 2
        )
        assertEquals("h5f7", mateResult.bestMove?.toUci())
        assertTrue(mateResult.nodesSearched > 0)
        assertTrue(mateResult.pvUci.isNotEmpty())
    }
}
