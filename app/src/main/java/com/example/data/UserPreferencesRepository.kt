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
import com.example.chess.PersonalityRisk
import com.example.chess.PieceStyle
import com.example.chess.SoundProfile
import com.example.chess.StrengthTier
import com.example.chess.VibrationStyle
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ruqaa_chess_prefs")

/**
 * Default values match Ruqaa Specification Section 13:
 * - light = false (AppThemeMode.DARK)
 * - sound = false, vibrate = false, sound_profile = soft, sound_volume = 55, vibration_style = 0
 * - auto_review = true, human_style = true, auto_overlay = true
 * - piece_set = CLASSIC, board_texture = FLAT, mark_rings = false, mark_legal_visible = true, mark_last_visible = true
 * - overlay_x = 12, overlay_y = 72, overlay_width = 270, overlay_opacity = 100
 * - engine_elo = -1 (uses EASY preset = 700), personality = CLASSIC, intensity = 50, risk = BALANCED
 */
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
    val customEngineElo: Int = -1, // -1 means use strengthPreset
    val humanStyleEnabled: Boolean = true,
    val enginePersonality: EnginePersonality = EnginePersonality.CLASSIC,
    val personalityIntensity: Int = 50,
    val personalityRisk: PersonalityRisk = PersonalityRisk.BALANCED,
    val engineBackendPreference: EngineBackendPreference = EngineBackendPreference.STOCKFISH_AUTO,
    val stockfishMoveTimeMs: Int = 900,
    val stockfishThreads: Int = 2,
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
    val selectedBotId: String = "fischer"
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
            stockfishThreads = p[Keys.STOCKFISH_THREADS] ?: 2,
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
        context.dataStore.edit { it[Keys.STOCKFISH_THREADS] = threads.coerceIn(1, 2) }
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
}
