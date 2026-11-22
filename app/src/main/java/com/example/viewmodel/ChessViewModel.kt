package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ChessSound
import com.example.audio.SoundManager
import com.example.chess.BoardTheme
import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.ChessCoachAnalyzer
import com.example.chess.ChessEngine
import com.example.chess.ChessMove
import com.example.chess.ChessPuzzle
import com.example.chess.GameResult
import com.example.chess.MoveHint
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.PostGameReport
import com.example.chess.PuzzleRepository
import com.example.chess.RecordedMove
import com.example.chess.Square
import com.example.data.AchievementDef
import com.example.data.AchievementEntity
import com.example.data.ChessDatabase
import com.example.data.ChessRepository
import com.example.data.MoveCodec
import com.example.data.PeriodicReport
import com.example.data.PuzzleRecordEntity
import com.example.data.RatingPointEntity
import com.example.data.ReportPeriod
import com.example.data.SavedGameEntity
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.notifications.DailyReminderScheduler
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveMatchUiState(
    val savedGameId: Long? = null,
    val bot: BotPersona = BotPersona.FISCHER,
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardState: ChessBoardState = ChessBoardState.initial(),
    val legalMoves: List<ChessMove> = ChessBoardState.initial().generateLegalMoves(),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val pendingPromotion: Pair<Square, Square>? = null,
    val recordedMoves: List<RecordedMove> = emptyList(),
    val evalWhiteCp: Int = 15,
    val gameResult: GameResult = GameResult.ONGOING,
    val isBotThinking: Boolean = false,
    val isAnalyzingHint: Boolean = false,
    val currentHint: MoveHint? = null,
    val botChatMessageAr: String = BotPersona.FISCHER.greetingsAr.first(),
    val lastMoveQualityBadge: Pair<MoveQuality, String>? = null,
    val flippedBoard: Boolean = false,
    val reviewIndex: Int? = null, // null = live board; >= 0 = inspecting historical move
    val postGameReport: PostGameReport? = null,
    val newlyUnlockedAchievements: List<AchievementDef> = emptyList(),
    val showPostGameSheet: Boolean = false,
    val isAdaptiveMatch: Boolean = false
) {
    val displayedBoardState: ChessBoardState
        get() = if (reviewIndex != null && reviewIndex in recordedMoves.indices) {
            ChessBoardState.fromFen(recordedMoves[reviewIndex].fenAfter)
        } else {
            boardState
        }

    val displayedLastMove: ChessMove?
        get() = when {
            reviewIndex != null && reviewIndex in recordedMoves.indices -> recordedMoves[reviewIndex].move
            recordedMoves.isNotEmpty() -> recordedMoves.last().move
            else -> null
        }

    val isReviewingPastMove: Boolean get() = reviewIndex != null
}

