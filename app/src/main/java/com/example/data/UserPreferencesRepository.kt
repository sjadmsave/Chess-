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
import com.example.chess.FloatingBubbleSize
import com.example.chess.FloatingFrameShape
import com.example.chess.PieceStyle
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elite_chess_prefs")

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD,
    val boardMaterial: BoardMaterial = BoardMaterial.FLAT,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showEvalBar: Boolean = true,
    val showLastMove: Boolean = true,
    val showLegalMoves: Boolean = true,
    val useRingsInsteadOfDots: Boolean = true,
    val selectedColorHex: Long = 0xAA0D9488L,
    val lastMoveColorHex: Long = 0x77769656L,
    val legalMoveColorHex: Long = 0xCC0D9488L,
    val adaptiveMode: Boolean = false,
    val customEngineElo: Int = 1800,
    val humanStyleEnabled: Boolean = true,
    val enginePersonality: EnginePersonality = EnginePersonality.OPPORTUNITY_HUNTER,
    val engineBackendPreference: EngineBackendPreference = EngineBackendPreference.STOCKFISH_AUTO,
    val stockfishMoveTimeMs: Int = 900,
    val stockfishThreads: Int = 2,
    val autoReviewAfterGame: Boolean = true,
    val floatingAutoShow: Boolean = false,
    val floatingWidthDp: Int = 220,
    val floatingFrameShape: FloatingFrameShape = FloatingFrameShape.COMPACT,
    val floatingOpacityPercent: Int = 85,
    val floatingBubbleSize: FloatingBubbleSize = FloatingBubbleSize.SMALL,
    val playerElo: Int = 1000,
    val xpPoints: Int = 0,
    val dailyStreak: Int = 0,
    val lastDailySolvedDate: String = "",
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val selectedBotId: String = "fischer"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val BOARD_MATERIAL = stringPreferencesKey("board_material")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_EVAL_BAR = booleanPreferencesKey("show_eval_bar")
        val SHOW_LAST_MOVE = booleanPreferencesKey("show_last_move")
        val SHOW_LEGAL_MOVES = booleanPreferencesKey("show_legal_moves")
        val USE_RINGS = booleanPreferencesKey("use_rings_instead_of_dots")
        val SELECTED_COLOR_HEX = longPreferencesKey("selected_color_hex")
        val LAST_MOVE_COLOR_HEX = longPreferencesKey("last_move_color_hex")
        val LEGAL_MOVE_COLOR_HEX = longPreferencesKey("legal_move_color_hex")
        val ADAPTIVE_MODE = booleanPreferencesKey("adaptive_mode")
        val CUSTOM_ENGINE_ELO = intPreferencesKey("custom_engine_elo")
        val HUMAN_STYLE = booleanPreferencesKey("human_style_enabled")
        val ENGINE_PERSONALITY = stringPreferencesKey("engine_personality")
        val ENGINE_BACKEND = stringPreferencesKey("engine_backend_preference")
        val STOCKFISH_MOVETIME_MS = intPreferencesKey("stockfish_movetime_ms")
        val STOCKFISH_THREADS = intPreferencesKey("stockfish_threads")
        val AUTO_REVIEW = booleanPreferencesKey("auto_review_after_game")
        val FLOATING_AUTO_SHOW = booleanPreferencesKey("floating_auto_show")
        val FLOATING_WIDTH_DP = intPreferencesKey("floating_width_dp")
        val FLOATING_FRAME_SHAPE = stringPreferencesKey("floating_frame_shape")
        val FLOATING_OPACITY = intPreferencesKey("floating_opacity_percent")
        val FLOATING_BUBBLE_SIZE = stringPreferencesKey("floating_bubble_size")
        val PLAYER_ELO = intPreferencesKey("player_elo")
        val XP_POINTS = intPreferencesKey("xp_points")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SELECTED_BOT_ID = stringPreferencesKey("selected_bot_id")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            themeMode = runCatching {
                AppThemeMode.valueOf(p[Keys.THEME_MODE] ?: AppThemeMode.LIGHT.name)
            }.getOrDefault(AppThemeMode.LIGHT),
            boardTheme = BoardTheme.byId(p[Keys.BOARD_THEME] ?: BoardTheme.TOURNAMENT_EMERALD.id),
            boardMaterial = BoardMaterial.byId(p[Keys.BOARD_MATERIAL] ?: BoardMaterial.FLAT.id),
            pieceStyle = PieceStyle.byId(p[Keys.PIECE_STYLE] ?: PieceStyle.CLASSIC_STAUNTON.id),
            soundEnabled = p[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = p[Keys.HAPTICS_ENABLED] ?: true,
            showEvalBar = p[Keys.SHOW_EVAL_BAR] ?: true,
            showLastMove = p[Keys.SHOW_LAST_MOVE] ?: true,
            showLegalMoves = p[Keys.SHOW_LEGAL_MOVES] ?: true,
            useRingsInsteadOfDots = p[Keys.USE_RINGS] ?: true,
            selectedColorHex = p[Keys.SELECTED_COLOR_HEX] ?: 0xAA0D9488L,
            lastMoveColorHex = p[Keys.LAST_MOVE_COLOR_HEX] ?: 0x77769656L,
            legalMoveColorHex = p[Keys.LEGAL_MOVE_COLOR_HEX] ?: 0xCC0D9488L,
            adaptiveMode = p[Keys.ADAPTIVE_MODE] ?: false,
            customEngineElo = p[Keys.CUSTOM_ENGINE_ELO] ?: 1800,
            humanStyleEnabled = p[Keys.HUMAN_STYLE] ?: true,
            enginePersonality = EnginePersonality.byId(p[Keys.ENGINE_PERSONALITY] ?: EnginePersonality.OPPORTUNITY_HUNTER.id),
            engineBackendPreference = EngineBackendPreference.byId(p[Keys.ENGINE_BACKEND] ?: EngineBackendPreference.STOCKFISH_AUTO.id),
            stockfishMoveTimeMs = p[Keys.STOCKFISH_MOVETIME_MS] ?: 900,
            stockfishThreads = p[Keys.STOCKFISH_THREADS] ?: 2,
            autoReviewAfterGame = p[Keys.AUTO_REVIEW] ?: true,
            floatingAutoShow = p[Keys.FLOATING_AUTO_SHOW] ?: false,
            floatingWidthDp = p[Keys.FLOATING_WIDTH_DP] ?: 220,
            floatingFrameShape = FloatingFrameShape.byId(p[Keys.FLOATING_FRAME_SHAPE] ?: FloatingFrameShape.COMPACT.id),
            floatingOpacityPercent = p[Keys.FLOATING_OPACITY] ?: 85,
            floatingBubbleSize = FloatingBubbleSize.byId(p[Keys.FLOATING_BUBBLE_SIZE] ?: FloatingBubbleSize.SMALL.id),
            playerElo = p[Keys.PLAYER_ELO] ?: 1000,
            xpPoints = p[Keys.XP_POINTS] ?: 0,
            dailyStreak = p[Keys.DAILY_STREAK] ?: 0,
            lastDailySolvedDate = p[Keys.LAST_DAILY_DATE] ?: "",
            remindersEnabled = p[Keys.REMINDERS_ENABLED] ?: false,
            reminderHour = p[Keys.REMINDER_HOUR] ?: 19,
            reminderMinute = p[Keys.REMINDER_MINUTE] ?: 0,
            selectedBotId = p[Keys.SELECTED_BOT_ID] ?: "fischer"
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
        useRingsInsteadOfDots: Boolean
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
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setShowEvalBar(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_EVAL_BAR] = show }
    }

    suspend fun setAdaptiveMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADAPTIVE_MODE] = enabled }
    }

    suspend fun setCustomEngineElo(elo: Int) {
        context.dataStore.edit { it[Keys.CUSTOM_ENGINE_ELO] = elo.coerceIn(200, 3000) }
    }

    suspend fun setHumanStyleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HUMAN_STYLE] = enabled }
    }

    suspend fun setEnginePersonality(personality: EnginePersonality) {
        context.dataStore.edit { it[Keys.ENGINE_PERSONALITY] = personality.id }
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

    suspend fun setAutoReviewAfterGame(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_REVIEW] = enabled }
    }

    suspend fun setFloatingAutoShow(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_AUTO_SHOW] = enabled }
    }

    suspend fun saveFloatingCustomization(
        widthDp: Int,
        frameShape: FloatingFrameShape,
        opacityPercent: Int,
        bubbleSize: FloatingBubbleSize
    ) {
        context.dataStore.edit {
            it[Keys.FLOATING_WIDTH_DP] = widthDp.coerceIn(160, 320)
            it[Keys.FLOATING_FRAME_SHAPE] = frameShape.id
            it[Keys.FLOATING_OPACITY] = opacityPercent.coerceIn(30, 100)
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
}
