package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.chess.ChessMove
import com.example.chess.GamePhase
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.RecordedMove
import com.example.chess.Square

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val botId: String,
    val playerColor: String, // "WHITE" or "BLACK"
    val currentFen: String,
    val movesEncoded: String,
    val moveCount: Int,
    val resultName: String,
    val isCompleted: Boolean,
    val playerAccuracy: Int,
    val opponentAccuracy: Int,
    val acpl: Int,
    val openingNameAr: String,
    val openingAccuracy: Int,
    val middlegameAccuracy: Int,
    val endgameAccuracy: Int,
    val eloDelta: Int,
    val updatedAt: Long
)

@Entity(tableName = "rating_points")
data class RatingPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val elo: Int,
    val accuracy: Int,
    val openingAcc: Int,
    val middlegameAcc: Int,
    val endgameAcc: Int,
    val blunders: Int,
    val mistakes: Int,
    val won: Boolean,
    val botId: String
)

@Entity(tableName = "puzzle_records")
data class PuzzleRecordEntity(
    @PrimaryKey val puzzleId: String,
    val solvedAt: Long,
    val usedHint: Boolean,
    val attempts: Int
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long
)

object MoveCodec {
    private const val FIELD_SEP = "\u001F"
    private const val ROW_SEP = "\u001E"

    fun encode(moves: List<RecordedMove>): String {
        if (moves.isEmpty()) return ""
        return moves.joinToString(ROW_SEP) { m ->
            listOf(
                m.moveNumber.toString(),
                m.color.name,
                m.move.toUci(),
                if (m.move.isCastleKingside) "1" else "0",
                if (m.move.isCastleQueenside) "1" else "0",
                if (m.move.isEnPassant) "1" else "0",
                m.move.capturedPiece?.toFenChar()?.toString() ?: "",
                m.san,
                m.fenAfter,
                m.evalWhiteCp.toString(),
                m.evalBeforeWhiteCp.toString(),
                m.centipawnLoss.toString(),
                m.quality.name,
                m.phase.name,
                m.coachNoteAr.replace(FIELD_SEP, " ").replace(ROW_SEP, " "),
                m.bestMoveSan.replace(FIELD_SEP, " ").replace(ROW_SEP, " ")
            ).joinToString(FIELD_SEP)
        }
    }

    fun decode(raw: String): List<RecordedMove> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ROW_SEP).mapNotNull { row ->
            val p = row.split(FIELD_SEP)
            if (p.size < 16) return@mapNotNull null
            val uci = p[2]
            if (uci.length < 4) return@mapNotNull null
            val from = Square.fromAlgebraic(uci.substring(0, 2)) ?: return@mapNotNull null
            val to = Square.fromAlgebraic(uci.substring(2, 4)) ?: return@mapNotNull null
            val promo = if (uci.length >= 5) {
                when (uci[4].uppercaseChar()) {
                    'Q' -> PieceType.QUEEN
                    'R' -> PieceType.ROOK
                    'B' -> PieceType.BISHOP
                    'N' -> PieceType.KNIGHT
                    else -> null
                }
            } else null
            val captured = p[6].firstOrNull()?.let { com.example.chess.ChessPiece.fromFenChar(it) }
            val move = ChessMove(
                from = from,
                to = to,
                promotion = promo,
                isCastleKingside = p[3] == "1",
                isCastleQueenside = p[4] == "1",
                isEnPassant = p[5] == "1",
                capturedPiece = captured
            )
            RecordedMove(
                moveNumber = p[0].toIntOrNull() ?: 1,
                color = runCatching { PieceColor.valueOf(p[1]) }.getOrDefault(PieceColor.WHITE),
                move = move,
                san = p[7],
                fenAfter = p[8],
                evalWhiteCp = p[9].toIntOrNull() ?: 0,
                evalBeforeWhiteCp = p[10].toIntOrNull() ?: 0,
                centipawnLoss = p[11].toIntOrNull() ?: 0,
                quality = runCatching { MoveQuality.valueOf(p[12]) }.getOrDefault(MoveQuality.GOOD),
                phase = runCatching { GamePhase.valueOf(p[13]) }.getOrDefault(GamePhase.MIDDLEGAME),
                coachNoteAr = p[14],
                bestMoveSan = p[15]
            )
        }
    }
}
