package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.BoardTheme
import com.example.chess.PieceStyle
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elite_chess_prefs")

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showEvalBar: Boolean = true,
    val adaptiveMode: Boolean = false,
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
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_EVAL_BAR = booleanPreferencesKey("show_eval_bar")
        val ADAPTIVE_MODE = booleanPreferencesKey("adaptive_mode")
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
            pieceStyle = PieceStyle.byId(p[Keys.PIECE_STYLE] ?: PieceStyle.CLASSIC_STAUNTON.id),
            soundEnabled = p[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = p[Keys.HAPTICS_ENABLED] ?: true,
            showEvalBar = p[Keys.SHOW_EVAL_BAR] ?: true,
            adaptiveMode = p[Keys.ADAPTIVE_MODE] ?: false,
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