data class ActivePuzzleUiState(
    val puzzle: ChessPuzzle = PuzzleRepository.puzzles.first(),
    val isDaily: Boolean = false,
    val boardState: ChessBoardState = ChessBoardState.fromFen(PuzzleRepository.puzzles.first().fen),
    val legalMoves: List<ChessMove> = ChessBoardState.fromFen(PuzzleRepository.puzzles.first().fen).generateLegalMoves(),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val stepIndex: Int = 0,
    val attempts: Int = 0,
    val usedHint: Boolean = false,
    val showHintText: Boolean = false,
    val hintArrowMove: ChessMove? = null,
    val statusMessageAr: String = "ابحث عن أفضل نقلة للأبيض!",
    val isErrorFeedback: Boolean = false,
    val isSolved: Boolean = false
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChessDatabase.getInstance(application)
    private val prefsRepo = UserPreferencesRepository(application)
    private val repository = ChessRepository(db, prefsRepo)
    private val soundManager = SoundManager()

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val ongoingGame: StateFlow<SavedGameEntity?> = repository.ongoingGame
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedGames: StateFlow<List<SavedGameEntity>> = repository.completedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ratingHistory: StateFlow<List<RatingPointEntity>> = repository.ratingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val puzzleRecords: StateFlow<List<PuzzleRecordEntity>> = repository.puzzleRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.ALL_TIME)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    val periodicReport: StateFlow<PeriodicReport> = combine(ratingHistory, _selectedPeriod) { pts, period ->
        repository.buildPeriodicReport(pts, period)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.buildPeriodicReport(emptyList(), ReportPeriod.ALL_TIME)
    )

    private val _matchState = MutableStateFlow(ActiveMatchUiState())
    val matchState: StateFlow<ActiveMatchUiState> = _matchState.asStateFlow()

    private val _puzzleState = MutableStateFlow(ActivePuzzleUiState())
    val puzzleState: StateFlow<ActivePuzzleUiState> = _puzzleState.asStateFlow()

    private var botThinkingJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.collect { pref ->
                soundManager.enabled = pref.soundEnabled
            }
        }
    }

    fun selectReportPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    // --- Settings Actions ---
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setBoardTheme(theme: BoardTheme) = viewModelScope.launch { prefsRepo.setBoardTheme(theme) }
    fun setPieceStyle(style: com.example.chess.PieceStyle) = viewModelScope.launch { prefsRepo.setPieceStyle(style) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setSoundEnabled(enabled) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    fun setShowEvalBar(show: Boolean) = viewModelScope.launch { prefsRepo.setShowEvalBar(show) }
    fun setAdaptiveMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAdaptiveMode(enabled) }
    fun setSelectedBotId(botId: String) = viewModelScope.launch { prefsRepo.setSelectedBotId(botId) }

    fun discardOngoingGame(id: Long) = viewModelScope.launch { repository.deleteGame(id) }

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setReminderSettings(enabled, hour, minute)
            val ctx = getApplication<Application>()
            if (enabled) {
                DailyReminderScheduler.scheduleDaily(ctx, hour, minute)
            } else {
                DailyReminderScheduler.cancelDaily(ctx)
            }
        }
    }

    fun sendTestNotification() {
        val pref = preferences.value
        DailyReminderScheduler.showMotivationalNotification(
            getApplication(),
            streak = pref.dailyStreak,
            elo = pref.playerElo
        )
    }

    // --- Match Lifecycle ---
    fun startNewMatch(
        bot: BotPersona,
        playerColor: PieceColor,
        adaptiveMode: Boolean = preferences.value.adaptiveMode
    ) {
        botThinkingJob?.cancel()
        val initial = ChessBoardState.initial()
        _matchState.value = ActiveMatchUiState(
            savedGameId = null,
            bot = bot,
            playerColor = playerColor,
            boardState = initial,
            legalMoves = initial.generateLegalMoves(),
            flippedBoard = playerColor == PieceColor.BLACK,
            botChatMessageAr = bot.greetingsAr.random(),
            isAdaptiveMatch = adaptiveMode
        )
        viewModelScope.launch { prefsRepo.setSelectedBotId(bot.id) }

        if (playerColor == PieceColor.BLACK) {
            triggerBotMove()
        }
    }

    fun resumeSavedGame(entity: SavedGameEntity) {
        botThinkingJob?.cancel()
        val bot = BotPersona.byId(entity.botId)
        val playerColor = runCatching { PieceColor.valueOf(entity.playerColor) }.getOrDefault(PieceColor.WHITE)
        val moves = MoveCodec.decode(entity.movesEncoded)
        val board = ChessBoardState.fromFen(entity.currentFen)
        val legal = board.generateLegalMoves()
        val result = runCatching { GameResult.valueOf(entity.resultName) }.getOrDefault(GameResult.ONGOING)

        val report = if (entity.isCompleted) {
            ChessCoachAnalyzer.buildPostGameReport(
                moves = moves,
                playerColor = playerColor,
                result = result,
                bot = bot,
                currentPlayerElo = preferences.value.playerElo
            )
        } else null

        _matchState.value = ActiveMatchUiState(
            savedGameId = entity.id,
            bot = bot,
            playerColor = playerColor,
            boardState = board,
            legalMoves = legal,
            recordedMoves = moves,
            evalWhiteCp = moves.lastOrNull()?.evalWhiteCp ?: 15,
            gameResult = result,
            flippedBoard = playerColor == PieceColor.BLACK,
            botChatMessageAr = if (entity.isCompleted) "مراجعة وتحليل المباراة السابقة" else "عدنا للمباراة! دورك لإكمال الخطة.",
            postGameReport = report,
            showPostGameSheet = false,
            reviewIndex = if (entity.isCompleted && moves.isNotEmpty()) moves.lastIndex else null,
            isAdaptiveMatch = preferences.value.adaptiveMode
        )

        if (!entity.isCompleted && result == GameResult.ONGOING && board.turn != playerColor) {
            triggerBotMove()
        }
    }

    fun onMatchSquareClick(square: Square) {
        val st = _matchState.value
        if (st.isReviewingPastMove) {
            // Return to live board if user taps while reviewing
            _matchState.update { it.copy(reviewIndex = null) }
            return
        }
        if (st.gameResult != GameResult.ONGOING || st.isBotThinking || st.boardState.turn != st.playerColor) return

        val selected = st.selectedSquare
        if (selected != null) {
            val matchingMoves = st.legalMovesForSelected.filter { it.to == square }
            if (matchingMoves.isNotEmpty()) {
                if (matchingMoves.any { it.promotion != null }) {
                    _matchState.update {
                        it.copy(
                            pendingPromotion = selected to square,
                            selectedSquare = null,
                            legalMovesForSelected = emptyList()
                        )
                    }
                } else {
                    executePlayerMove(matchingMoves.first())
                }
                return
            }
        }

        val piece = st.boardState.pieceAt(square)
        if (piece != null && piece.color == st.playerColor) {
            val movesForPiece = st.legalMoves.filter { it.from == square }
            _matchState.update {
                it.copy(
                    selectedSquare = square,
                    legalMovesForSelected = movesForPiece
                )
            }
        } else {
            _matchState.update {
                it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    fun selectPromotion(pieceType: PieceType) {
        val st = _matchState.value
        val pair = st.pendingPromotion ?: return
        val move = st.legalMoves.find {
            it.from == pair.first && it.to == pair.second && it.promotion == pieceType
        } ?: return
        _matchState.update { it.copy(pendingPromotion = null) }
        executePlayerMove(move)
    }

    fun dismissPromotion() {
        _matchState.update { it.copy(pendingPromotion = null) }
    }

    private fun executePlayerMove(move: ChessMove) {
        val st = _matchState.value
        val beforeState = st.boardState
        val legalBefore = st.legalMoves
        val san = beforeState.toSan(move, legalBefore)
        val phase = beforeState.detectPhase()
        val afterState = beforeState.copyWithMove(move)
        val afterLegal = afterState.generateLegalMoves()
        val result = afterState.evaluateGameResult(afterLegal)

        // Immediate responsive sound
        playMoveSound(move, afterState, result, st.playerColor)

        val placeholderRecord = RecordedMove(
            moveNumber = beforeState.fullMoveNumber,
            color = st.playerColor,
            move = move,
            san = san,
            fenAfter = afterState.toFen(),
            evalWhiteCp = st.evalWhiteCp,
            evalBeforeWhiteCp = st.evalWhiteCp,
            centipawnLoss = 0,
            quality = MoveQuality.GOOD,
            phase = phase
        )
        val updatedList = st.recordedMoves + placeholderRecord

        _matchState.update {
            it.copy(
                boardState = afterState,
                legalMoves = afterLegal,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                recordedMoves = updatedList,
                currentHint = null,
                gameResult = result,
                isBotThinking = result == GameResult.ONGOING
            )
        }

        botThinkingJob = viewModelScope.launch {
            // 1) Analyze player's move in background
            val evalResult = withContext(Dispatchers.Default) {
                ChessCoachAnalyzer.analyzePlayedMove(
                    stateBefore = beforeState,
                    playedMove = move,
                    legalBefore = legalBefore,
                    depth = 2
                )
            }

            val analyzedMove = placeholderRecord.copy(
                evalWhiteCp = evalResult.evalAfterWhiteCp,
                evalBeforeWhiteCp = evalResult.evalBeforeWhiteCp,
                centipawnLoss = evalResult.centipawnLoss,
                quality = evalResult.quality,
                coachNoteAr = evalResult.coachNoteAr,
                bestMoveSan = evalResult.bestMoveSan
            )

            _matchState.update { current ->
                val list = current.recordedMoves.toMutableList()
                if (list.isNotEmpty()) {
                    list[list.lastIndex] = analyzedMove
                }
                val reaction = when (evalResult.quality) {
                    MoveQuality.BRILLIANT, MoveQuality.BEST -> current.bot.goodMoveReactionsAr.random()
                    MoveQuality.MISTAKE, MoveQuality.BLUNDER -> current.bot.blunderReactionsAr.random()
                    else -> current.botChatMessageAr
                }
                current.copy(
                    recordedMoves = list,
                    evalWhiteCp = evalResult.evalAfterWhiteCp,
                    lastMoveQualityBadge = evalResult.quality to evalResult.coachNoteAr,
                    botChatMessageAr = reaction
                )
            }

            if (result != GameResult.ONGOING) {
                finalizeMatch(result)
                return@launch
            }

            // Auto-save ongoing progress
            saveCurrentMatchProgress()

            // 2) Compute Bot response
            delay(220)
            performBotSearchAndMove()
        }
    }

    private fun triggerBotMove() {
        botThinkingJob?.cancel()
        _matchState.update { it.copy(isBotThinking = true) }
        botThinkingJob = viewModelScope.launch {
            delay(320)
            performBotSearchAndMove()
        }
    }

    private suspend fun performBotSearchAndMove() {
        val st = _matchState.value
        if (st.gameResult != GameResult.ONGOING) {
            _matchState.update { it.copy(isBotThinking = false) }
            return
        }
        val stateBefore = st.boardState
        val legalBefore = st.legalMoves
        val adaptiveElo = if (st.isAdaptiveMatch) preferences.value.playerElo else null

        val searchResult = withContext(Dispatchers.Default) {
            ChessEngine.chooseBotMove(stateBefore, st.bot, adaptiveElo)
        }
        val botMove = searchResult.bestMove
        if (botMove == null) {
            val res = stateBefore.evaluateGameResult(legalBefore)
            _matchState.update { it.copy(isBotThinking = false, gameResult = res) }
            if (res != GameResult.ONGOING) finalizeMatch(res)
            return
        }

        val san = stateBefore.toSan(botMove, legalBefore)
        val phase = stateBefore.detectPhase()
        val afterState = stateBefore.copyWithMove(botMove)
        val afterLegal = afterState.generateLegalMoves()
        val result = afterState.evaluateGameResult(afterLegal)

        playMoveSound(botMove, afterState, result, st.playerColor)

        val botRecord = RecordedMove(
            moveNumber = stateBefore.fullMoveNumber,
            color = stateBefore.turn,
            move = botMove,
            san = san,
            fenAfter = afterState.toFen(),
            evalWhiteCp = searchResult.evalWhiteCp,
            evalBeforeWhiteCp = st.evalWhiteCp,
            centipawnLoss = 0,
            quality = MoveQuality.BEST,
            phase = phase,
            coachNoteAr = "نقلة ${st.bot.nameAr}"
        )

        _matchState.update {
            it.copy(
                boardState = afterState,
                legalMoves = afterLegal,
                recordedMoves = it.recordedMoves + botRecord,
                evalWhiteCp = searchResult.evalWhiteCp,
                isBotThinking = false,
                gameResult = result
            )
        }

        if (result != GameResult.ONGOING) {
            finalizeMatch(result)
        } else {
            saveCurrentMatchProgress()
        }
    }

    private suspend fun saveCurrentMatchProgress() {
        val st = _matchState.value
        if (st.recordedMoves.isEmpty() || st.gameResult != GameResult.ONGOING) return
        val opening = ChessBoardState.recognizeOpeningArabic(st.recordedMoves.map { it.move.toUci() })
        val id = repository.saveOngoingGame(
            existingId = st.savedGameId,
            botId = st.bot.id,
            playerColor = st.playerColor,
            currentFen = st.boardState.toFen(),
            moves = st.recordedMoves,
            openingNameAr = opening
        )
        _matchState.update { it.copy(savedGameId = id) }
    }

    private suspend fun finalizeMatch(result: GameResult) {
        val st = _matchState.value
        val report = withContext(Dispatchers.Default) {
            ChessCoachAnalyzer.buildPostGameReport(
                moves = st.recordedMoves,
                playerColor = st.playerColor,
                result = result,
                bot = st.bot,
                currentPlayerElo = preferences.value.playerElo
            )
        }
        val unlocked = repository.finalizeCompletedGame(
            existingId = st.savedGameId,
            bot = st.bot,
            playerColor = st.playerColor,
            currentFen = st.boardState.toFen(),
            moves = st.recordedMoves,
            result = result,
            report = report,
            currentElo = preferences.value.playerElo,
            solvedPuzzleCount = puzzleRecords.value.size
        )

        _matchState.update {
            it.copy(
                gameResult = result,
                isBotThinking = false,
                postGameReport = report,
                newlyUnlockedAchievements = unlocked,
                showPostGameSheet = true
            )
        }
    }

    fun requestHint() {
        val st = _matchState.value
        if (st.gameResult != GameResult.ONGOING || st.isBotThinking || st.isAnalyzingHint) return
        if (st.boardState.turn != st.playerColor) return
        _matchState.update { it.copy(isAnalyzingHint = true) }
        viewModelScope.launch {
            val hint = withContext(Dispatchers.Default) {
                ChessCoachAnalyzer.generateHint(st.boardState)
            }
            _matchState.update {
                it.copy(
                    isAnalyzingHint = false,
                    currentHint = hint
                )
            }
        }
    }

    fun undoLastTurn() {
        val st = _matchState.value
        if (st.isBotThinking || st.gameResult != GameResult.ONGOING) return
        val moves = st.recordedMoves
        if (moves.isEmpty()) return

        // Undo back until it's player's turn again (usually 2 plies: bot + player)
        var dropCount = if (moves.last().color != st.playerColor) 2 else 1
        if (moves.size < dropCount) dropCount = moves.size
        val remaining = moves.dropLast(dropCount)

        val targetBoard = if (remaining.isEmpty()) {
            ChessBoardState.initial()
        } else {
            ChessBoardState.fromFen(remaining.last().fenAfter)
        }
        _matchState.update {
            it.copy(
                boardState = targetBoard,
                legalMoves = targetBoard.generateLegalMoves(),
                recordedMoves = remaining,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                currentHint = null,
                reviewIndex = null,
                evalWhiteCp = remaining.lastOrNull()?.evalWhiteCp ?: 15,
                lastMoveQualityBadge = null
            )
        }
        viewModelScope.launch {
            if (remaining.isEmpty() && st.savedGameId != null) {
                repository.deleteGame(st.savedGameId)
                _matchState.update { it.copy(savedGameId = null) }
            } else {
                saveCurrentMatchProgress()
            }
        }
    }

    fun toggleBoardFlip() {
        _matchState.update { it.copy(flippedBoard = !it.flippedBoard) }
    }

    fun resignMatch() {
        val st = _matchState.value
        if (st.gameResult != GameResult.ONGOING) return
        botThinkingJob?.cancel()
        val res = if (st.playerColor == PieceColor.WHITE) GameResult.WHITE_RESIGNED else GameResult.BLACK_RESIGNED
        soundManager.play(ChessSound.DEFEAT)
        viewModelScope.launch {
            finalizeMatch(res)
        }
    }

    fun selectReviewMove(index: Int?) {
        _matchState.update { it.copy(reviewIndex = index) }
    }

    fun openPostGameSheet() {
        _matchState.update { it.copy(showPostGameSheet = true) }
    }

    fun dismissPostGameSheet() {
        _matchState.update { it.copy(showPostGameSheet = false) }
    }

    // --- Puzzle Solver ---
    fun loadPuzzle(puzzle: ChessPuzzle, isDaily: Boolean = false) {
        val board = ChessBoardState.fromFen(puzzle.fen)
        _puzzleState.value = ActivePuzzleUiState(
            puzzle = puzzle,
            isDaily = isDaily,
            boardState = board,
            legalMoves = board.generateLegalMoves(),
            statusMessageAr = "${puzzle.category.labelAr} • ابحث عن النقلة الأفضل!"
        )
    }

    fun loadDailyPuzzle() {
        val daily = PuzzleRepository.dailyPuzzleForToday(preferences.value.playerElo)
        loadPuzzle(daily, isDaily = true)
    }

    fun onPuzzleSquareClick(square: Square) {
        val st = _puzzleState.value
        if (st.isSolved) return

        val selected = st.selectedSquare
        if (selected != null) {
            val move = st.legalMovesForSelected.find { it.to == square }
            if (move != null) {
                handlePuzzlePlayerMove(move)
                return
            }
        }

        val piece = st.boardState.pieceAt(square)
        if (piece != null && piece.color == st.boardState.turn) {
            _puzzleState.update {
                it.copy(
                    selectedSquare = square,
                    legalMovesForSelected = st.legalMoves.filter { m -> m.from == square }
                )
            }
        } else {
            _puzzleState.update {
                it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    private fun handlePuzzlePlayerMove(move: ChessMove) {
        val st = _puzzleState.value
        val expectedUci = st.puzzle.solutionUci.getOrNull(st.stepIndex) ?: return

        // Match UCI (allow auto-queen promotion if solution ends in q)
        val playedUci = move.toUci()
        val matches = playedUci == expectedUci ||
            (expectedUci.length == 5 && playedUci.startsWith(expectedUci.substring(0, 4)) && move.promotion == PieceType.QUEEN)

        if (!matches) {
            soundManager.play(ChessSound.DEFEAT)
            _puzzleState.update {
                it.copy(
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    attempts = it.attempts + 1,
                    isErrorFeedback = true,
                    statusMessageAr = "ليست النقلة الدقيقة! جرّب البحث عن تهديد أقوى."
                )
            }
            return
        }

        // Correct move!
        val afterPlayer = st.boardState.copyWithMove(move)
        val nextStep = st.stepIndex + 1

        if (nextStep >= st.puzzle.solutionUci.size) {
            // Puzzle solved!
            soundManager.play(ChessSound.PUZZLE_SOLVED)
            _puzzleState.update {
                it.copy(
                    boardState = afterPlayer,
                    legalMoves = emptyList(),
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    stepIndex = nextStep,
                    hintArrowMove = null,
                    isErrorFeedback = false,
                    isSolved = true,
                    statusMessageAr = "أحسنت! حلّ عبقري: ${st.puzzle.explanationAr}"
                )
            }
            viewModelScope.launch {
                repository.recordPuzzleSolved(
                    puzzleId = st.puzzle.id,
                    usedHint = st.usedHint,
                    attempts = st.attempts + 1,
                    isDaily = st.isDaily,
                    todayKey = PuzzleRepository.todayDateKey(),
                    totalSolvedSoFar = puzzleRecords.value.size,
                    currentStreak = preferences.value.dailyStreak
                )
            }
            return
        }

        // Play opponent forced reply automatically
        soundManager.play(ChessSound.MOVE)
        _puzzleState.update {
            it.copy(
                boardState = afterPlayer,
                legalMoves = afterPlayer.generateLegalMoves(),
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                stepIndex = nextStep,
                hintArrowMove = null,
                isErrorFeedback = false,
                statusMessageAr = "ممتاز! أكمل الضربة الحاسمة..."
            )
        }

        viewModelScope.launch {
            delay(420)
            val curr = _puzzleState.value
            val oppUci = curr.puzzle.solutionUci.getOrNull(nextStep) ?: return@launch
            val oppMove = curr.legalMoves.find { it.toUci() == oppUci } ?: return@launch
            val afterOpp = curr.boardState.copyWithMove(oppMove)
            soundManager.play(if (oppMove.capturedPiece != null) ChessSound.CAPTURE else ChessSound.MOVE)
            _puzzleState.update {
                it.copy(
                    boardState = afterOpp,
                    legalMoves = afterOpp.generateLegalMoves(),
                    stepIndex = nextStep + 1,
                    statusMessageAr = "الآن وجّه النقلة النهائية!"
                )
            }
        }
    }

    fun showPuzzleHint() {
        val st = _puzzleState.value
        val expectedUci = st.puzzle.solutionUci.getOrNull(st.stepIndex) ?: return
        val hintMove = st.legalMoves.find { it.toUci() == expectedUci }
        _puzzleState.update {
            it.copy(
                usedHint = true,
                showHintText = true,
                hintArrowMove = hintMove
            )
        }
    }

    private fun playMoveSound(
        move: ChessMove,
        afterState: ChessBoardState,
        result: GameResult,
        playerColor: PieceColor
    ) {
        when {
            result == GameResult.WHITE_WINS_CHECKMATE || result == GameResult.BLACK_WINS_CHECKMATE -> {
                val playerWon = (result == GameResult.WHITE_WINS_CHECKMATE && playerColor == PieceColor.WHITE) ||
                    (result == GameResult.BLACK_WINS_CHECKMATE && playerColor == PieceColor.BLACK)
                soundManager.play(if (playerWon) ChessSound.VICTORY else ChessSound.DEFEAT)
            }
            afterState.isInCheck(afterState.turn) -> soundManager.play(ChessSound.CHECK)
            move.isCastleKingside || move.isCastleQueenside -> soundManager.play(ChessSound.CASTLE)
            move.capturedPiece != null || move.isEnPassant -> soundManager.play(ChessSound.CAPTURE)
            else -> soundManager.play(ChessSound.MOVE)
        }
    }
}
