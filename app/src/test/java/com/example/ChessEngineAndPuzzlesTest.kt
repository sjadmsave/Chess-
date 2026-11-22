package com.example

import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.ChessCoachAnalyzer
import com.example.chess.ChessEngine
import com.example.chess.GamePhase
import com.example.chess.GameResult
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PuzzleCategory
import com.example.chess.PuzzleRepository
import com.example.chess.RecordedMove
import com.example.chess.TimeControl
import com.example.ui.components.formatEvalPawns
import com.example.ui.components.groupMovesIntoTurns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineAndPuzzlesTest {

    @Test
    fun verifyAllPuzzleSolutionsAreLegalAndMatesDeliverCheckmate() {
        for (puzzle in PuzzleRepository.puzzles) {
            var state = ChessBoardState.fromFen(puzzle.fen)
            assertTrue("Puzzle ${puzzle.id} has empty solution", puzzle.solutionUci.isNotEmpty())
            for (uci in puzzle.solutionUci) {
                val legal = state.generateLegalMoves()
                val move = legal.find { it.toUci() == uci }
                assertNotNull("Puzzle ${puzzle.id} illegal move $uci at FEN ${state.toFen()}", move)
                state = state.copyWithMove(move!!)
            }
            if (puzzle.category == PuzzleCategory.MATE_IN_1 || puzzle.category == PuzzleCategory.MATE_IN_2) {
                val result = state.evaluateGameResult()
                assertEquals(
                    "Puzzle ${puzzle.id} did not end in checkmate (got $result)",
                    GameResult.WHITE_WINS_CHECKMATE,
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
        assertEquals(6, com.example.chess.PieceStyle.entries.size)
        assertEquals(8, com.example.chess.BoardTheme.entries.size)
        assertEquals(4, TimeControl.entries.size)
        assertEquals(10, com.example.chess.EnginePersonality.entries.size)

        val state = ChessBoardState.initial()
        for (bot in BotPersona.entries) {
            val res = ChessEngine.chooseBotMove(state, bot)
            assertNotNull("Bot ${bot.id} failed to produce move", res.bestMove)
        }
    }

    @Test
    fun verifyPgnFenCodecAndCustomEnginePersonalities() {
        val samplePgn = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6"
        val parsed = com.example.chess.PgnFenCodec.importAutoDetect(samplePgn)
        assertTrue(parsed.isSuccess)
        val data = parsed.getOrNull()!!
        assertEquals(6, data.moves.size)
        assertEquals("e4", data.moves[0].san)

        val fenStr = data.finalBoard.toFen()
        val parsedFen = com.example.chess.PgnFenCodec.parseFenStrict(fenStr)
        assertTrue(parsedFen.isSuccess)

        val exportedPgn = com.example.chess.PgnFenCodec.exportPgn(data.moves)
        assertTrue(exportedPgn.contains("1. e4 e5"))

        for (personality in com.example.chess.EnginePersonality.entries) {
            val moveRes = ChessEngine.chooseCustomEngineMove(
                state = data.finalBoard,
                targetElo = 1400,
                personality = personality,
                humanStyle = true
            )
            assertNotNull("Personality ${personality.id} failed to produce move", moveRes.bestMove)
        }
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
    fun verifyStockfishUciParsingAndUpgradedEngineTelemetry() = kotlinx.coroutines.runBlocking {
        val cpInfo = com.example.chess.StockfishUciEngine.parseUciInfoLine(
            "info depth 14 seldepth 20 multipv 1 score cp 42 nodes 128450 nps 850000 time 151 pv e2e4 e7e5 g1f3"
        )
        assertNotNull(cpInfo)
        assertEquals(14, cpInfo!!.depth)
        assertEquals(42, cpInfo.scoreCp)
        assertEquals(128450, cpInfo.nodes)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), cpInfo.pv)

        val mateInfo = com.example.chess.StockfishUciEngine.parseUciInfoLine(
            "info depth 9 score mate 2 nodes 4500 pv d1h5 g7g6 h5e5"
        )
        assertNotNull(mateInfo)
        assertTrue(mateInfo!!.scoreCp > 90_000)

        // Zobrist determinism
        val start = ChessBoardState.initial()
        assertEquals(ChessEngine.computeZobristHash(start), ChessEngine.computeZobristHash(ChessBoardState.initial()))

        // Mate-in-1 detection with upgraded search (White to move, Qh5 + Bc4 delivers Qf7#)
        val scholarsPreMate = ChessBoardState.fromFen("r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        val mateResult = com.example.chess.StockfishUciEngine.computeMove(
            context = null,
            state = scholarsPreMate,
            backendPreference = com.example.chess.EngineBackendPreference.BUILTIN_ENGINE,
            targetElo = 2600,
            personality = com.example.chess.EnginePersonality.TRADITIONAL,
            humanStyle = false,
            moveTimeMs = 800,
            threads = 2
        )
        assertEquals("h5f7", mateResult.bestMove?.toUci())
        assertTrue(mateResult.nodesSearched > 0)
        assertTrue(mateResult.pvUci.isNotEmpty())
    }
}
