package com.example

import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.ChessCoachAnalyzer
import com.example.chess.ChessEngine
import com.example.chess.GameResult
import com.example.chess.PieceColor
import com.example.chess.PuzzleCategory
import com.example.chess.PuzzleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(5, com.example.chess.PieceStyle.entries.size)
        assertEquals(8, com.example.chess.BoardTheme.entries.size)

        val state = ChessBoardState.initial()
        for (bot in BotPersona.entries) {
            val res = ChessEngine.chooseBotMove(state, bot)
            assertNotNull("Bot ${bot.id} failed to produce move", res.bestMove)
        }
    }
}
