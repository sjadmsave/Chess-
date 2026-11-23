package com.example.chess

import kotlin.math.abs

class ChessBoardState private constructor(
    val board: Array<ChessPiece?>,
    val turn: PieceColor,
    val castling: CastlingRights,
    val enPassantTarget: Square?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
    val positionHistory: Map<String, Int>
) {
    fun pieceAt(row: Int, col: Int): ChessPiece? = board[row * 8 + col]
    fun pieceAt(sq: Square): ChessPiece? = board[sq.row * 8 + sq.col]

    fun positionKey(): String {
        val sb = StringBuilder(72)
        for (r in 0..7) {
            var empty = 0
            for (c in 0..7) {
                val p = board[r * 8 + c]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.toFenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        sb.append(' ').append(if (turn == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ').append(castling.toFen())
        sb.append(' ').append(enPassantTarget?.toAlgebraic() ?: "-")
        return sb.toString()
    }

    fun toFen(): String = "${positionKey()} $halfMoveClock $fullMoveNumber"

    fun copyWithMove(move: ChessMove): ChessBoardState {
        val newBoard = board.copyOf()
        val movingPiece = newBoard[move.from.index]
            ?: error("No piece at ${move.from.toAlgebraic()}")

        // Clear source
        newBoard[move.from.index] = null

        // Handle en passant capture
        if (move.isEnPassant) {
            val capturedPawnRow = move.from.row
            newBoard[capturedPawnRow * 8 + move.to.col] = null
        }

        // Place moving or promoted piece
        val placedPiece = if (move.promotion != null) {
            ChessPiece(move.promotion, movingPiece.color)
        } else {
            movingPiece
        }
        newBoard[move.to.index] = placedPiece

        // Handle castling rook movement
        if (move.isCastleKingside) {
            val row = move.from.row
            newBoard[row * 8 + 5] = newBoard[row * 8 + 7]
            newBoard[row * 8 + 7] = null
        } else if (move.isCastleQueenside) {
            val row = move.from.row
            newBoard[row * 8 + 3] = newBoard[row * 8 + 0]
            newBoard[row * 8 + 0] = null
        }

        // Update castling rights
        var wk = castling.whiteKingside
        var wq = castling.whiteQueenside
        var bk = castling.blackKingside
        var bq = castling.blackQueenside

        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) {
                wk = false; wq = false
            } else {
                bk = false; bq = false
            }
        }
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) wk = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) wq = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) bk = false
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) bq = false

        // Update en passant square
        val newEp = if (movingPiece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            Square((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        val isCaptureOrPawn = movingPiece.type == PieceType.PAWN || move.capturedPiece != null || move.isEnPassant
        val newHalfMove = if (isCaptureOrPawn) 0 else halfMoveClock + 1
        val newFullMove = if (turn == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber

        val tempState = ChessBoardState(
            board = newBoard,
            turn = turn.opposite(),
            castling = CastlingRights(wk, wq, bk, bq),
            enPassantTarget = newEp,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            positionHistory = positionHistory
        )
        val key = tempState.positionKey()
        val updatedHistory = positionHistory.toMutableMap()
        updatedHistory[key] = (updatedHistory[key] ?: 0) + 1

        return ChessBoardState(
            board = newBoard,
            turn = turn.opposite(),
            castling = CastlingRights(wk, wq, bk, bq),
            enPassantTarget = newEp,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            positionHistory = updatedHistory
        )
    }

    fun findKing(color: PieceColor): Square? {
        for (i in 0..63) {
            val p = board[i]
            if (p != null && p.color == color && p.type == PieceType.KING) {
                return Square.fromIndex(i)
            }
        }
        return null
    }

    fun isSquareAttacked(sq: Square, byColor: PieceColor): Boolean {
        val r = sq.row
        val c = sq.col

        // Pawn attacks
        val pawnSourceRow = if (byColor == PieceColor.WHITE) r + 1 else r - 1
        if (pawnSourceRow in 0..7) {
            if (c - 1 >= 0) {
                val p = pieceAt(pawnSourceRow, c - 1)
                if (p?.color == byColor && p.type == PieceType.PAWN) return true
            }
            if (c + 1 <= 7) {
                val p = pieceAt(pawnSourceRow, c + 1)
                if (p?.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // Knight attacks
        for (d in KNIGHT_DELTAS) {
            val nr = r + d[0]
            val nc = c + d[1]
            if (nr in 0..7 && nc in 0..7) {
                val p = pieceAt(nr, nc)
                if (p?.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0..7 && nc in 0..7) {
                    val p = pieceAt(nr, nc)
                    if (p?.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        // Orthogonal rays (Rook / Queen)
        for (d in ORTHO_DIRS) {
            var nr = r + d[0]
            var nc = c + d[1]
            while (nr in 0..7 && nc in 0..7) {
                val p = pieceAt(nr, nc)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                nr += d[0]
                nc += d[1]
            }
        }

        // Diagonal rays (Bishop / Queen)
        for (d in DIAG_DIRS) {
            var nr = r + d[0]
            var nc = c + d[1]
            while (nr in 0..7 && nc in 0..7) {
                val p = pieceAt(nr, nc)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                nr += d[0]
                nc += d[1]
            }
        }

        return false
    }

    fun isInCheck(color: PieceColor = turn): Boolean {
        val kingSq = findKing(color) ?: return false
        return isSquareAttacked(kingSq, color.opposite())
    }

    fun generateLegalMoves(): List<ChessMove> {
        val pseudo = generatePseudoLegalMoves(turn)
        val legal = ArrayList<ChessMove>(pseudo.size)
        for (move in pseudo) {
            val next = copyWithMoveFast(move)
            if (!next.isInCheck(turn)) {
                legal.add(move)
            }
        }
        return legal
    }

    // Fast copy without rebuilding positionHistory map — used inside engine search
    fun copyWithMoveFast(move: ChessMove): ChessBoardState {
        val newBoard = board.copyOf()
        val movingPiece = newBoard[move.from.index]!!
        newBoard[move.from.index] = null

        if (move.isEnPassant) {
            newBoard[move.from.row * 8 + move.to.col] = null
        }

        newBoard[move.to.index] = if (move.promotion != null) {
            ChessPiece(move.promotion, movingPiece.color)
        } else {
            movingPiece
        }

        if (move.isCastleKingside) {
            val row = move.from.row
            newBoard[row * 8 + 5] = newBoard[row * 8 + 7]
            newBoard[row * 8 + 7] = null
        } else if (move.isCastleQueenside) {
            val row = move.from.row
            newBoard[row * 8 + 3] = newBoard[row * 8 + 0]
            newBoard[row * 8 + 0] = null
        }

        var wk = castling.whiteKingside
        var wq = castling.whiteQueenside
        var bk = castling.blackKingside
        var bq = castling.blackQueenside
        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) { wk = false; wq = false }
            else { bk = false; bq = false }
        }
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) wk = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) wq = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) bk = false
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) bq = false

        val newEp = if (movingPiece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            Square((move.from.row + move.to.row) / 2, move.from.col)
        } else null

        val isCaptureOrPawn = movingPiece.type == PieceType.PAWN || move.capturedPiece != null || move.isEnPassant
        val newHalfMove = if (isCaptureOrPawn) 0 else halfMoveClock + 1
        val newFullMove = if (turn == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber

        return ChessBoardState(
            board = newBoard,
            turn = turn.opposite(),
            castling = CastlingRights(wk, wq, bk, bq),
            enPassantTarget = newEp,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            positionHistory = positionHistory
        )
    }

    fun copyWithNullMove(): ChessBoardState = ChessBoardState(
        board = board,
        turn = turn.opposite(),
        castling = castling,
        enPassantTarget = null,
        halfMoveClock = halfMoveClock + 1,
        fullMoveNumber = if (turn == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber,
        positionHistory = positionHistory
    )

    fun generatePseudoLegalMoves(color: PieceColor): List<ChessMove> {
        val moves = ArrayList<ChessMove>(40)
        for (idx in 0..63) {
            val piece = board[idx] ?: continue
            if (piece.color != color) continue
            val r = idx / 8
            val c = idx % 8
            val from = Square(r, c)

            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(from, color, moves)
                PieceType.KNIGHT -> {
                    for (d in KNIGHT_DELTAS) {
                        val nr = r + d[0]; val nc = c + d[1]
                        if (nr in 0..7 && nc in 0..7) {
                            val target = pieceAt(nr, nc)
                            if (target == null || target.color != color) {
                                moves.add(ChessMove(from, Square(nr, nc), capturedPiece = target))
                            }
                        }
                    }
                }
                PieceType.BISHOP -> generateSlidingMoves(from, color, DIAG_DIRS, moves)
                PieceType.ROOK -> generateSlidingMoves(from, color, ORTHO_DIRS, moves)
                PieceType.QUEEN -> {
                    generateSlidingMoves(from, color, DIAG_DIRS, moves)
                    generateSlidingMoves(from, color, ORTHO_DIRS, moves)
                }
                PieceType.KING -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val nr = r + dr; val nc = c + dc
                            if (nr in 0..7 && nc in 0..7) {
                                val target = pieceAt(nr, nc)
                                if (target == null || target.color != color) {
                                    moves.add(ChessMove(from, Square(nr, nc), capturedPiece = target))
                                }
                            }
                        }
                    }
                    generateCastlingMoves(from, color, moves)
                }
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Square, color: PieceColor, out: MutableList<ChessMove>) {
        val dir = if (color == PieceColor.WHITE) -1 else 1
        val startRow = if (color == PieceColor.WHITE) 6 else 1
        val promoRow = if (color == PieceColor.WHITE) 0 else 7
        val nextRow = from.row + dir

        if (nextRow in 0..7) {
            // Single push
            if (pieceAt(nextRow, from.col) == null) {
                val to = Square(nextRow, from.col)
                if (nextRow == promoRow) {
                    addPromotions(from, to, null, out)
                } else {
                    out.add(ChessMove(from, to))
                    // Double push
                    if (from.row == startRow) {
                        val doubleRow = from.row + dir * 2
                        if (pieceAt(doubleRow, from.col) == null) {
                            out.add(ChessMove(from, Square(doubleRow, from.col)))
                        }
                    }
                }
            }
            // Captures
            for (dc in intArrayOf(-1, 1)) {
                val nc = from.col + dc
                if (nc in 0..7) {
                    val to = Square(nextRow, nc)
                    val target = pieceAt(nextRow, nc)
                    if (target != null && target.color != color) {
                        if (nextRow == promoRow) {
                            addPromotions(from, to, target, out)
                        } else {
                            out.add(ChessMove(from, to, capturedPiece = target))
                        }
                    } else if (enPassantTarget != null && enPassantTarget == to) {
                        val epPawn = pieceAt(from.row, nc)
                        if (epPawn != null && epPawn.color != color && epPawn.type == PieceType.PAWN) {
                            out.add(ChessMove(from, to, isEnPassant = true, capturedPiece = epPawn))
                        }
                    }
                }
            }
        }
    }

    private fun addPromotions(from: Square, to: Square, captured: ChessPiece?, out: MutableList<ChessMove>) {
        out.add(ChessMove(from, to, promotion = PieceType.QUEEN, capturedPiece = captured))
        out.add(ChessMove(from, to, promotion = PieceType.ROOK, capturedPiece = captured))
        out.add(ChessMove(from, to, promotion = PieceType.BISHOP, capturedPiece = captured))
        out.add(ChessMove(from, to, promotion = PieceType.KNIGHT, capturedPiece = captured))
    }

    private fun generateSlidingMoves(
        from: Square,
        color: PieceColor,
        dirs: Array<IntArray>,
        out: MutableList<ChessMove>
    ) {
        for (d in dirs) {
            var nr = from.row + d[0]
            var nc = from.col + d[1]
            while (nr in 0..7 && nc in 0..7) {
                val target = pieceAt(nr, nc)
                if (target == null) {
                    out.add(ChessMove(from, Square(nr, nc)))
                } else {
                    if (target.color != color) {
                        out.add(ChessMove(from, Square(nr, nc), capturedPiece = target))
                    }
                    break
                }
                nr += d[0]
                nc += d[1]
            }
        }
    }

    private fun generateCastlingMoves(from: Square, color: PieceColor, out: MutableList<ChessMove>) {
        val row = if (color == PieceColor.WHITE) 7 else 0
        if (from.row != row || from.col != 4) return
        val opp = color.opposite()
        if (isSquareAttacked(from, opp)) return

        val canKing = if (color == PieceColor.WHITE) castling.whiteKingside else castling.blackKingside
        if (canKing) {
            val rook = pieceAt(row, 7)
            if (rook?.color == color && rook.type == PieceType.ROOK &&
                pieceAt(row, 5) == null && pieceAt(row, 6) == null &&
                !isSquareAttacked(Square(row, 5), opp) &&
                !isSquareAttacked(Square(row, 6), opp)
            ) {
                out.add(ChessMove(from, Square(row, 6), isCastleKingside = true))
            }
        }

        val canQueen = if (color == PieceColor.WHITE) castling.whiteQueenside else castling.blackQueenside
        if (canQueen) {
            val rook = pieceAt(row, 0)
            if (rook?.color == color && rook.type == PieceType.ROOK &&
                pieceAt(row, 1) == null && pieceAt(row, 2) == null && pieceAt(row, 3) == null &&
                !isSquareAttacked(Square(row, 3), opp) &&
                !isSquareAttacked(Square(row, 2), opp)
            ) {
                out.add(ChessMove(from, Square(row, 2), isCastleQueenside = true))
            }
        }
    }

    fun evaluateGameResult(legalMoves: List<ChessMove> = generateLegalMoves()): GameResult {
        if (legalMoves.isEmpty()) {
            return if (isInCheck(turn)) {
                if (turn == PieceColor.WHITE) GameResult.BLACK_WINS_CHECKMATE else GameResult.WHITE_WINS_CHECKMATE
            } else {
                GameResult.DRAW_STALEMATE
            }
        }
        if (halfMoveClock >= 150) return GameResult.DRAW_SEVENTY_FIVE_MOVE
        val key = positionKey()
        if ((positionHistory[key] ?: 0) >= 5) return GameResult.DRAW_FIVE_FOLD
        if (hasInsufficientMaterial()) return GameResult.DRAW_INSUFFICIENT_MATERIAL
        return GameResult.ONGOING
    }

    /**
     * Claimable draws per Ruqaa Section 9.2:
     * 3-fold repetition or 50-move rule (100 half-moves).
     */
    fun canClaimDraw(): String? {
        val key = positionKey()
        val reps = positionHistory[key] ?: 0
        return when {
            reps >= 3 -> "يمكن طلب التعادل (تكرار الوضعية ٣ مرات)"
            halfMoveClock >= 100 -> "يمكن طلب التعادل (قاعدة ٥٠ نقلة)"
            else -> null
        }
    }

    /**
     * Ruqaa Section 9.2: On clock expiry, if the non-timed-out side cannot deliver checkmate
     * due to insufficient material, award DRAW_TIMEOUT_INSUFFICIENT.
     */
    fun evaluateTimeoutResult(timedOutColor: PieceColor): GameResult {
        val winner = timedOutColor.opposite()
        return if (hasInsufficientMatingMaterialFor(winner)) {
            GameResult.DRAW_TIMEOUT_INSUFFICIENT
        } else if (timedOutColor == PieceColor.WHITE) {
            GameResult.WHITE_TIMEOUT
        } else {
            GameResult.BLACK_TIMEOUT
        }
    }

    fun hasInsufficientMatingMaterialFor(color: PieceColor): Boolean {
        var pawnsRooksQueens = 0
        var knights = 0
        var bishops = 0
        for (i in 0..63) {
            val p = board[i] ?: continue
            if (p.color != color || p.type == PieceType.KING) continue
            when (p.type) {
                PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> pawnsRooksQueens++
                PieceType.KNIGHT -> knights++
                PieceType.BISHOP -> bishops++
                PieceType.KING -> {}
            }
        }
        if (pawnsRooksQueens > 0) return false
        return (knights + bishops) <= 1
    }

    fun hasInsufficientMaterial(): Boolean {
        val whitePieces = mutableListOf<Pair<PieceType, Square>>()
        val blackPieces = mutableListOf<Pair<PieceType, Square>>()
        for (i in 0..63) {
            val p = board[i] ?: continue
            if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) {
                return false
            }
            if (p.type != PieceType.KING) {
                val sq = Square.fromIndex(i)
                if (p.color == PieceColor.WHITE) whitePieces.add(p.type to sq)
                else blackPieces.add(p.type to sq)
            }
        }
        if (whitePieces.isEmpty() && blackPieces.isEmpty()) return true
        if (whitePieces.size == 1 && blackPieces.isEmpty()) return true
        if (blackPieces.size == 1 && whitePieces.isEmpty()) return true
        if (whitePieces.size == 1 && blackPieces.size == 1 &&
            whitePieces[0].first == PieceType.BISHOP && blackPieces[0].first == PieceType.BISHOP
        ) {
            val wColor = (whitePieces[0].second.row + whitePieces[0].second.col) % 2
            val bColor = (blackPieces[0].second.row + blackPieces[0].second.col) % 2
            return wColor == bColor
        }
        return false
    }

    fun toSan(move: ChessMove, legalMoves: List<ChessMove> = generateLegalMoves()): String {
        val next = copyWithMoveFast(move)
        val nextLegal = next.generateLegalMoves()
        val inCheck = next.isInCheck(next.turn)
        val suffix = when {
            inCheck && nextLegal.isEmpty() -> "#"
            inCheck -> "+"
            else -> ""
        }

        if (move.isCastleKingside) return "O-O$suffix"
        if (move.isCastleQueenside) return "O-O-O$suffix"

        val piece = pieceAt(move.from) ?: return move.toUci()
        val sb = StringBuilder()

        if (piece.type == PieceType.PAWN) {
            if (move.capturedPiece != null || move.isEnPassant) {
                sb.append('a' + move.from.col).append('x')
            }
            sb.append(move.to.toAlgebraic())
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanLetter)
            }
        } else {
            sb.append(piece.type.sanLetter)
            // Disambiguation
            val ambiguous = legalMoves.filter {
                it.to == move.to && it.from != move.from &&
                    pieceAt(it.from)?.type == piece.type
            }
            if (ambiguous.isNotEmpty()) {
                val sameCol = ambiguous.any { it.from.col == move.from.col }
                val sameRow = ambiguous.any { it.from.row == move.from.row }
                if (!sameCol) {
                    sb.append('a' + move.from.col)
                } else if (!sameRow) {
                    sb.append(8 - move.from.row)
                } else {
                    sb.append(move.from.toAlgebraic())
                }
            }
            if (move.capturedPiece != null) sb.append('x')
            sb.append(move.to.toAlgebraic())
        }
        sb.append(suffix)
        return sb.toString()
    }

    fun detectPhase(): GamePhase {
        var nonPawnMaterial = 0
        var queenCount = 0
        for (i in 0..63) {
            val p = board[i] ?: continue
            if (p.type != PieceType.KING && p.type != PieceType.PAWN) {
                nonPawnMaterial += p.type.valueCp
                if (p.type == PieceType.QUEEN) queenCount++
            }
        }
        return when {
            fullMoveNumber <= 9 && nonPawnMaterial >= 4800 -> GamePhase.OPENING
            nonPawnMaterial <= 2600 || (queenCount == 0 && nonPawnMaterial <= 3400) -> GamePhase.ENDGAME
            else -> GamePhase.MIDDLEGAME
        }
    }

    fun materialBalanceWhite(): Int {
        var score = 0
        for (i in 0..63) {
            val p = board[i] ?: continue
            if (p.type == PieceType.KING) continue
            val v = p.type.valueCp / 100
            score += if (p.color == PieceColor.WHITE) v else -v
        }
        return score
    }

    fun capturedPiecesFromStart(): Pair<List<ChessPiece>, List<ChessPiece>> {
        val currentCounts = mutableMapOf<Pair<PieceColor, PieceType>, Int>()
        for (p in board) {
            if (p != null) {
                val key = p.color to p.type
                currentCounts[key] = (currentCounts[key] ?: 0) + 1
            }
        }
        val standardCounts = mapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val capturedByWhite = mutableListOf<ChessPiece>() // black pieces missing
        val capturedByBlack = mutableListOf<ChessPiece>() // white pieces missing
        for ((type, stdCount) in standardCounts) {
            val blackLeft = currentCounts[PieceColor.BLACK to type] ?: 0
            repeat((stdCount - blackLeft).coerceAtLeast(0)) {
                capturedByWhite.add(ChessPiece(type, PieceColor.BLACK))
            }
            val whiteLeft = currentCounts[PieceColor.WHITE to type] ?: 0
            repeat((stdCount - whiteLeft).coerceAtLeast(0)) {
                capturedByBlack.add(ChessPiece(type, PieceColor.WHITE))
            }
        }
        return capturedByWhite to capturedByBlack
    }

    companion object {
        const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        private val KNIGHT_DELTAS = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1)
        )
        private val ORTHO_DIRS = arrayOf(
            intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)
        )
        private val DIAG_DIRS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1)
        )

        fun fromFen(fen: String): ChessBoardState {
            val parts = fen.trim().split(Regex("\\s+"))
            val board = arrayOfNulls<ChessPiece>(64)
            val rows = parts[0].split('/')
            for (r in 0..7) {
                val rowStr = rows.getOrElse(r) { "8" }
                var c = 0
                for (ch in rowStr) {
                    if (ch.isDigit()) {
                        c += ch - '0'
                    } else if (c in 0..7) {
                        board[r * 8 + c] = ChessPiece.fromFenChar(ch)
                        c++
                    }
                }
            }
            val turn = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
            val castling = CastlingRights.fromFen(parts.getOrNull(2) ?: "-")
            val ep = parts.getOrNull(3)?.let { if (it == "-") null else Square.fromAlgebraic(it) }
            val halfMove = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val fullMove = parts.getOrNull(5)?.toIntOrNull() ?: 1

            val initial = ChessBoardState(
                board = board,
                turn = turn,
                castling = castling,
                enPassantTarget = ep,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove,
                positionHistory = emptyMap()
            )
            val key = initial.positionKey()
            return ChessBoardState(
                board = board,
                turn = turn,
                castling = castling,
                enPassantTarget = ep,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove,
                positionHistory = mapOf(key to 1)
            )
        }

        fun initial(): ChessBoardState = fromFen(INITIAL_FEN)

        fun recognizeOpeningArabic(uciMoves: List<String>): String {
            val line = uciMoves.take(6).joinToString(" ")
            return when {
                line.startsWith("e2e4 c7c5") -> "الدفاع الصقلي (Sicilian Defense)"
                line.startsWith("e2e4 e7e5 g1f3 b8c6 f1b5") -> "افتتاح روي لوبيز (Ruy Lopez)"
                line.startsWith("e2e4 e7e5 g1f3 b8c6 f1c4") -> "الافتتاح الإيطالي (Italian Game)"
                line.startsWith("e2e4 e7e5 g1f3 g8f6") -> "دفاع بتروف (Petrov Defense)"
                line.startsWith("e2e4 e7e6") -> "الدفاع الفرنسي (French Defense)"
                line.startsWith("e2e4 c7c6") -> "دفاع كارو-كان (Caro-Kann)"
                line.startsWith("e2e4 d7d5") -> "الدفاع الإسكندنافي (Scandinavian)"
                line.startsWith("d2d4 d7d5 c2c4") -> "مناورة الملكة (Queen's Gambit)"
                line.startsWith("d2d4 g8f6 c2c4 g7g6") -> "الدفاع الهندي للملك (King's Indian)"
                line.startsWith("d2d4 g8f6 c2c4 e7e6") -> "الدفاع الهندي للوزير / نيمزو"
                line.startsWith("c2c4") -> "الافتتاح الإنجليزي (English Opening)"
                line.startsWith("g1f3") -> "افتتاح ريتي (Réti Opening)"
                line.startsWith("e2e4 e7e5") -> "الافتتاح المفتوح الكلاسيكي"
                line.startsWith("d2d4 d7d5") -> "افتتاح بيدق الوزير المغلق"
                else -> "افتتاح حر وتطوير متوازن"
            }
        }
    }
}
