package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ChessHapticEvent
import com.example.audio.ChessSound
import com.example.audio.HapticsManager
import com.example.audio.SoundManager
import com.example.chess.BoardMaterial
import com.example.chess.BoardTheme
import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.ChessCoachAnalyzer
import com.example.chess.ChessMove
import com.example.chess.ChessPuzzle
import com.example.chess.EngineBackendPreference
import com.example.chess.EnginePersonality
import com.example.chess.EvalBarPosition
import com.example.chess.FloatingBubbleSize
import com.example.chess.FloatingFrameShape
import com.example.chess.GameMode
import com.example.chess.GameResult
import com.example.chess.HintStage
import com.example.chess.LastMoveHighlightStyle
import com.example.chess.MoveHint
import com.example.chess.MoveQuality
import com.example.chess.MultiPvLine
import com.example.chess.ParsedGameData
import com.example.chess.PersonalityRisk
import com.example.chess.PgnFenCodec
import com.example.chess.PieceAnimationStyle
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.chess.PostGameReport
import com.example.chess.ProgressiveHint
import com.example.chess.PuzzleRepository
import com.example.chess.RecordedMove
import com.example.chess.SoundProfile
import com.example.chess.Square
import com.example.chess.StockfishProbeResult
import com.example.chess.StockfishUciEngine
import com.example.chess.StrengthTier
import com.example.chess.SyzygyImportReport
import com.example.chess.SyzygyManager
import com.example.chess.SyzygyTableEntry
import com.example.chess.ThreatArrow
import com.example.chess.TimeControl
import com.example.chess.VibrationStyle
import com.example.data.AchievementDef
import com.example.data.AchievementEntity
import com.example.data.ChessDatabase
import com.example.data.ChessRepository
import com.example.data.MoveCodec
import com.example.data.PeriodicReport
import com.example.data.PuzzleRecordEntity
import com.example.data.RatingPointEntity
import com.example.data.ReportPeriod
import com.example.data.RuqaaEngineStatsSummary
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
import java.util.UUID

