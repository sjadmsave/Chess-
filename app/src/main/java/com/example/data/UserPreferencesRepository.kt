package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.BoardMaterial
import com.example.chess.BoardTheme
import com.example.chess.EngineBackendPreference
import com.example.chess.EnginePersonality
import com.example.chess.EvalBarPosition
import com.example.chess.FloatingBubbleSize
import com.example.chess.FloatingFrameShape
import com.example.chess.LastMoveHighlightStyle
import com.example.chess.PersonalityRisk
import com.example.chess.PieceAnimationStyle
import com.example.chess.PieceStyle
import com.example.chess.SoundProfile
import com.example.chess.StrengthTier
import com.example.chess.VibrationStyle
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ruqaa_chess_prefs")

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD,
    val boardMaterial: BoardMaterial = BoardMaterial.FLAT,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    val soundEnabled: Boolean = false,
    val soundProfile: SoundProfile = SoundProfile.SOFT,
    val soundVolume: Int = 55,
    val hapticsEnabled: Boolean = false,
    val vibrationStyle: VibrationStyle = VibrationStyle.LIGHT,
    val showEvalBar: Boolean = true,
    val showLastMove: Boolean = true,
    val showLegalMoves: Boolean = true,
    val useRingsInsteadOfDots: Boolean = false,
    val selectedColorHex: Long = 0xAA006A87L,
    val lastMoveColorHex: Long = 0x7772D9EFL,
    val legalMoveColorHex: Long = 0xCC72D9EFL,
    val adaptiveMode: Boolean = false,
    val strengthPreset: StrengthTier = StrengthTier.EASY,
    val customEngineElo: Int = -1,
    val humanStyleEnabled: Boolean = true,
    val enginePersonality: EnginePersonality = EnginePersonality.CLASSIC,
    val personalityIntensity: Int = 50,
    val personalityRisk: PersonalityRisk = PersonalityRisk.BALANCED,
    val engineBackendPreference: EngineBackendPreference = EngineBackendPreference.STOCKFISH_AUTO,
    val stockfishMoveTimeMs: Int = 900,
    val stockfishThreads: Int = 2,
    val stockfishHashMb: Int = 32,
    val powerSavingMode: Boolean = false,
    val autoReviewAfterGame: Boolean = true,
    val floatingAutoShow: Boolean = true,
    val floatingWidthDp: Int = 270,
    val floatingFrameShape: FloatingFrameShape = FloatingFrameShape.ROUNDED,
    val floatingOpacityPercent: Int = 100,
    val floatingBubbleSize: FloatingBubbleSize = FloatingBubbleSize.MEDIUM,
    val floatingX: Int = 12,
    val floatingY: Int = 72,
    val playerElo: Int = 1000,
    val xpPoints: Int = 0,
    val dailyStreak: Int = 0,
    val lastDailySolvedDate: String = "",
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val selectedBotId: String = "fischer",
    // Game settings (Chessis Pro parity)
    val premoveEnabled: Boolean = true,
    val unassistedMode: Boolean = false,
    val showThreats: Boolean = false,
    val botResignEnabled: Boolean = false,
    val pauseOnBlunder: Boolean = true,
    val pauseOnMistake: Boolean = false,
    val showMyMoveStrength: Boolean = true,
    val showOpponentMoveStrength: Boolean = true,
    // Analysis & Board customization settings
    val evalBarPosition: EvalBarPosition = EvalBarPosition.SMART,
    val showEngineArrowsOnBoard: Boolean = true,
    val analysisMultiPvCount: Int = 3,
    val showMultiPvArrowNumbers: Boolean = true,
    val arrowStrengthColors: Boolean = true,
    val showPreviousBestGhostArrow: Boolean = false,
    val colorLastMoveByQuality: Boolean = true,
    val figurineNotation: Boolean = true,
    val showCapturedPieces: Boolean = true,
    val showCoordinates: Boolean = true,
    val lastMoveHighlightStyle: LastMoveHighlightStyle = LastMoveHighlightStyle.FILL,
    val pieceAnimationStyle: PieceAnimationStyle = PieceAnimationStyle.DEFAULT,
    val tapOnlyDisableDrag: Boolean = false,
    val enlargeOnDrag: Boolean = true,
    val showAverageCplInReport: Boolean = true,
    val boardScalePercent: Int = 100
) {
    val effectiveEngineElo: Int
        get() = if (customEngineElo == -1) strengthPreset.targetElo else customEngineElo

    val effectiveStrengthTier: StrengthTier
        get() = if (customEngineElo == -1) strengthPreset else StrengthTier.fromElo(customEngineElo)
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val BOARD_MATERIAL = stringPreferencesKey("board_material")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound")
        val SOUND_PROFILE = stringPreferencesKey("sound_profile")
        val SOUND_VOLUME = intPreferencesKey("sound_volume")
        val HAPTICS_ENABLED = booleanPreferencesKey("vibrate")
        val VIBRATION_STYLE = intPreferencesKey("vibration_style")
        val SHOW_EVAL_BAR = booleanPreferencesKey("show_eval_bar")
        val SHOW_LAST_MOVE = booleanPreferencesKey("mark_last_visible")
        val SHOW_LEGAL_MOVES = booleanPreferencesKey("mark_legal_visible")
        val USE_RINGS = booleanPreferencesKey("mark_rings")
        val SELECTED_COLOR_HEX = longPreferencesKey("selected_color_hex")
        val LAST_MOVE_COLOR_HEX = longPreferencesKey("last_move_color_hex")
        val LEGAL_MOVE_COLOR_HEX = longPreferencesKey("legal_move_color_hex")
        val ADAPTIVE_MODE = booleanPreferencesKey("adaptive_mode")
        val STRENGTH_PRESET = stringPreferencesKey("strength_preset")
        val CUSTOM_ENGINE_ELO = intPreferencesKey("engine_elo")
        val HUMAN_STYLE = booleanPreferencesKey("human_style")
        val ENGINE_PERSONALITY = stringPreferencesKey("personality")
        val PERSONALITY_INTENSITY = intPreferencesKey("personality_intensity")
        val PERSONALITY_RISK = stringPreferencesKey("personality_risk")
        val ENGINE_BACKEND = stringPreferencesKey("engine_backend_preference")
        val STOCKFISH_MOVETIME_MS = intPreferencesKey("stockfish_movetime_ms")
        val STOCKFISH_THREADS = intPreferencesKey("stockfish_threads")
        val STOCKFISH_HASH_MB = intPreferencesKey("stockfish_hash_mb")
        val POWER_SAVING_MODE = booleanPreferencesKey("power_saving_mode")
        val AUTO_REVIEW = booleanPreferencesKey("auto_review")
        val FLOATING_AUTO_SHOW = booleanPreferencesKey("auto_overlay")
        val FLOATING_WIDTH_DP = intPreferencesKey("overlay_width")
        val FLOATING_FRAME_SHAPE = stringPreferencesKey("floating_frame_shape")
        val FLOATING_OPACITY = intPreferencesKey("overlay_opacity")
        val FLOATING_BUBBLE_SIZE = stringPreferencesKey("floating_bubble_size")
        val FLOATING_X = intPreferencesKey("overlay_x")
        val FLOATING_Y = intPreferencesKey("overlay_y")
        val PLAYER_ELO = intPreferencesKey("player_elo")
        val XP_POINTS = intPreferencesKey("xp_points")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SELECTED_BOT_ID = stringPreferencesKey("selected_bot_id")
        // Game settings
        val PREMOVE_ENABLED = booleanPreferencesKey("premove_enabled")
        val UNASSISTED_MODE = booleanPreferencesKey("unassisted_mode")
        val SHOW_THREATS = booleanPreferencesKey("show_threats")
        val BOT_RESIGN_ENABLED = booleanPreferencesKey("bot_resign_enabled")
        val PAUSE_ON_BLUNDER = booleanPreferencesKey("pause_on_blunder")
        val PAUSE_ON_MISTAKE = booleanPreferencesKey("pause_on_mistake")
        val SHOW_MY_MOVE_STRENGTH = booleanPreferencesKey("show_my_move_strength")
        val SHOW_OPP_MOVE_STRENGTH = booleanPreferencesKey("show_opp_move_strength")
        // Analysis & Board Appearance
        val EVAL_BAR_POSITION = stringPreferencesKey("eval_bar_position")
        val SHOW_ENGINE_ARROWS = booleanPreferencesKey("show_engine_arrows")
        val ANALYSIS_MULTIPV = intPreferencesKey("analysis_multipv")
        val SHOW_MULTIPV_NUMBERS = booleanPreferencesKey("show_multipv_numbers")
        val ARROW_STRENGTH_COLORS = booleanPreferencesKey("arrow_strength_colors")
        val SHOW_PREV_BEST_GHOST = booleanPreferencesKey("show_prev_best_ghost")
        val COLOR_LAST_MOVE_QUALITY = booleanPreferencesKey("color_last_move_quality")
        val FIGURINE_NOTATION = booleanPreferencesKey("figurine_notation")
        val SHOW_CAPTURED_PIECES = booleanPreferencesKey("show_captured_pieces")
        val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        val LAST_MOVE_HIGHLIGHT_STYLE = stringPreferencesKey("last_move_highlight_style")
        val PIECE_ANIMATION_STYLE = stringPreferencesKey("piece_animation_style")
        val TAP_ONLY_DISABLE_DRAG = booleanPreferencesKey("tap_only_disable_drag")
        val ENLARGE_ON_DRAG = booleanPreferencesKey("enlarge_on_drag")
        val SHOW_AVG_CPL_REPORT = booleanPreferencesKey("show_avg_cpl_report")
        val BOARD_SCALE_PERCENT = intPreferencesKey("board_scale_percent")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            themeMode = runCatching {
                AppThemeMode.valueOf(p[Keys.THEME_MODE] ?: AppThemeMode.DARK.name)
            }.getOrDefault(AppThemeMode.DARK),
            boardTheme = BoardTheme.byId(p[Keys.BOARD_THEME] ?: BoardTheme.TOURNAMENT_EMERALD.id),
            boardMaterial = BoardMaterial.byId(p[Keys.BOARD_MATERIAL] ?: BoardMaterial.FLAT.id),
            pieceStyle = PieceStyle.byId(p[Keys.PIECE_STYLE] ?: PieceStyle.CLASSIC_STAUNTON.id),
            soundEnabled = p[Keys.SOUND_ENABLED] ?: false,
            soundProfile = SoundProfile.byId(p[Keys.SOUND_PROFILE] ?: SoundProfile.SOFT.id),
            soundVolume = (p[Keys.SOUND_VOLUME] ?: 55).coerceIn(0, 100),
            hapticsEnabled = p[Keys.HAPTICS_ENABLED] ?: false,
            vibrationStyle = VibrationStyle.byLevel(p[Keys.VIBRATION_STYLE] ?: 0),
            showEvalBar = p[Keys.SHOW_EVAL_BAR] ?: true,
            showLastMove = p[Keys.SHOW_LAST_MOVE] ?: true,
            showLegalMoves = p[Keys.SHOW_LEGAL_MOVES] ?: true,
            useRingsInsteadOfDots = p[Keys.USE_RINGS] ?: false,
            selectedColorHex = p[Keys.SELECTED_COLOR_HEX] ?: 0xAA006A87L,
            lastMoveColorHex = p[Keys.LAST_MOVE_COLOR_HEX] ?: 0x7772D9EFL,
            legalMoveColorHex = p[Keys.LEGAL_MOVE_COLOR_HEX] ?: 0xCC72D9EFL,
            adaptiveMode = p[Keys.ADAPTIVE_MODE] ?: false,
            strengthPreset = StrengthTier.byId(p[Keys.STRENGTH_PRESET] ?: StrengthTier.EASY.id),
            customEngineElo = p[Keys.CUSTOM_ENGINE_ELO] ?: -1,
            humanStyleEnabled = p[Keys.HUMAN_STYLE] ?: true,
            enginePersonality = EnginePersonality.byId(p[Keys.ENGINE_PERSONALITY] ?: EnginePersonality.CLASSIC.id),
            personalityIntensity = (p[Keys.PERSONALITY_INTENSITY] ?: 50).coerceIn(0, 100),
            personalityRisk = PersonalityRisk.byId(p[Keys.PERSONALITY_RISK] ?: PersonalityRisk.BALANCED.id),
            engineBackendPreference = EngineBackendPreference.byId(p[Keys.ENGINE_BACKEND] ?: EngineBackendPreference.STOCKFISH_AUTO.id),
            stockfishMoveTimeMs = p[Keys.STOCKFISH_MOVETIME_MS] ?: 900,
            stockfishThreads = (p[Keys.STOCKFISH_THREADS] ?: 2).coerceIn(1, 4),
            stockfishHashMb = (p[Keys.STOCKFISH_HASH_MB] ?: 32).coerceIn(16, 256),
            powerSavingMode = p[Keys.POWER_SAVING_MODE] ?: false,
            autoReviewAfterGame = p[Keys.AUTO_REVIEW] ?: true,
            floatingAutoShow = p[Keys.FLOATING_AUTO_SHOW] ?: true,
            floatingWidthDp = (p[Keys.FLOATING_WIDTH_DP] ?: 270).coerceIn(180, 560),
            floatingFrameShape = FloatingFrameShape.byId(p[Keys.FLOATING_FRAME_SHAPE] ?: FloatingFrameShape.ROUNDED.id),
            floatingOpacityPercent = (p[Keys.FLOATING_OPACITY] ?: 100).coerceIn(65, 100),
            floatingBubbleSize = FloatingBubbleSize.byId(p[Keys.FLOATING_BUBBLE_SIZE] ?: FloatingBubbleSize.MEDIUM.id),
            floatingX = p[Keys.FLOATING_X] ?: 12,
            floatingY = p[Keys.FLOATING_Y] ?: 72,
            playerElo = p[Keys.PLAYER_ELO] ?: 1000,
            xpPoints = p[Keys.XP_POINTS] ?: 0,
            dailyStreak = p[Keys.DAILY_STREAK] ?: 0,
            lastDailySolvedDate = p[Keys.LAST_DAILY_DATE] ?: "",
            remindersEnabled = p[Keys.REMINDERS_ENABLED] ?: false,
            reminderHour = p[Keys.REMINDER_HOUR] ?: 19,
            reminderMinute = p[Keys.REMINDER_MINUTE] ?: 0,
            selectedBotId = p[Keys.SELECTED_BOT_ID] ?: "fischer",
            premoveEnabled = p[Keys.PREMOVE_ENABLED] ?: true,
            unassistedMode = p[Keys.UNASSISTED_MODE] ?: false,
            showThreats = p[Keys.SHOW_THREATS] ?: false,
            botResignEnabled = p[Keys.BOT_RESIGN_ENABLED] ?: false,
            pauseOnBlunder = p[Keys.PAUSE_ON_BLUNDER] ?: true,
            pauseOnMistake = p[Keys.PAUSE_ON_MISTAKE] ?: false,
            showMyMoveStrength = p[Keys.SHOW_MY_MOVE_STRENGTH] ?: true,
            showOpponentMoveStrength = p[Keys.SHOW_OPP_MOVE_STRENGTH] ?: true,
            evalBarPosition = EvalBarPosition.byId(p[Keys.EVAL_BAR_POSITION] ?: EvalBarPosition.SMART.id),
            showEngineArrowsOnBoard = p[Keys.SHOW_ENGINE_ARROWS] ?: true,
            analysisMultiPvCount = (p[Keys.ANALYSIS_MULTIPV] ?: 3).coerceIn(1, 5),
            showMultiPvArrowNumbers = p[Keys.SHOW_MULTIPV_NUMBERS] ?: true,
            arrowStrengthColors = p[Keys.ARROW_STRENGTH_COLORS] ?: true,
            showPreviousBestGhostArrow = p[Keys.SHOW_PREV_BEST_GHOST] ?: false,
            colorLastMoveByQuality = p[Keys.COLOR_LAST_MOVE_QUALITY] ?: true,
            figurineNotation = p[Keys.FIGURINE_NOTATION] ?: true,
            showCapturedPieces = p[Keys.SHOW_CAPTURED_PIECES] ?: true,
            showCoordinates = p[Keys.SHOW_COORDINATES] ?: true,
            lastMoveHighlightStyle = LastMoveHighlightStyle.byId(
                p[Keys.LAST_MOVE_HIGHLIGHT_STYLE] ?: LastMoveHighlightStyle.FILL.id
            ),
            pieceAnimationStyle = PieceAnimationStyle.byId(
                p[Keys.PIECE_ANIMATION_STYLE] ?: PieceAnimationStyle.DEFAULT.id
            ),
            tapOnlyDisableDrag = p[Keys.TAP_ONLY_DISABLE_DRAG] ?: false,
            enlargeOnDrag = p[Keys.ENLARGE_ON_DRAG] ?: true,
            showAverageCplInReport = p[Keys.SHOW_AVG_CPL_REPORT] ?: true,
            boardScalePercent = (p[Keys.BOARD_SCALE_PERCENT] ?: 100).coerceIn(85, 100)
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        context.dataStore.edit { it[Keys.BOARD_THEME] = theme.id }
    }

    suspend fun setPieceStyle(style: PieceStyle) {
        context.dataStore.edit { it[Keys.PIECE_STYLE] = style.id }
    }

    suspend fun saveBoardAppearance(
        pieceStyle: PieceStyle,
        boardMaterial: BoardMaterial,
        selectedColorHex: Long,
        lastMoveColorHex: Long,
        legalMoveColorHex: Long,
        showLastMove: Boolean,
        showLegalMoves: Boolean,
        useRingsInsteadOfDots: Boolean,
        lastMoveHighlightStyle: LastMoveHighlightStyle = LastMoveHighlightStyle.FILL,
        pieceAnimationStyle: PieceAnimationStyle = PieceAnimationStyle.DEFAULT,
        showCoordinates: Boolean = true,
        boardTheme: BoardTheme? = null
    ) {
        context.dataStore.edit {
            it[Keys.PIECE_STYLE] = pieceStyle.id
            it[Keys.BOARD_MATERIAL] = boardMaterial.id
            it[Keys.SELECTED_COLOR_HEX] = selectedColorHex
            it[Keys.LAST_MOVE_COLOR_HEX] = lastMoveColorHex
            it[Keys.LEGAL_MOVE_COLOR_HEX] = legalMoveColorHex
            it[Keys.SHOW_LAST_MOVE] = showLastMove
            it[Keys.SHOW_LEGAL_MOVES] = showLegalMoves
            it[Keys.USE_RINGS] = useRingsInsteadOfDots
            it[Keys.LAST_MOVE_HIGHLIGHT_STYLE] = lastMoveHighlightStyle.id
            it[Keys.PIECE_ANIMATION_STYLE] = pieceAnimationStyle.id
            it[Keys.SHOW_COORDINATES] = showCoordinates
            if (boardTheme != null) {
                it[Keys.BOARD_THEME] = boardTheme.id
            }
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setSoundProfile(profile: SoundProfile) {
        context.dataStore.edit { it[Keys.SOUND_PROFILE] = profile.id }
    }

    suspend fun setSoundVolume(volume: Int) {
        context.dataStore.edit { it[Keys.SOUND_VOLUME] = volume.coerceIn(0, 100) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setVibrationStyle(style: VibrationStyle) {
        context.dataStore.edit { it[Keys.VIBRATION_STYLE] = style.level }
    }

    suspend fun setShowEvalBar(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_EVAL_BAR] = show }
    }

    suspend fun setAdaptiveMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADAPTIVE_MODE] = enabled }
    }

    suspend fun setStrengthTier(tier: StrengthTier) {
        context.dataStore.edit {
            it[Keys.STRENGTH_PRESET] = tier.id
            it[Keys.CUSTOM_ENGINE_ELO] = -1
        }
    }

    suspend fun setCustomEngineElo(elo: Int) {
        val snapped = StrengthTier.snapElo(elo)
        context.dataStore.edit {
            it[Keys.CUSTOM_ENGINE_ELO] = snapped
            it[Keys.STRENGTH_PRESET] = StrengthTier.fromElo(snapped).id
        }
    }

    suspend fun setHumanStyleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HUMAN_STYLE] = enabled }
    }

    suspend fun setEnginePersonality(personality: EnginePersonality) {
        context.dataStore.edit { it[Keys.ENGINE_PERSONALITY] = personality.id }
    }

    suspend fun setPersonalityIntensity(intensity: Int) {
        context.dataStore.edit { it[Keys.PERSONALITY_INTENSITY] = intensity.coerceIn(0, 100) }
    }

    suspend fun setPersonalityRisk(risk: PersonalityRisk) {
        context.dataStore.edit { it[Keys.PERSONALITY_RISK] = risk.id }
    }

    suspend fun setEngineBackendPreference(backend: EngineBackendPreference) {
        context.dataStore.edit { it[Keys.ENGINE_BACKEND] = backend.id }
    }

    suspend fun setStockfishMoveTimeMs(ms: Int) {
        context.dataStore.edit { it[Keys.STOCKFISH_MOVETIME_MS] = ms.coerceIn(200, 4000) }
    }

    suspend fun setStockfishThreads(threads: Int) {
        context.dataStore.edit { it[Keys.STOCKFISH_THREADS] = threads.coerceIn(1, 4) }
    }

    suspend fun setStockfishHashMb(hashMb: Int) {
        context.dataStore.edit { it[Keys.STOCKFISH_HASH_MB] = hashMb.coerceIn(16, 256) }
    }

    suspend fun setPowerSavingMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.POWER_SAVING_MODE] = enabled }
    }

    suspend fun setAutoReviewAfterGame(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_REVIEW] = enabled }
    }

    suspend fun setFloatingAutoShow(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_AUTO_SHOW] = enabled }
    }

    suspend fun saveFloatingPosition(xDp: Int, yDp: Int) {
        context.dataStore.edit {
            it[Keys.FLOATING_X] = xDp
            it[Keys.FLOATING_Y] = yDp
        }
    }

    suspend fun saveFloatingCustomization(
        widthDp: Int,
        frameShape: FloatingFrameShape,
        opacityPercent: Int,
        bubbleSize: FloatingBubbleSize
    ) {
        context.dataStore.edit {
            it[Keys.FLOATING_WIDTH_DP] = widthDp.coerceIn(180, 560)
            it[Keys.FLOATING_FRAME_SHAPE] = frameShape.id
            it[Keys.FLOATING_OPACITY] = opacityPercent.coerceIn(65, 100)
            it[Keys.FLOATING_BUBBLE_SIZE] = bubbleSize.id
        }
    }

    suspend fun setSelectedBotId(botId: String) {
        context.dataStore.edit { it[Keys.SELECTED_BOT_ID] = botId }
    }

    suspend fun setReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDERS_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateRatingAndXp(newElo: Int, addXp: Int) {
        context.dataStore.edit {
            it[Keys.PLAYER_ELO] = newElo.coerceIn(400, 2800)
            it[Keys.XP_POINTS] = (it[Keys.XP_POINTS] ?: 0) + addXp
        }
    }

    suspend fun markDailyPuzzleSolved(todayKey: String, yesterdayKey: String, bonusXp: Int) {
        context.dataStore.edit {
            val prevDate = it[Keys.LAST_DAILY_DATE] ?: ""
            if (prevDate != todayKey) {
                val prevStreak = it[Keys.DAILY_STREAK] ?: 0
                val newStreak = if (prevDate == yesterdayKey) prevStreak + 1 else 1
                it[Keys.DAILY_STREAK] = newStreak
                it[Keys.LAST_DAILY_DATE] = todayKey
                it[Keys.XP_POINTS] = (it[Keys.XP_POINTS] ?: 0) + bonusXp
            }
        }
    }

    suspend fun addXp(xp: Int) {
        context.dataStore.edit {
            it[Keys.XP_POINTS] = (it[Keys.XP_POINTS] ?: 0) + xp
        }
    }

    suspend fun setPremoveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREMOVE_ENABLED] = enabled }
    }

    suspend fun setUnassistedMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.UNASSISTED_MODE] = enabled }
    }

    suspend fun setShowThreats(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_THREATS] = enabled }
    }

    suspend fun setBotResignEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BOT_RESIGN_ENABLED] = enabled }
    }

    suspend fun setPauseOnBlunder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_ON_BLUNDER] = enabled }
    }

    suspend fun setPauseOnMistake(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PAUSE_ON_MISTAKE] = enabled }
    }

    suspend fun setShowMyMoveStrength(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_MY_MOVE_STRENGTH] = enabled }
    }

    suspend fun setShowOpponentMoveStrength(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_OPP_MOVE_STRENGTH] = enabled }
    }

    suspend fun setEvalBarPosition(position: EvalBarPosition) {
        context.dataStore.edit { it[Keys.EVAL_BAR_POSITION] = position.id }
    }

    suspend fun setShowEngineArrowsOnBoard(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ENGINE_ARROWS] = enabled }
    }

    suspend fun setAnalysisMultiPvCount(count: Int) {
        context.dataStore.edit { it[Keys.ANALYSIS_MULTIPV] = count.coerceIn(1, 5) }
    }

    suspend fun setShowMultiPvArrowNumbers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_MULTIPV_NUMBERS] = enabled }
    }

    suspend fun setArrowStrengthColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ARROW_STRENGTH_COLORS] = enabled }
    }

    suspend fun setShowPreviousBestGhostArrow(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PREV_BEST_GHOST] = enabled }
    }

    suspend fun setColorLastMoveByQuality(enabled: Boolean) {
        context.dataStore.edit { it[Keys.COLOR_LAST_MOVE_QUALITY] = enabled }
    }

    suspend fun setFigurineNotation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FIGURINE_NOTATION] = enabled }
    }

    suspend fun setShowCapturedPieces(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_CAPTURED_PIECES] = enabled }
    }

    suspend fun setShowCoordinates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_COORDINATES] = enabled }
    }

    suspend fun setTapOnlyDisableDrag(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TAP_ONLY_DISABLE_DRAG] = enabled }
    }

    suspend fun setEnlargeOnDrag(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENLARGE_ON_DRAG] = enabled }
    }

    suspend fun setShowAverageCplInReport(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_AVG_CPL_REPORT] = enabled }
    }

    suspend fun setBoardScalePercent(scale: Int) {
        context.dataStore.edit { it[Keys.BOARD_SCALE_PERCENT] = scale.coerceIn(85, 100) }
    }
}
