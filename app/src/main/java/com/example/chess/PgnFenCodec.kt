package com.example.chess

data class ParsedGameData(
    val startFen: String,
    val finalBoard: ChessBoardState,
    val moves: List<RecordedMove>,
    val openingNameAr: String
)

object PgnFenCodec {

    fun parseFenStrict(rawFen: String): Result<ChessBoardState> {
        val trimmed = rawFen.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("وضعية FEN غير صالحة"))
        }
        val parts = trimmed.split(Regex("\\s+"))
        val ranks = parts[0].split('/')
        if (ranks.size != 8) {
            return Result.failure(IllegalArgumentException("وضعية FEN غير صالحة: يجب أن تحتوي على 8 صفوف"))
        }
        for ((idx, rank) in ranks.withIndex()) {
            var count = 0
            for (ch in rank) {
                when {
                    ch in '1'..'8' -> count += ch - '0'
                    ChessPiece.fromFenChar(ch) != null -> count += 1
                    else -> return Result.failure(IllegalArgumentException("وضعية FEN غير صالحة في الصف ${idx + 1}: $ch"))
                }
            }
            if (count != 8) {
                return Result.failure(IllegalArgumentException("وضعية FEN غير صالحة في الصف ${idx + 1}"))
            }
        }
        val state = ChessBoardState.fromFen(trimmed)
        if (state.findKing(PieceColor.WHITE) == null || state.findKing(PieceColor.BLACK) == null) {
            return Result.failure(IllegalArgumentException("وضعية FEN غير صالحة: يجب وجود الملكين"))
        }
        return Result.success(state)
    }

    fun parsePgn(rawPgn: String): Result<ParsedGameData> {
        val text = rawPgn.trim()
        if (text.isEmpty()) {
            return Result.failure(IllegalArgumentException("نص PGN فارغ"))
        }

        // Extract optional [FEN "..."] header
        val fenHeaderMatch = Regex("""\[FEN\s+"([^"]+)"]""").find(text)
        val startFen = fenHeaderMatch?.groupValues?.get(1) ?: ChessBoardState.INITIAL_FEN
        var state = parseFenStrict(startFen).getOrElse { return Result.failure(it) }

        // Remove headers [..], comments {..}, variations (..), NAGs $1
        var body = text
            .replace(Regex("""\[[^\]]*]"""), " ")
            .replace(Regex("""\{[^}]*}"""), " ")
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\$\d+"""), " ")

        val rawTokens = body.split(Regex("\\s+")).filter { it.isNotBlank() }
        val resultTokens = setOf("1-0", "0-1", "1/2-1/2", "*")
        val recorded = mutableListOf<RecordedMove>()

        for (raw in rawTokens) {
            if (raw in resultTokens) break
            // Strip leading move numbers like "1.", "12...", "3.e4"
            val token = raw.replace(Regex("""^\d+\.+"""), "").trim()
            if (token.isBlank() || token == "..." || token in resultTokens) continue

            val legal = state.generateLegalMoves()
            val normalizedTarget = normalizeSan(token)
            val matchedMove = legal.find { m ->
                normalizeSan(state.toSan(m, legal)) == normalizedTarget ||
                    m.toUci().equals(token, ignoreCase = true)
            } ?: return Result.failure(
                IllegalArgumentException("نقلة غير قانونية أو غير مفهومة: $token")
            )

            val moveColor = state.turn
            val moveNum = state.fullMoveNumber
            val san = state.toSan(matchedMove, legal)
            val phase = state.detectPhase()
            state = state.copyWithMove(matchedMove)
            val eval = ChessEngine.evaluateWhitePerspective(state)

            recorded.add(
                RecordedMove(
                    moveNumber = moveNum,
                    color = moveColor,
                    move = matchedMove,
                    san = san,
                    fenAfter = state.toFen(),
                    evalWhiteCp = eval,
                    evalBeforeWhiteCp = recorded.lastOrNull()?.evalWhiteCp ?: 15,
                    centipawnLoss = 0,
                    quality = MoveQuality.GOOD,
                    phase = phase
                )
            )
        }

        if (recorded.isEmpty() && fenHeaderMatch == null) {
            return Result.failure(IllegalArgumentException("لم يتم العثور على نقلات صالحة في PGN"))
        }

        val opening = ChessBoardState.recognizeOpeningArabic(recorded.map { it.move.toUci() })
        return Result.success(
            ParsedGameData(
                startFen = startFen,
                finalBoard = state,
                moves = recorded,
                openingNameAr = opening
            )
        )
    }

    fun importAutoDetect(input: String): Result<ParsedGameData> {
        val trimmed = input.trim()
        // If it looks like a FEN string (has '/' 7 times in first token)
        val firstToken = trimmed.split(Regex("\\s+")).firstOrNull() ?: ""
        if (firstToken.count { it == '/' } == 7 && !trimmed.contains("[")) {
            return parseFenStrict(trimmed).map { board ->
                ParsedGameData(
                    startFen = board.toFen(),
                    finalBoard = board,
                    moves = emptyList(),
                    openingNameAr = "وضعية FEN مخصصة"
                )
            }
        }
        return parsePgn(trimmed)
    }

    fun exportPgn(moves: List<RecordedMove>, whiteName: String = "Player", blackName: String = "Engine"): String {
        val sb = StringBuilder()
        sb.append("[Event \"رُقعة\"]\n")
        sb.append("[White \"$whiteName\"]\n")
        sb.append("[Black \"$blackName\"]\n\n")
        var idx = 0
        while (idx < moves.size) {
            val m = moves[idx]
            if (m.color == PieceColor.WHITE) {
                sb.append("${m.moveNumber}. ${m.san} ")
                if (idx + 1 < moves.size && moves[idx + 1].color == PieceColor.BLACK) {
                    sb.append("${moves[idx + 1].san} ")
                    idx += 2
                } else {
                    idx += 1
                }
            } else {
                sb.append("${m.moveNumber}... ${m.san} ")
                idx += 1
            }
        }
        return sb.toString().trim()
    }

    private fun normalizeSan(s: String): String =
        s.replace("+", "")
            .replace("#", "")
            .replace("!", "")
            .replace("?", "")
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")
            .trim()
}