data class ActiveMatchUiState(
    val sessionUuid: String = UUID.randomUUID().toString(),
    val savedGameId: Long? = null,
    val gameMode: GameMode = GameMode.VS_ENGINE,
    val bot: BotPersona = BotPersona.FISCHER,
    val usingCustomEngine: Boolean = true,
    val customEngineElo: Int = 700,
    val customPersonality: EnginePersonality = EnginePersonality.CLASSIC,
    val personalityIntensity: Int = 50,
    val personalityRisk: PersonalityRisk = PersonalityRisk.BALANCED,
    val humanStyle: Boolean = true,
    val customStartFen: String? = null,
    val editedPosition: Boolean = false,
    val strengthMixed: Boolean = false,
    val hintsUsed: Int = 0,
    val assisted: Boolean = false,
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardState: ChessBoardState = ChessBoardState.initial(),
    val legalMoves: List<ChessMove> = ChessBoardState.initial().generateLegalMoves(),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val pendingPromotion: Pair<Square, Square>? = null,
    val recordedMoves: List<RecordedMove> = emptyList(),
    val evalWhiteCp: Int = 15,
    val gameResult: GameResult = GameResult.ONGOING,
    val claimableDrawReason: String? = null,
    val isBotThinking: Boolean = false,
    val isAnalyzingHint: Boolean = false,
    val currentHint: MoveHint? = null,
    val progressiveHint: ProgressiveHint? = null,
    val currentHintStage: HintStage? = null,
    val hintCacheByFen: Map<String, ProgressiveHint> = emptyMap(),
    val botChatMessageAr: String = "مخصص • ٧٠٠ إيلو • كلاسيكي",
    val lastMoveQualityBadge: Pair<MoveQuality, String>? = null,
    val flippedBoard: Boolean = false,
    val reviewIndex: Int? = null,
    val postGameReport: PostGameReport? = null,
    val newlyUnlockedAchievements: List<AchievementDef> = emptyList(),
    val showPostGameSheet: Boolean = false,
    val showMoveLedgerSheet: Boolean = false,
    val isAdaptiveMatch: Boolean = false,
    val timeControl: TimeControl = TimeControl.UNLIMITED,
    val whiteTimeSec: Long = TimeControl.UNLIMITED.initialSeconds,
    val blackTimeSec: Long = TimeControl.UNLIMITED.initialSeconds,
    // Pausable full-game review
    val isFullReviewRunning: Boolean = false,
    val isFullReviewPaused: Boolean = false,
    val fullReviewNextIndex: Int = 0,
    val fullReviewCompleted: Boolean = false,
    val reviewCacheKey: String = "",
    // MultiPV-3..5 Analysis & Chessis Pro HUD State
    val isAnalyzingMultiPv: Boolean = false,
    val multiPvLines: List<MultiPvLine> = emptyList(),
    val pendingPremove: Pair<Square, Square>? = null,
    val pauseAlertMove: RecordedMove? = null,
    val threatArrows: List<ThreatArrow> = emptyList(),
    val showPostGameReportBar: Boolean = true,
    val consecutiveBotLostTurns: Int = 0,
    val previousBestMoveForReview: ChessMove? = null
) {
    val openingNameAr: String
        get() = ChessBoardState.recognizeOpeningArabic(recordedMoves.map { it.move.toUci() })

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

    val effectiveBotId: String
        get() = when (gameMode) {
            GameMode.LOCAL_TWO_PLAYER -> "local_2p"
            GameMode.VS_ENGINE -> "custom:${customPersonality.id}:$customEngineElo"
            GameMode.VS_BOT_PERSONA -> bot.id
        }

    val matchSubtitleAr: String
        get() = when (gameMode) {
            GameMode.LOCAL_TWO_PLAYER -> "لاعبان محليان على جهاز واحد"
            GameMode.VS_ENGINE -> {
                val eloStr = if (customEngineElo <= 0) "بلا حد (أقصى قوة)" else "نحو $customEngineElo إيلو"
                "ضد المحرك • $eloStr • ${customPersonality.nameAr}"
            }
            GameMode.VS_BOT_PERSONA -> "${bot.nameAr} • ${bot.elo} ELO"
        }
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
    private val hapticsManager = HapticsManager(application)

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val ongoingGame: StateFlow<SavedGameEntity?> = repository.ongoingGame
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedGames: StateFlow<List<SavedGameEntity>> = repository.completedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGames: StateFlow<List<SavedGameEntity>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ratingHistory: StateFlow<List<RatingPointEntity>> = repository.ratingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val puzzleRecords: StateFlow<List<PuzzleRecordEntity>> = repository.puzzleRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.ALL_TIME)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    private val _unassistedStatsOnly = MutableStateFlow(false)
    val unassistedStatsOnly: StateFlow<Boolean> = _unassistedStatsOnly.asStateFlow()

    val engineStatsSummary: StateFlow<RuqaaEngineStatsSummary> = combine(allGames, _unassistedStatsOnly) { games, filter ->
        repository.buildEngineStatsSummary(games, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RuqaaEngineStatsSummary())

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

    private val _stockfishStatus = MutableStateFlow(StockfishUciEngine.quickInspectStatic(application))
    val stockfishStatus: StateFlow<StockfishProbeResult> = _stockfishStatus.asStateFlow()

    private val _lastEngineTelemetry = MutableStateFlow("")
    val lastEngineTelemetry: StateFlow<String> = _lastEngineTelemetry.asStateFlow()

    private val _syzygyTables = MutableStateFlow<List<SyzygyTableEntry>>(emptyList())
    val syzygyTables: StateFlow<List<SyzygyTableEntry>> = _syzygyTables.asStateFlow()

    private val _syzygyStatusMessage = MutableStateFlow<String?>(null)
    val syzygyStatusMessage: StateFlow<String?> = _syzygyStatusMessage.asStateFlow()

    private var botThinkingJob: Job? = null
    private var clockJob: Job? = null
    private var fullReviewJob: Job? = null
    private var multiPvJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.collect { pref ->
                soundManager.enabled = pref.soundEnabled
                soundManager.profile = pref.soundProfile
                soundManager.volumePercent = pref.soundVolume
                hapticsManager.enabled = pref.hapticsEnabled
                hapticsManager.style = pref.vibrationStyle
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _stockfishStatus.value = StockfishUciEngine.warmupAndProbe(getApplication())
        }
        refreshSyzygyTables()
    }

    fun selectReportPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun setUnassistedStatsOnly(only: Boolean) {
        _unassistedStatsOnly.value = only
    }

    // --- Settings Actions ---
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setBoardTheme(theme: BoardTheme) = viewModelScope.launch { prefsRepo.setBoardTheme(theme) }
    fun setPieceStyle(style: PieceStyle) = viewModelScope.launch { prefsRepo.setPieceStyle(style) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setSoundEnabled(enabled) }
    fun setSoundProfile(profile: SoundProfile) = viewModelScope.launch { prefsRepo.setSoundProfile(profile) }
    fun previewSoundProfile(profile: SoundProfile) {
        soundManager.previewProfile(profile, preferences.value.soundVolume)
    }
    fun setSoundVolume(vol: Int) = viewModelScope.launch { prefsRepo.setSoundVolume(vol) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    fun setVibrationStyle(style: VibrationStyle) = viewModelScope.launch { prefsRepo.setVibrationStyle(style) }
    fun setShowEvalBar(show: Boolean) = viewModelScope.launch { prefsRepo.setShowEvalBar(show) }
    fun setAdaptiveMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAdaptiveMode(enabled) }
    fun setSelectedBotId(botId: String) = viewModelScope.launch { prefsRepo.setSelectedBotId(botId) }

    fun saveBoardAppearance(
        pieceStyle: PieceStyle,
        boardMaterial: BoardMaterial,
        selectedColorHex: Long,
        lastMoveColorHex: Long,
        legalMoveColorHex: Long,
        showLastMove: Boolean,
        showLegalMoves: Boolean,
        useRings: Boolean,
        lastMoveHighlightStyle: LastMoveHighlightStyle = preferences.value.lastMoveHighlightStyle,
        pieceAnimationStyle: PieceAnimationStyle = preferences.value.pieceAnimationStyle,
        showCoordinates: Boolean = preferences.value.showCoordinates,
        boardTheme: BoardTheme? = null
    ) = viewModelScope.launch {
        prefsRepo.saveBoardAppearance(
            pieceStyle = pieceStyle,
            boardMaterial = boardMaterial,
            selectedColorHex = selectedColorHex,
            lastMoveColorHex = lastMoveColorHex,
            legalMoveColorHex = legalMoveColorHex,
            showLastMove = showLastMove,
            showLegalMoves = showLegalMoves,
            useRingsInsteadOfDots = useRings,
            lastMoveHighlightStyle = lastMoveHighlightStyle,
            pieceAnimationStyle = pieceAnimationStyle,
            showCoordinates = showCoordinates,
            boardTheme = boardTheme
        )
    }

    fun setStockfishThreads(threads: Int) = viewModelScope.launch { prefsRepo.setStockfishThreads(threads) }
    fun setStockfishHashMb(hashMb: Int) = viewModelScope.launch { prefsRepo.setStockfishHashMb(hashMb) }
    fun setPowerSavingMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setPowerSavingMode(enabled) }
    fun setPremoveEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setPremoveEnabled(enabled)
        if (!enabled) _matchState.update { it.copy(pendingPremove = null) }
    }
    fun setUnassistedMode(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setUnassistedMode(enabled)
        if (enabled) _matchState.update { it.copy(pendingPremove = null, currentHint = null, multiPvLines = emptyList()) }
    }
    fun setShowThreats(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowThreats(enabled) }
        if (enabled) {
            recomputeThreatsAsync(_matchState.value.displayedBoardState)
        } else {
            _matchState.update { it.copy(threatArrows = emptyList()) }
        }
    }
    fun setBotResignEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setBotResignEnabled(enabled) }
    fun setPauseOnBlunder(enabled: Boolean) = viewModelScope.launch { prefsRepo.setPauseOnBlunder(enabled) }
    fun setPauseOnMistake(enabled: Boolean) = viewModelScope.launch { prefsRepo.setPauseOnMistake(enabled) }
    fun setShowMyMoveStrength(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowMyMoveStrength(enabled) }
    fun setShowOpponentMoveStrength(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowOpponentMoveStrength(enabled) }
    fun setEvalBarPosition(position: EvalBarPosition) = viewModelScope.launch { prefsRepo.setEvalBarPosition(position) }
    fun setShowEngineArrowsOnBoard(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowEngineArrowsOnBoard(enabled) }
    fun setAnalysisMultiPvCount(count: Int) = viewModelScope.launch { prefsRepo.setAnalysisMultiPvCount(count) }
    fun setShowMultiPvArrowNumbers(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowMultiPvArrowNumbers(enabled) }
    fun setArrowStrengthColors(enabled: Boolean) = viewModelScope.launch { prefsRepo.setArrowStrengthColors(enabled) }
    fun setShowPreviousBestGhostArrow(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowPreviousBestGhostArrow(enabled) }
    fun setColorLastMoveByQuality(enabled: Boolean) = viewModelScope.launch { prefsRepo.setColorLastMoveByQuality(enabled) }
    fun setFigurineNotation(enabled: Boolean) = viewModelScope.launch { prefsRepo.setFigurineNotation(enabled) }
    fun setShowCapturedPieces(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowCapturedPieces(enabled) }
    fun setShowCoordinates(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowCoordinates(enabled) }
    fun setTapOnlyDisableDrag(enabled: Boolean) = viewModelScope.launch { prefsRepo.setTapOnlyDisableDrag(enabled) }
    fun setEnlargeOnDrag(enabled: Boolean) = viewModelScope.launch { prefsRepo.setEnlargeOnDrag(enabled) }
    fun setShowAverageCplInReport(enabled: Boolean) = viewModelScope.launch { prefsRepo.setShowAverageCplInReport(enabled) }
    fun setBoardScalePercent(scale: Int) = viewModelScope.launch { prefsRepo.setBoardScalePercent(scale) }

    fun setStrengthTier(tier: StrengthTier) = viewModelScope.launch {
        prefsRepo.setStrengthTier(tier)
        _matchState.update { st ->
            if (st.usingCustomEngine) {
                val mixed = st.recordedMoves.isNotEmpty() && st.gameResult.isOngoing
                st.copy(
                    customEngineElo = tier.targetElo,
                    strengthMixed = st.strengthMixed || mixed
                )
            } else st
        }
    }

    fun setCustomEngineElo(elo: Int) = viewModelScope.launch {
        val snapped = StrengthTier.snapElo(elo)
        prefsRepo.setCustomEngineElo(snapped)
        _matchState.update { st ->
            if (st.usingCustomEngine) {
                val mixed = st.recordedMoves.isNotEmpty() && st.gameResult.isOngoing
                st.copy(
                    customEngineElo = snapped,
                    strengthMixed = st.strengthMixed || mixed
                )
            } else st
        }
    }

    fun setHumanStyleEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setHumanStyleEnabled(enabled)
        _matchState.update { st ->
            if (st.usingCustomEngine) st.copy(humanStyle = enabled) else st
        }
    }

    fun setEnginePersonality(personality: EnginePersonality) = viewModelScope.launch {
        prefsRepo.setEnginePersonality(personality)
        _matchState.update { st ->
            if (st.usingCustomEngine) {
                val mixed = st.recordedMoves.isNotEmpty() && st.gameResult.isOngoing
                st.copy(
                    customPersonality = personality,
                    strengthMixed = st.strengthMixed || mixed
                )
            } else st
        }
    }

    fun setPersonalityIntensity(intensity: Int) = viewModelScope.launch {
        prefsRepo.setPersonalityIntensity(intensity)
        _matchState.update { st ->
            if (st.usingCustomEngine) st.copy(personalityIntensity = intensity.coerceIn(0, 100)) else st
        }
    }

    fun setPersonalityRisk(risk: PersonalityRisk) = viewModelScope.launch {
        prefsRepo.setPersonalityRisk(risk)
        _matchState.update { st ->
            if (st.usingCustomEngine) st.copy(personalityRisk = risk) else st
        }
    }

    fun setEngineBackendPreference(backend: EngineBackendPreference) = viewModelScope.launch {
        prefsRepo.setEngineBackendPreference(backend)
        refreshStockfishProbe()
    }

    fun setStockfishMoveTimeMs(ms: Int) = viewModelScope.launch {
        prefsRepo.setStockfishMoveTimeMs(ms)
    }

    fun refreshStockfishProbe() {
        viewModelScope.launch(Dispatchers.IO) {
            _stockfishStatus.value = StockfishUciEngine.warmupAndProbe(getApplication())
        }
    }

    fun refreshSyzygyTables() {
        viewModelScope.launch(Dispatchers.IO) {
            _syzygyTables.value = SyzygyManager.listTables(getApplication())
        }
    }

    fun importSyzygyFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val report: SyzygyImportReport = SyzygyManager.importFromUris(getApplication(), uris)
            StockfishUciEngine.resetSyzygyFallbackFlag()
            _syzygyTables.value = SyzygyManager.listTables(getApplication())
            _syzygyStatusMessage.value = buildString {
                if (report.importedCount > 0) {
                    append("تم استيراد ${report.importedCount} جدول بنجاح.")
                }
                if (report.skippedReasonsAr.isNotEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(report.skippedReasonsAr.first())
                }
            }.ifBlank { "لم يتم استيراد أي ملف." }
        }
    }

    fun deleteSyzygyTable(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SyzygyManager.deleteTable(getApplication(), fileName)
            _syzygyTables.value = SyzygyManager.listTables(getApplication())
        }
    }

    fun clearSyzygyTables() {
        viewModelScope.launch(Dispatchers.IO) {
            SyzygyManager.clearAll(getApplication())
            _syzygyTables.value = emptyList()
        }
    }

    fun setAutoReviewAfterGame(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setAutoReviewAfterGame(enabled)
    }

    fun setFloatingAutoShow(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setFloatingAutoShow(enabled)
    }

    fun saveFloatingCustomization(
        widthDp: Int,
        frameShape: FloatingFrameShape,
        opacityPercent: Int,
        bubbleSize: FloatingBubbleSize
    ) = viewModelScope.launch {
        prefsRepo.saveFloatingCustomization(widthDp, frameShape, opacityPercent, bubbleSize)
    }

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
        adaptiveMode: Boolean = preferences.value.adaptiveMode,
        timeControl: TimeControl = TimeControl.MIN_10
    ) {
        cancelBackgroundJobs()
        val initial = ChessBoardState.initial()
        _matchState.value = ActiveMatchUiState(
            sessionUuid = UUID.randomUUID().toString(),
            savedGameId = null,
            gameMode = GameMode.VS_BOT_PERSONA,
            bot = bot,
            usingCustomEngine = false,
            customEngineElo = bot.elo,
            playerColor = playerColor,
            boardState = initial,
            legalMoves = initial.generateLegalMoves(),
            flippedBoard = playerColor == PieceColor.BLACK,
            botChatMessageAr = bot.greetingsAr.random(),
            isAdaptiveMatch = adaptiveMode,
            timeControl = timeControl,
            whiteTimeSec = timeControl.initialSeconds,
            blackTimeSec = timeControl.initialSeconds
        )
        soundManager.play(ChessSound.GAME_START)
        viewModelScope.launch { prefsRepo.setSelectedBotId(bot.id) }
        startClockTickerIfTimed()

        if (playerColor == PieceColor.BLACK) {
            triggerBotMove()
        }
    }

    fun startCustomEngineMatch(
        playerColor: PieceColor,
        targetElo: Int = preferences.value.effectiveEngineElo,
        personality: EnginePersonality = preferences.value.enginePersonality,
        humanStyle: Boolean = preferences.value.humanStyleEnabled,
        timeControl: TimeControl = TimeControl.UNLIMITED,
        startFen: String? = null,
        edited: Boolean = false
    ) {
        cancelBackgroundJobs()
        val initialBoard = startFen?.let { ChessBoardState.fromFen(it) } ?: ChessBoardState.initial()
        val legal = initialBoard.generateLegalMoves()
        val result = initialBoard.evaluateGameResult(legal)
        val pref = preferences.value
        val eloLabel = if (targetElo <= 0) "بلا حد" else "$targetElo إيلو"
        _matchState.value = ActiveMatchUiState(
            sessionUuid = UUID.randomUUID().toString(),
            savedGameId = null,
            gameMode = GameMode.VS_ENGINE,
            bot = BotPersona.FISCHER,
            usingCustomEngine = true,
            customEngineElo = targetElo,
            customPersonality = personality,
            personalityIntensity = pref.personalityIntensity,
            personalityRisk = pref.personalityRisk,
            humanStyle = humanStyle,
            customStartFen = startFen,
            editedPosition = edited || (startFen != null && startFen != ChessBoardState.INITIAL_FEN),
            playerColor = playerColor,
            boardState = initialBoard,
            legalMoves = legal,
            gameResult = result,
            claimableDrawReason = initialBoard.canClaimDraw(),
            flippedBoard = playerColor == PieceColor.BLACK,
            botChatMessageAr = "${personality.nameAr} • $eloLabel",
            isAdaptiveMatch = false,
            timeControl = timeControl,
            whiteTimeSec = timeControl.initialSeconds,
            blackTimeSec = timeControl.initialSeconds
        )
        soundManager.play(ChessSound.GAME_START)
        startClockTickerIfTimed()
        if (result == GameResult.ONGOING && initialBoard.turn != playerColor) {
            triggerBotMove()
        }
    }

    fun startLocalTwoPlayerMatch(
        timeControl: TimeControl = TimeControl.UNLIMITED,
        startFen: String? = null,
        edited: Boolean = false
    ) {
        cancelBackgroundJobs()
        val initialBoard = startFen?.let { ChessBoardState.fromFen(it) } ?: ChessBoardState.initial()
        val legal = initialBoard.generateLegalMoves()
        val result = initialBoard.evaluateGameResult(legal)
        _matchState.value = ActiveMatchUiState(
            sessionUuid = UUID.randomUUID().toString(),
            savedGameId = null,
            gameMode = GameMode.LOCAL_TWO_PLAYER,
            usingCustomEngine = false,
            customStartFen = startFen,
            editedPosition = edited || (startFen != null && startFen != ChessBoardState.INITIAL_FEN),
            playerColor = PieceColor.WHITE,
            boardState = initialBoard,
            legalMoves = legal,
            gameResult = result,
            claimableDrawReason = initialBoard.canClaimDraw(),
            flippedBoard = false,
            botChatMessageAr = "مباراة محلية • دور الأبيض",
            timeControl = timeControl,
            whiteTimeSec = timeControl.initialSeconds,
            blackTimeSec = timeControl.initialSeconds
        )
        startClockTickerIfTimed()
    }

    fun importParsedGame(
        parsed: ParsedGameData,
        playerColor: PieceColor = PieceColor.WHITE,
        playFromPosition: Boolean = false
    ) {
        cancelBackgroundJobs()
        val pref = preferences.value
        val legal = parsed.finalBoard.generateLegalMoves()
        val result = parsed.finalBoard.evaluateGameResult(legal)
        _matchState.value = ActiveMatchUiState(
            sessionUuid = UUID.randomUUID().toString(),
            savedGameId = null,
            gameMode = GameMode.VS_ENGINE,
            usingCustomEngine = true,
            customEngineElo = pref.effectiveEngineElo,
            customPersonality = pref.enginePersonality,
            personalityIntensity = pref.personalityIntensity,
            personalityRisk = pref.personalityRisk,
            humanStyle = pref.humanStyleEnabled,
            customStartFen = parsed.startFen.takeIf { it != ChessBoardState.INITIAL_FEN },
            editedPosition = parsed.startFen != ChessBoardState.INITIAL_FEN,
            playerColor = playerColor,
            boardState = parsed.finalBoard,
            legalMoves = legal,
            recordedMoves = parsed.moves,
            evalWhiteCp = parsed.moves.lastOrNull()?.evalWhiteCp ?: 0,
            gameResult = result,
            claimableDrawReason = parsed.finalBoard.canClaimDraw(),
            flippedBoard = playerColor == PieceColor.BLACK,
            botChatMessageAr = "تم استيراد: ${parsed.openingNameAr}",
            reviewIndex = if (!playFromPosition && parsed.moves.isNotEmpty()) parsed.moves.lastIndex else null,
            timeControl = TimeControl.UNLIMITED
        )
        if (playFromPosition && result == GameResult.ONGOING && parsed.finalBoard.turn != playerColor) {
            triggerBotMove()
        }
    }

    fun exportCurrentGamePgn(): String {
        val st = _matchState.value
        val whiteName = when {
            st.gameMode == GameMode.LOCAL_TWO_PLAYER -> "الأبيض (محلي)"
            st.playerColor == PieceColor.WHITE -> "اللاعب"
            else -> st.matchSubtitleAr
        }
        val blackName = when {
            st.gameMode == GameMode.LOCAL_TWO_PLAYER -> "الأسود (محلي)"
            st.playerColor == PieceColor.BLACK -> "اللاعب"
            else -> st.matchSubtitleAr
        }
        return PgnFenCodec.exportPgn(st.recordedMoves, whiteName = whiteName, blackName = blackName)
    }

    private fun cancelBackgroundJobs() {
        botThinkingJob?.cancel()
        clockJob?.cancel()
        fullReviewJob?.cancel()
        multiPvJob?.cancel()
    }

    private fun startClockTickerIfTimed() {
        clockJob?.cancel()
        val tc = _matchState.value.timeControl
        if (tc == TimeControl.UNLIMITED || _matchState.value.gameResult != GameResult.ONGOING) return
        clockJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val st = _matchState.value
                if (st.gameResult != GameResult.ONGOING) break
                if (st.recordedMoves.isEmpty()) continue
                if (st.boardState.turn == PieceColor.WHITE) {
                    val next = (st.whiteTimeSec - 1).coerceAtLeast(0L)
                    _matchState.update { it.copy(whiteTimeSec = next) }
                    if (next == 0L) {
                        val res = st.boardState.evaluateTimeoutResult(PieceColor.WHITE)
                        playEndFeedback(res, st.playerColor)
                        finalizeMatch(res)
                        break
                    }
                } else {
                    val next = (st.blackTimeSec - 1).coerceAtLeast(0L)
                    _matchState.update { it.copy(blackTimeSec = next) }
                    if (next == 0L) {
                        val res = st.boardState.evaluateTimeoutResult(PieceColor.BLACK)
                        playEndFeedback(res, st.playerColor)
                        finalizeMatch(res)
                        break
                    }
                }
            }
        }
    }

    fun resumeSavedGame(entity: SavedGameEntity) {
        cancelBackgroundJobs()
        val mode = GameMode.byId(entity.gameMode)
        val isCustom = entity.botId.startsWith("custom:")
        val customParts = if (isCustom) entity.botId.split(":") else emptyList()
        val customPersonality = if (customParts.size >= 2) {
            EnginePersonality.byId(customParts[1])
        } else preferences.value.enginePersonality
        val customElo = customParts.getOrNull(2)?.toIntOrNull() ?: preferences.value.effectiveEngineElo

        val bot = BotPersona.byId(entity.botId)
        val playerColor = runCatching { PieceColor.valueOf(entity.playerColor) }.getOrDefault(PieceColor.WHITE)
        val moves = MoveCodec.decode(entity.movesEncoded)
        val board = ChessBoardState.fromFen(entity.currentFen)
        val legal = board.generateLegalMoves()
        val result = runCatching { GameResult.valueOf(entity.resultName) }.getOrDefault(GameResult.ONGOING)
        val tc = TimeControl.byId(entity.timeControlId)

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
            sessionUuid = "saved_${entity.id}",
            savedGameId = entity.id,
            gameMode = mode,
            bot = bot,
            usingCustomEngine = isCustom,
            customEngineElo = customElo,
            customPersonality = customPersonality,
            personalityIntensity = preferences.value.personalityIntensity,
            personalityRisk = preferences.value.personalityRisk,
            humanStyle = preferences.value.humanStyleEnabled,
            customStartFen = entity.startFen.takeIf { it != ChessBoardState.INITIAL_FEN },
            editedPosition = entity.edited,
            strengthMixed = entity.strengthMixed,
            hintsUsed = entity.hintsUsed,
            assisted = entity.assisted,
            playerColor = playerColor,
            boardState = board,
            legalMoves = legal,
            recordedMoves = moves,
            evalWhiteCp = moves.lastOrNull()?.evalWhiteCp ?: 15,
            gameResult = result,
            claimableDrawReason = if (result.isOngoing) board.canClaimDraw() else null,
            flippedBoard = entity.flipped,
            botChatMessageAr = if (entity.isCompleted) "مراجعة وتحليل المباراة السابقة" else "عدنا للمباراة! دورك لإكمال الخطة.",
            postGameReport = report,
            showPostGameSheet = false,
            reviewIndex = if (entity.isCompleted && moves.isNotEmpty()) moves.lastIndex else null,
            isAdaptiveMatch = preferences.value.adaptiveMode,
            timeControl = tc,
            whiteTimeSec = entity.whiteTimeSec,
            blackTimeSec = entity.blackTimeSec,
            fullReviewCompleted = entity.reviewCompleted,
            reviewCacheKey = entity.reviewCacheKey
        )

        if (!entity.isCompleted && result == GameResult.ONGOING) {
            startClockTickerIfTimed()
            if (mode != GameMode.LOCAL_TWO_PLAYER && board.turn != playerColor) {
                triggerBotMove()
            }
        }
    }

    fun onMatchSquareClick(square: Square) {
        val st = _matchState.value
        val pref = preferences.value
        if (st.isReviewingPastMove) {
            _matchState.update { it.copy(reviewIndex = null, multiPvLines = emptyList(), previousBestMoveForReview = null) }
            recomputeThreatsIfEnabled(st.boardState)
            return
        }
        if (st.gameResult != GameResult.ONGOING) return

        // Premove support while engine/bot is thinking
        if (st.isBotThinking && st.gameMode != GameMode.LOCAL_TWO_PLAYER) {
            if (!pref.premoveEnabled || pref.unassistedMode) return
            val existingPremove = st.pendingPremove
            if (existingPremove != null && square == existingPremove.second) {
                _matchState.update { it.copy(pendingPremove = null, selectedSquare = null) }
                return
            }
            val selected = st.selectedSquare
            if (selected != null && selected != square) {
                soundManager.play(ChessSound.PREMOVE)
                _matchState.update {
                    it.copy(pendingPremove = selected to square, selectedSquare = null, legalMovesForSelected = emptyList())
                }
                return
            }
            val piece = st.boardState.pieceAt(square)
            if (piece != null && piece.color == st.playerColor) {
                _matchState.update { it.copy(selectedSquare = square, legalMovesForSelected = emptyList()) }
            } else {
                _matchState.update { it.copy(selectedSquare = null, pendingPremove = null) }
            }
            return
        }

        val allowedMoverColor = if (st.gameMode == GameMode.LOCAL_TWO_PLAYER) {
            st.boardState.turn
        } else {
            if (st.boardState.turn != st.playerColor) return
            st.playerColor
        }

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
                    executePlayerMove(matchingMoves.first(), allowedMoverColor)
                }
                return
            } else if (square != selected) {
                val tappedPiece = st.boardState.pieceAt(square)
                if (tappedPiece == null || tappedPiece.color != allowedMoverColor) {
                    soundManager.play(ChessSound.ILLEGAL)
                }
            }
        }

        val piece = st.boardState.pieceAt(square)
        if (piece != null && piece.color == allowedMoverColor) {
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
        executePlayerMove(move, st.boardState.turn)
    }

    fun dismissPromotion() {
        _matchState.update { it.copy(pendingPromotion = null) }
    }

    private fun executePlayerMove(move: ChessMove, moverColor: PieceColor) {
        val st = _matchState.value
        val beforeState = st.boardState
        val legalBefore = st.legalMoves
        val san = beforeState.toSan(move, legalBefore)
        val phase = beforeState.detectPhase()
        val afterState = beforeState.copyWithMove(move)
        val afterLegal = afterState.generateLegalMoves()
        val result = afterState.evaluateGameResult(afterLegal)
        val claimDraw = if (result.isOngoing) afterState.canClaimDraw() else null

        playMoveFeedback(move, afterState, result, st.playerColor)

        val placeholderRecord = RecordedMove(
            moveNumber = beforeState.fullMoveNumber,
            color = moverColor,
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

        val isLocal = st.gameMode == GameMode.LOCAL_TWO_PLAYER
        val nextChatMsg = if (isLocal) {
            if (afterState.turn == PieceColor.WHITE) "مباراة محلية • دور الأبيض" else "مباراة محلية • دور الأسود"
        } else st.botChatMessageAr

        _matchState.update {
            it.copy(
                boardState = afterState,
                legalMoves = afterLegal,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                recordedMoves = updatedList,
                currentHint = null,
                progressiveHint = null,
                currentHintStage = null,
                multiPvLines = emptyList(),
                pendingPremove = null,
                pauseAlertMove = null,
                gameResult = result,
                claimableDrawReason = claimDraw,
                botChatMessageAr = nextChatMsg,
                isBotThinking = result == GameResult.ONGOING && !isLocal
            )
        }
        recomputeThreatsIfEnabled(afterState)

        botThinkingJob = viewModelScope.launch {
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

            val pref = preferences.value
            val shouldPause = !pref.unassistedMode && result == GameResult.ONGOING && (
                (pref.pauseOnBlunder && evalResult.quality == MoveQuality.BLUNDER) ||
                    (pref.pauseOnMistake && evalResult.quality == MoveQuality.MISTAKE)
                )
            if (evalResult.quality == MoveQuality.BLUNDER) {
                soundManager.play(ChessSound.BLUNDER)
            }

            _matchState.update { current ->
                val list = current.recordedMoves.toMutableList()
                if (list.isNotEmpty()) {
                    list[list.lastIndex] = analyzedMove
                }
                val reaction = if (isLocal) {
                    current.botChatMessageAr
                } else {
                    when (evalResult.quality) {
                        MoveQuality.BRILLIANT, MoveQuality.BEST -> current.bot.goodMoveReactionsAr.random()
                        MoveQuality.MISTAKE, MoveQuality.BLUNDER -> current.bot.blunderReactionsAr.random()
                        else -> current.botChatMessageAr
                    }
                }
                current.copy(
                    recordedMoves = list,
                    evalWhiteCp = evalResult.evalAfterWhiteCp,
                    lastMoveQualityBadge = evalResult.quality to evalResult.coachNoteAr,
                    botChatMessageAr = reaction,
                    pauseAlertMove = if (shouldPause) analyzedMove else null,
                    isBotThinking = if (shouldPause) false else current.isBotThinking
                )
            }

            if (result != GameResult.ONGOING) {
                finalizeMatch(result)
                return@launch
            }

            saveCurrentMatchProgress()

            if (!isLocal && !shouldPause) {
                delay(200)
                performBotSearchAndMove()
            }
        }
    }

    private fun triggerBotMove() {
        if (_matchState.value.gameMode == GameMode.LOCAL_TWO_PLAYER) return
        botThinkingJob?.cancel()
        _matchState.update { it.copy(isBotThinking = true) }
        botThinkingJob = viewModelScope.launch {
            delay(280)
            performBotSearchAndMove()
        }
    }

    private suspend fun performBotSearchAndMove() {
        val st = _matchState.value
        if (st.gameResult != GameResult.ONGOING || st.gameMode == GameMode.LOCAL_TWO_PLAYER) {
            _matchState.update { it.copy(isBotThinking = false) }
            return
        }
        val stateBefore = st.boardState
        val legalBefore = st.legalMoves
        val pref = preferences.value
        val adaptiveElo = if (st.isAdaptiveMatch) pref.playerElo else null
        val appCtx = getApplication<Application>()

        val remainingClockMs: Long? = if (st.timeControl != TimeControl.UNLIMITED) {
            val sec = if (stateBefore.turn == PieceColor.WHITE) st.whiteTimeSec else st.blackTimeSec
            sec * 1000L
        } else null

        val prevBotMove = st.recordedMoves.lastOrNull { it.color == stateBefore.turn }
        val prevWing = prevBotMove?.move?.to?.col?.let {
            when {
                it <= 2 -> 0
                it >= 5 -> 2
                else -> 1
            }
        }
        val prevPiece = prevBotMove?.let { stateBefore.pieceAt(it.move.to)?.type }

        val searchResult = if (st.usingCustomEngine) {
            StockfishUciEngine.computeMove(
                context = appCtx,
                state = stateBefore,
                backendPreference = pref.engineBackendPreference,
                targetElo = st.customEngineElo,
                personality = st.customPersonality,
                humanStyle = st.humanStyle,
                moveTimeMs = pref.stockfishMoveTimeMs,
                threads = pref.stockfishThreads,
                hashMb = pref.stockfishHashMb,
                powerSavingMode = pref.powerSavingMode,
                personalityIntensity = st.personalityIntensity,
                personalityRisk = st.personalityRisk,
                sessionId = st.sessionUuid,
                remainingClockMs = remainingClockMs,
                gameSeed = st.sessionUuid.hashCode().toLong(),
                lastMoveFromWing = prevWing,
                lastMovedPieceType = prevPiece
            )
        } else {
            StockfishUciEngine.computeBotMove(
                context = appCtx,
                state = stateBefore,
                backendPreference = pref.engineBackendPreference,
                bot = st.bot,
                adaptivePlayerElo = adaptiveElo,
                moveTimeMs = pref.stockfishMoveTimeMs,
                threads = pref.stockfishThreads,
                sessionId = st.sessionUuid
            )
        }

        val nodesStr = if (searchResult.nodesSearched > 0) " • ${searchResult.nodesSearched} عقدة" else ""
        _lastEngineTelemetry.value = "${searchResult.sourceName} • عمق ${searchResult.depthReached}$nodesStr"

        val botMove = searchResult.bestMove
        if (botMove == null) {
            val res = stateBefore.evaluateGameResult(legalBefore)
            _matchState.update { it.copy(isBotThinking = false, gameResult = res, pendingPremove = null) }
            if (res != GameResult.ONGOING) finalizeMatch(res)
            return
        }

        // Check bot resign threshold (lost >= 850 cp from bot perspective for 3 consecutive bot turns)
        val botColor = stateBefore.turn
        val botEvalCp = if (botColor == PieceColor.WHITE) searchResult.evalWhiteCp else -searchResult.evalWhiteCp
        val nextLostStreak = if (botEvalCp <= -850) st.consecutiveBotLostTurns + 1 else 0
        if (pref.botResignEnabled && nextLostStreak >= 3 && legalBefore.size > 1) {
            val resignResult = if (botColor == PieceColor.WHITE) GameResult.WHITE_RESIGNED else GameResult.BLACK_RESIGNED
            playEndFeedback(resignResult, st.playerColor)
            _matchState.update {
                it.copy(
                    isBotThinking = false,
                    pendingPremove = null,
                    botChatMessageAr = "الموقف ميؤوس منه... أستسلم لك وأهنئك على الفوز!"
                )
            }
            finalizeMatch(resignResult)
            return
        }

        val san = stateBefore.toSan(botMove, legalBefore)
        val phase = stateBefore.detectPhase()
        val afterState = stateBefore.copyWithMove(botMove)
        val afterLegal = afterState.generateLegalMoves()
        val result = afterState.evaluateGameResult(afterLegal)
        val claimDraw = if (result.isOngoing) afterState.canClaimDraw() else null

        playMoveFeedback(botMove, afterState, result, st.playerColor)

        val engineLabel = if (st.usingCustomEngine) st.customPersonality.nameAr else st.bot.nameAr
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
            coachNoteAr = "نقلة $engineLabel"
        )

        val queuedPremove = _matchState.value.pendingPremove
        _matchState.update {
            it.copy(
                boardState = afterState,
                legalMoves = afterLegal,
                recordedMoves = it.recordedMoves + botRecord,
                evalWhiteCp = searchResult.evalWhiteCp,
                isBotThinking = false,
                consecutiveBotLostTurns = nextLostStreak,
                pendingPremove = null,
                gameResult = result,
                claimableDrawReason = claimDraw
            )
        }
        recomputeThreatsIfEnabled(afterState)

        if (result != GameResult.ONGOING) {
            finalizeMatch(result)
        } else {
            saveCurrentMatchProgress()
            if (queuedPremove != null && pref.premoveEnabled && afterState.turn == st.playerColor) {
                val premoveMatch = afterLegal.filter {
                    it.from == queuedPremove.first && it.to == queuedPremove.second
                }
                if (premoveMatch.isNotEmpty()) {
                    val chosen = premoveMatch.firstOrNull { it.promotion == PieceType.QUEEN } ?: premoveMatch.first()
                    executePlayerMove(chosen, st.playerColor)
                }
            }
        }
    }

    private suspend fun saveCurrentMatchProgress() {
        val st = _matchState.value
        if (st.recordedMoves.isEmpty() || st.gameResult != GameResult.ONGOING) return
        val opening = ChessBoardState.recognizeOpeningArabic(st.recordedMoves.map { it.move.toUci() })
        val id = repository.saveOngoingGame(
            existingId = st.savedGameId,
            botId = st.effectiveBotId,
            playerColor = st.playerColor,
            currentFen = st.boardState.toFen(),
            moves = st.recordedMoves,
            openingNameAr = opening,
            gameMode = st.gameMode.id,
            startFen = st.customStartFen ?: ChessBoardState.INITIAL_FEN,
            timeControlId = st.timeControl.id,
            whiteTimeSec = st.whiteTimeSec,
            blackTimeSec = st.blackTimeSec,
            hintsUsed = st.hintsUsed,
            assisted = st.assisted,
            strengthMixed = st.strengthMixed,
            edited = st.editedPosition,
            flipped = st.flippedBoard
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
            botIdOverride = st.effectiveBotId,
            playerColor = st.playerColor,
            currentFen = st.boardState.toFen(),
            moves = st.recordedMoves,
            result = result,
            report = report,
            currentElo = preferences.value.playerElo,
            solvedPuzzleCount = puzzleRecords.value.size,
            gameMode = st.gameMode.id,
            startFen = st.customStartFen ?: ChessBoardState.INITIAL_FEN,
            timeControlId = st.timeControl.id,
            whiteTimeSec = st.whiteTimeSec,
            blackTimeSec = st.blackTimeSec,
            hintsUsed = st.hintsUsed,
            assisted = st.assisted,
            strengthMixed = st.strengthMixed,
            edited = st.editedPosition,
            flipped = st.flippedBoard
        )

        val autoReview = preferences.value.autoReviewAfterGame && st.recordedMoves.isNotEmpty()
        _matchState.update {
            it.copy(
                gameResult = result,
                claimableDrawReason = null,
                isBotThinking = false,
                postGameReport = report,
                newlyUnlockedAchievements = unlocked,
                showPostGameSheet = preferences.value.autoReviewAfterGame
            )
        }
        if (autoReview) {
            startOrResumeFullReview()
        }
    }

    /**
     * Ruqaa Section 8.2: 3-stage progressive hint cached per FEN, player turn only.
     */
    fun requestHint() {
        val st = _matchState.value
        if (st.gameResult != GameResult.ONGOING || st.isBotThinking || st.isAnalyzingHint) return
        if (st.gameMode != GameMode.LOCAL_TWO_PLAYER && st.boardState.turn != st.playerColor) return

        val fenKey = st.boardState.toFen()
        val cached = st.hintCacheByFen[fenKey]
        if (cached != null) {
            val nextStage = when (st.currentHintStage) {
                null -> HintStage.CONCEPT
                HintStage.CONCEPT -> HintStage.PIECE
                HintStage.PIECE, HintStage.EXACT -> HintStage.EXACT
            }
            val displayHint = MoveHint(
                move = cached.move,
                san = cached.san,
                explanationAr = when (nextStage) {
                    HintStage.CONCEPT -> cached.conceptAr
                    HintStage.PIECE -> cached.pieceHintAr
                    HintStage.EXACT -> cached.exactMoveAr
                }
            )
            _matchState.update {
                it.copy(
                    currentHint = displayHint,
                    progressiveHint = cached,
                    currentHintStage = nextStage
                )
            }
            return
        }

        _matchState.update { it.copy(isAnalyzingHint = true) }
        val pref = preferences.value
        viewModelScope.launch {
            val search = StockfishUciEngine.queryHintMove(
                context = getApplication(),
                state = st.boardState,
                backendPreference = pref.engineBackendPreference,
                moveTimeMs = 320,
                threads = pref.stockfishThreads
            )
            val prog = withContext(Dispatchers.Default) {
                ChessCoachAnalyzer.generateProgressiveHint(st.boardState, precomputedBest = search.bestMove)
            }
            val nodesStr = if (search.nodesSearched > 0) " • ${search.nodesSearched} عقدة" else ""
            _lastEngineTelemetry.value = "${search.sourceName} • عمق ${search.depthReached}$nodesStr"

            if (prog != null) {
                val stage1Hint = MoveHint(
                    move = prog.move,
                    san = prog.san,
                    explanationAr = prog.conceptAr
                )
                _matchState.update {
                    it.copy(
                        isAnalyzingHint = false,
                        currentHint = stage1Hint,
                        progressiveHint = prog,
                        currentHintStage = HintStage.CONCEPT,
                        hintCacheByFen = it.hintCacheByFen + (fenKey to prog),
                        hintsUsed = it.hintsUsed + 1,
                        assisted = true
                    )
                }
                saveCurrentMatchProgress()
            } else {
                _matchState.update { it.copy(isAnalyzingHint = false) }
            }
        }
    }

    /**
     * Ruqaa Section 8.3: Pausable & resumable post-game review keyed by SHA-256.
     */
    fun startOrResumeFullReview() {
        val st = _matchState.value
        if (st.gameResult == GameResult.ONGOING || st.recordedMoves.isEmpty()) return
        if (st.isFullReviewRunning) return

        val engineId = _stockfishStatus.value.engineName
        val startFen = st.customStartFen ?: ChessBoardState.INITIAL_FEN
        val key = ChessCoachAnalyzer.computeReviewCacheKey(engineId, startFen, st.recordedMoves.map { it.move.toUci() })

        if (st.fullReviewCompleted && st.reviewCacheKey == key) {
            return
        }

        val resumeFrom = if (st.reviewCacheKey == key) st.fullReviewNextIndex else 0
        _matchState.update {
            it.copy(
                isFullReviewRunning = true,
                isFullReviewPaused = false,
                fullReviewNextIndex = resumeFrom.coerceIn(0, st.recordedMoves.size),
                fullReviewCompleted = false,
                reviewCacheKey = key
            )
        }

        fullReviewJob?.cancel()
        fullReviewJob = viewModelScope.launch {
            var currentBoard = ChessBoardState.fromFen(startFen)
            val total = st.recordedMoves.size
            for (idx in 0 until total) {
                val rec = _matchState.value.recordedMoves.getOrNull(idx) ?: break
                val legalBefore = currentBoard.generateLegalMoves()
                if (idx >= resumeFrom) {
                    if (_matchState.value.isFullReviewPaused) break
                    val eval = withContext(Dispatchers.Default) {
                        ChessCoachAnalyzer.analyzePlayedMove(currentBoard, rec.move, legalBefore, depth = 3)
                    }
                    val updatedMove = rec.copy(
                        evalBeforeWhiteCp = eval.evalBeforeWhiteCp,
                        evalWhiteCp = eval.evalAfterWhiteCp,
                        centipawnLoss = eval.centipawnLoss,
                        quality = eval.quality,
                        bestMoveSan = eval.bestMoveSan,
                        coachNoteAr = eval.coachNoteAr
                    )
                    _matchState.update { curr ->
                        val mutable = curr.recordedMoves.toMutableList()
                        if (idx in mutable.indices) mutable[idx] = updatedMove
                        curr.copy(
                            recordedMoves = mutable,
                            fullReviewNextIndex = idx + 1
                        )
                    }
                }
                currentBoard = currentBoard.copyWithMove(rec.move)
            }

            val finished = _matchState.value.fullReviewNextIndex >= total && !_matchState.value.isFullReviewPaused
            if (finished) {
                val updated = _matchState.value
                val refreshedReport = withContext(Dispatchers.Default) {
                    ChessCoachAnalyzer.buildPostGameReport(
                        moves = updated.recordedMoves,
                        playerColor = updated.playerColor,
                        result = updated.gameResult,
                        bot = updated.bot,
                        currentPlayerElo = preferences.value.playerElo
                    )
                }
                _matchState.update {
                    it.copy(
                        isFullReviewRunning = false,
                        fullReviewCompleted = true,
                        postGameReport = refreshedReport
                    )
                }
                updated.savedGameId?.let { gid ->
                    repository.updateCompletedReview(gid, updated.recordedMoves, refreshedReport, key, true)
                }
            } else {
                _matchState.update { it.copy(isFullReviewRunning = false) }
            }
        }
    }

    fun pauseFullReview() {
        fullReviewJob?.cancel()
        _matchState.update {
            it.copy(isFullReviewRunning = false, isFullReviewPaused = true)
        }
    }

    /**
     * MultiPV (1..5) Analysis Explorer of current displayed board position.
     */
    fun analyzeDisplayedPositionMultiPv() {
        val st = _matchState.value
        val targetState = st.displayedBoardState
        val pref = preferences.value
        if (st.isAnalyzingMultiPv) return
        multiPvJob?.cancel()
        _matchState.update { it.copy(isAnalyzingMultiPv = true) }
        multiPvJob = viewModelScope.launch {
            val lines = StockfishUciEngine.analyzeMultiPv(
                context = getApplication(),
                state = targetState,
                backendPreference = pref.engineBackendPreference,
                multiPv = pref.analysisMultiPvCount.coerceIn(1, 5),
                moveTimeMs = 1700,
                threads = pref.stockfishThreads,
                hashMb = pref.stockfishHashMb,
                powerSavingMode = pref.powerSavingMode,
                sessionId = "analysis_${st.sessionUuid}"
            )
            _matchState.update {
                it.copy(
                    isAnalyzingMultiPv = false,
                    multiPvLines = lines
                )
            }
        }
    }

    fun forceReanalyzeGame() {
        fullReviewJob?.cancel()
        _matchState.update {
            it.copy(
                isFullReviewRunning = false,
                isFullReviewPaused = false,
                fullReviewNextIndex = 0,
                fullReviewCompleted = false
            )
        }
        startOrResumeFullReview()
    }

    fun toggleMatchReportVisibility() {
        _matchState.update { it.copy(showPostGameReportBar = !it.showPostGameReportBar) }
    }

    fun dismissPauseAlert() {
        val st = _matchState.value
        _matchState.update { it.copy(pauseAlertMove = null) }
        if (st.gameResult == GameResult.ONGOING && st.gameMode != GameMode.LOCAL_TWO_PLAYER && st.boardState.turn != st.playerColor) {
            triggerBotMove()
        }
    }

    fun undoFromPauseAlert() {
        _matchState.update { it.copy(pauseAlertMove = null) }
        undoLastTurn()
    }

    fun playFromReviewPosition() {
        val st = _matchState.value
        val idx = st.reviewIndex
        val truncatedMoves = if (idx != null && idx in st.recordedMoves.indices) {
            st.recordedMoves.take(idx + 1)
        } else {
            st.recordedMoves
        }
        val board = if (truncatedMoves.isNotEmpty()) {
            ChessBoardState.fromFen(truncatedMoves.last().fenAfter)
        } else {
            st.customStartFen?.let { ChessBoardState.fromFen(it) } ?: ChessBoardState.initial()
        }
        val legal = board.generateLegalMoves()
        val result = board.evaluateGameResult(legal)
        _matchState.update {
            it.copy(
                sessionUuid = UUID.randomUUID().toString(),
                savedGameId = null,
                boardState = board,
                legalMoves = legal,
                recordedMoves = truncatedMoves,
                reviewIndex = null,
                previousBestMoveForReview = null,
                multiPvLines = emptyList(),
                gameResult = result,
                claimableDrawReason = if (result.isOngoing) board.canClaimDraw() else null,
                postGameReport = null,
                showPostGameSheet = false,
                botChatMessageAr = "تم الاستكمال من هذه الوضعية"
            )
        }
        recomputeThreatsIfEnabled(board)
        if (result == GameResult.ONGOING && st.gameMode != GameMode.LOCAL_TWO_PLAYER && board.turn != st.playerColor) {
            triggerBotMove()
        }
    }

    private fun recomputeThreatsIfEnabled(board: ChessBoardState) {
        if (!preferences.value.showThreats) {
            if (_matchState.value.threatArrows.isNotEmpty()) {
                _matchState.update { it.copy(threatArrows = emptyList()) }
            }
            return
        }
        recomputeThreatsAsync(board)
    }

    private fun recomputeThreatsAsync(board: ChessBoardState) {
        viewModelScope.launch {
            val detected = withContext(Dispatchers.Default) {
                ChessCoachAnalyzer.detectThreats(board)
            }
            _matchState.update { it.copy(threatArrows = detected) }
        }
    }

    fun claimDraw() {
        val st = _matchState.value
        if (st.gameResult != GameResult.ONGOING || st.claimableDrawReason == null) return
        botThinkingJob?.cancel()
        clockJob?.cancel()
        soundManager.play(ChessSound.DRAW)
        hapticsManager.trigger(ChessHapticEvent.GAME_END)
        viewModelScope.launch {
            finalizeMatch(GameResult.DRAW_CLAIMED)
        }
    }

    fun undoLastTurn() {
        val st = _matchState.value
        if (st.isBotThinking || st.gameResult != GameResult.ONGOING) return
        val moves = st.recordedMoves
        if (moves.isEmpty()) return

        var dropCount = if (st.gameMode == GameMode.LOCAL_TWO_PLAYER) {
            1
        } else {
            if (moves.last().color != st.playerColor) 2 else 1
        }
        if (moves.size < dropCount) dropCount = moves.size
        val remaining = moves.dropLast(dropCount)

        val targetBoard = if (remaining.isEmpty()) {
            st.customStartFen?.let { ChessBoardState.fromFen(it) } ?: ChessBoardState.initial()
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
                progressiveHint = null,
                currentHintStage = null,
                multiPvLines = emptyList(),
                pendingPremove = null,
                pauseAlertMove = null,
                claimableDrawReason = targetBoard.canClaimDraw(),
                reviewIndex = null,
                previousBestMoveForReview = null,
                evalWhiteCp = remaining.lastOrNull()?.evalWhiteCp ?: 15,
                lastMoveQualityBadge = null
            )
        }
        recomputeThreatsIfEnabled(targetBoard)
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
        clockJob?.cancel()
        val resigningColor = if (st.gameMode == GameMode.LOCAL_TWO_PLAYER) st.boardState.turn else st.playerColor
        val res = if (resigningColor == PieceColor.WHITE) GameResult.WHITE_RESIGNED else GameResult.BLACK_RESIGNED
        playEndFeedback(res, st.playerColor)
        viewModelScope.launch {
            finalizeMatch(res)
        }
    }

    fun selectReviewMove(index: Int?) {
        val st = _matchState.value
        val prevBestMove = if (index != null && index in st.recordedMoves.indices) {
            val rec = st.recordedMoves[index]
            if (rec.bestMoveSan.isNotBlank()) {
                val fenBefore = if (index == 0) {
                    st.customStartFen ?: ChessBoardState.INITIAL_FEN
                } else {
                    st.recordedMoves[index - 1].fenAfter
                }
                val boardBefore = ChessBoardState.fromFen(fenBefore)
                val legal = boardBefore.generateLegalMoves()
                legal.find { boardBefore.toSan(it, legal) == rec.bestMoveSan }
            } else null
        } else null

        _matchState.update {
            it.copy(
                reviewIndex = index,
                multiPvLines = emptyList(),
                previousBestMoveForReview = prevBestMove
            )
        }
        val nextBoard = if (index != null && index in st.recordedMoves.indices) {
            ChessBoardState.fromFen(st.recordedMoves[index].fenAfter)
        } else {
            st.boardState
        }
        recomputeThreatsIfEnabled(nextBoard)
    }

    fun openMoveLedgerSheet() {
        _matchState.update { it.copy(showMoveLedgerSheet = true) }
    }

    fun dismissMoveLedgerSheet() {
        _matchState.update { it.copy(showMoveLedgerSheet = false) }
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
            val expectedUci = st.puzzle.solutionUci.getOrNull(st.stepIndex)
            val candidates = st.legalMovesForSelected.filter { it.to == square }
            val move = candidates.firstOrNull { it.toUci() == expectedUci }
                ?: candidates.firstOrNull { it.promotion == PieceType.QUEEN }
                ?: candidates.firstOrNull()
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

        val playedUci = move.toUci()
        val matches = playedUci == expectedUci ||
            (expectedUci.length == 5 && playedUci.startsWith(expectedUci.substring(0, 4)) && move.promotion == PieceType.QUEEN)

        if (!matches) {
            soundManager.play(ChessSound.DEFEAT)
            hapticsManager.trigger(ChessHapticEvent.CAPTURE)
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

        val afterPlayer = st.boardState.copyWithMove(move)
        val nextStep = st.stepIndex + 1

        if (nextStep >= st.puzzle.solutionUci.size) {
            soundManager.play(ChessSound.PUZZLE_SOLVED)
            hapticsManager.trigger(ChessHapticEvent.GAME_END)
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

        soundManager.play(ChessSound.MOVE)
        hapticsManager.trigger(ChessHapticEvent.MOVE)
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
            hapticsManager.trigger(if (oppMove.capturedPiece != null) ChessHapticEvent.CAPTURE else ChessHapticEvent.MOVE)
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

    private fun playMoveFeedback(
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
                hapticsManager.trigger(ChessHapticEvent.GAME_END)
            }
            !result.isOngoing -> {
                soundManager.play(ChessSound.DRAW)
                hapticsManager.trigger(ChessHapticEvent.GAME_END)
            }
            move.promotion != null -> {
                soundManager.play(ChessSound.PROMOTION)
                hapticsManager.trigger(ChessHapticEvent.CAPTURE)
            }
            afterState.isInCheck(afterState.turn) -> {
                soundManager.play(ChessSound.CHECK)
                hapticsManager.trigger(ChessHapticEvent.CHECK)
            }
            move.isCastleKingside || move.isCastleQueenside -> {
                soundManager.play(ChessSound.CASTLE)
                hapticsManager.trigger(ChessHapticEvent.CASTLE)
            }
            move.capturedPiece != null || move.isEnPassant -> {
                soundManager.play(ChessSound.CAPTURE)
                hapticsManager.trigger(ChessHapticEvent.CAPTURE)
            }
            else -> {
                soundManager.play(ChessSound.MOVE)
                hapticsManager.trigger(ChessHapticEvent.MOVE)
            }
        }
    }

    private fun playEndFeedback(result: GameResult, playerColor: PieceColor) {
        val won = (result == GameResult.BLACK_RESIGNED || result == GameResult.BLACK_TIMEOUT) && playerColor == PieceColor.WHITE ||
            (result == GameResult.WHITE_RESIGNED || result == GameResult.WHITE_TIMEOUT) && playerColor == PieceColor.BLACK
        soundManager.play(if (won) ChessSound.VICTORY else ChessSound.DEFEAT)
        hapticsManager.trigger(ChessHapticEvent.GAME_END)
    }

    override fun onCleared() {
        super.onCleared()
        cancelBackgroundJobs()
    }
}
