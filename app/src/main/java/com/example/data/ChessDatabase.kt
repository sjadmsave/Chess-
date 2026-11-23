package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class ChessDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val writeLock = Any()
    private val savedGamesFlow = MutableStateFlow<List<SavedGameEntity>>(emptyList())
    private val ratingPointsFlow = MutableStateFlow<List<RatingPointEntity>>(emptyList())
    private val puzzleRecordsFlow = MutableStateFlow<List<PuzzleRecordEntity>>(emptyList())
    private val achievementsFlow = MutableStateFlow<List<AchievementEntity>>(emptyList())

    @Volatile
    private var savedGamesLoaded = false
    @Volatile
    private var ratingPointsLoaded = false
    @Volatile
    private var puzzleRecordsLoaded = false
    @Volatile
    private var achievementsLoaded = false

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_games (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                botId TEXT NOT NULL,
                playerColor TEXT NOT NULL,
                currentFen TEXT NOT NULL,
                movesEncoded TEXT NOT NULL,
                moveCount INTEGER NOT NULL,
                resultName TEXT NOT NULL,
                isCompleted INTEGER NOT NULL,
                playerAccuracy INTEGER NOT NULL,
                opponentAccuracy INTEGER NOT NULL,
                acpl INTEGER NOT NULL,
                openingNameAr TEXT NOT NULL,
                openingAccuracy INTEGER NOT NULL,
                middlegameAccuracy INTEGER NOT NULL,
                endgameAccuracy INTEGER NOT NULL,
                eloDelta INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                gameMode TEXT NOT NULL DEFAULT 'vs_engine',
                startFen TEXT NOT NULL DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
                timeControlId TEXT NOT NULL DEFAULT 'unlimited',
                whiteTimeSec INTEGER NOT NULL DEFAULT 0,
                blackTimeSec INTEGER NOT NULL DEFAULT 0,
                hintsUsed INTEGER NOT NULL DEFAULT 0,
                assisted INTEGER NOT NULL DEFAULT 0,
                strengthMixed INTEGER NOT NULL DEFAULT 0,
                edited INTEGER NOT NULL DEFAULT 0,
                flipped INTEGER NOT NULL DEFAULT 0,
                reviewCacheKey TEXT NOT NULL DEFAULT '',
                reviewCompleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS rating_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                elo INTEGER NOT NULL,
                accuracy INTEGER NOT NULL,
                openingAcc INTEGER NOT NULL,
                middlegameAcc INTEGER NOT NULL,
                endgameAcc INTEGER NOT NULL,
                blunders INTEGER NOT NULL,
                mistakes INTEGER NOT NULL,
                won INTEGER NOT NULL,
                botId TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS puzzle_records (
                puzzleId TEXT PRIMARY KEY NOT NULL,
                solvedAt INTEGER NOT NULL,
                usedHint INTEGER NOT NULL,
                attempts INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS achievements (
                id TEXT PRIMARY KEY NOT NULL,
                unlockedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS saved_games")
        db.execSQL("DROP TABLE IF EXISTS rating_points")
        db.execSQL("DROP TABLE IF EXISTS puzzle_records")
        db.execSQL("DROP TABLE IF EXISTS achievements")
        onCreate(db)
    }

    fun savedGameDao(): SavedGameDao = savedGameDaoImpl
    fun ratingPointDao(): RatingPointDao = ratingPointDaoImpl
    fun puzzleRecordDao(): PuzzleRecordDao = puzzleRecordDaoImpl
    fun achievementDao(): AchievementDao = achievementDaoImpl

    private val savedGameDaoImpl = object : SavedGameDao {
        override fun observeLatestOngoingGame(): Flow<SavedGameEntity?> =
            observeSavedGames().map { list -> list.firstOrNull { !it.isCompleted } }

        override fun observeCompletedGames(): Flow<List<SavedGameEntity>> =
            observeSavedGames().map { list -> list.filter { it.isCompleted } }

        override fun observeAllGames(): Flow<List<SavedGameEntity>> = observeSavedGames()

        override suspend fun getById(id: Long): SavedGameEntity? = withContext(Dispatchers.IO) {
            synchronized(writeLock) {
                readableDatabase.query(
                    "saved_games",
                    null,
                    "id = ?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst()) readSavedGame(cursor) else null
                }
            }
        }

        override suspend fun upsert(game: SavedGameEntity): Long = withContext(Dispatchers.IO) {
            synchronized(writeLock) {
                val values = ContentValues().apply {
                    if (game.id > 0L) put("id", game.id)
                    put("botId", game.botId)
                    put("playerColor", game.playerColor)
                    put("currentFen", game.currentFen)
                    put("movesEncoded", game.movesEncoded)
                    put("moveCount", game.moveCount)
                    put("resultName", game.resultName)
                    put("isCompleted", if (game.isCompleted) 1 else 0)
                    put("playerAccuracy", game.playerAccuracy)
                    put("opponentAccuracy", game.opponentAccuracy)
                    put("acpl", game.acpl)
                    put("openingNameAr", game.openingNameAr)
                    put("openingAccuracy", game.openingAccuracy)
                    put("middlegameAccuracy", game.middlegameAccuracy)
                    put("endgameAccuracy", game.endgameAccuracy)
                    put("eloDelta", game.eloDelta)
                    put("updatedAt", game.updatedAt)
                    put("gameMode", game.gameMode)
                    put("startFen", game.startFen)
                    put("timeControlId", game.timeControlId)
                    put("whiteTimeSec", game.whiteTimeSec)
                    put("blackTimeSec", game.blackTimeSec)
                    put("hintsUsed", game.hintsUsed)
                    put("assisted", if (game.assisted) 1 else 0)
                    put("strengthMixed", if (game.strengthMixed) 1 else 0)
                    put("edited", if (game.edited) 1 else 0)
                    put("flipped", if (game.flipped) 1 else 0)
                    put("reviewCacheKey", game.reviewCacheKey)
                    put("reviewCompleted", if (game.reviewCompleted) 1 else 0)
                }
                val rowId = writableDatabase.insertWithOnConflict(
                    "saved_games",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                refreshSavedGamesLocked()
                if (game.id > 0L) game.id else rowId
            }
        }

        override suspend fun deleteById(id: Long) {
            withContext(Dispatchers.IO) {
                synchronized(writeLock) {
                    writableDatabase.delete("saved_games", "id = ?", arrayOf(id.toString()))
                    refreshSavedGamesLocked()
                }
            }
        }
    }

    private val ratingPointDaoImpl = object : RatingPointDao {
        override fun observeAll(): Flow<List<RatingPointEntity>> =
            ratingPointsFlow.onStart {
                if (!ratingPointsLoaded) {
                    withContext(Dispatchers.IO) {
                        synchronized(writeLock) {
                            if (!ratingPointsLoaded) refreshRatingPointsLocked()
                        }
                    }
                }
            }

        override suspend fun insert(point: RatingPointEntity) {
            withContext(Dispatchers.IO) {
                synchronized(writeLock) {
                    val values = ContentValues().apply {
                        if (point.id > 0L) put("id", point.id)
                        put("timestamp", point.timestamp)
                        put("elo", point.elo)
                        put("accuracy", point.accuracy)
                        put("openingAcc", point.openingAcc)
                        put("middlegameAcc", point.middlegameAcc)
                        put("endgameAcc", point.endgameAcc)
                        put("blunders", point.blunders)
                        put("mistakes", point.mistakes)
                        put("won", if (point.won) 1 else 0)
                        put("botId", point.botId)
                    }
                    writableDatabase.insertWithOnConflict(
                        "rating_points",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                    refreshRatingPointsLocked()
                }
            }
        }
    }

    private val puzzleRecordDaoImpl = object : PuzzleRecordDao {
        override fun observeAll(): Flow<List<PuzzleRecordEntity>> =
            puzzleRecordsFlow.onStart {
                if (!puzzleRecordsLoaded) {
                    withContext(Dispatchers.IO) {
                        synchronized(writeLock) {
                            if (!puzzleRecordsLoaded) refreshPuzzleRecordsLocked()
                        }
                    }
                }
            }

        override suspend fun upsert(record: PuzzleRecordEntity) {
            withContext(Dispatchers.IO) {
                synchronized(writeLock) {
                    val values = ContentValues().apply {
                        put("puzzleId", record.puzzleId)
                        put("solvedAt", record.solvedAt)
                        put("usedHint", if (record.usedHint) 1 else 0)
                        put("attempts", record.attempts)
                    }
                    writableDatabase.insertWithOnConflict(
                        "puzzle_records",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                    refreshPuzzleRecordsLocked()
                }
            }
        }
    }

    private val achievementDaoImpl = object : AchievementDao {
        override fun observeAll(): Flow<List<AchievementEntity>> =
            achievementsFlow.onStart {
                if (!achievementsLoaded) {
                    withContext(Dispatchers.IO) {
                        synchronized(writeLock) {
                            if (!achievementsLoaded) refreshAchievementsLocked()
                        }
                    }
                }
            }

        override suspend fun insert(achievement: AchievementEntity) {
            withContext(Dispatchers.IO) {
                synchronized(writeLock) {
                    val values = ContentValues().apply {
                        put("id", achievement.id)
                        put("unlockedAt", achievement.unlockedAt)
                    }
                    writableDatabase.insertWithOnConflict(
                        "achievements",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_IGNORE
                    )
                    refreshAchievementsLocked()
                }
            }
        }
    }

    private fun observeSavedGames(): Flow<List<SavedGameEntity>> =
        savedGamesFlow.onStart {
            if (!savedGamesLoaded) {
                withContext(Dispatchers.IO) {
                    synchronized(writeLock) {
                        if (!savedGamesLoaded) refreshSavedGamesLocked()
                    }
                }
            }
        }

    private fun refreshSavedGamesLocked() {
        val list = mutableListOf<SavedGameEntity>()
        readableDatabase.query(
            "saved_games",
            null,
            null,
            null,
            null,
            null,
            "updatedAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(readSavedGame(cursor))
            }
        }
        savedGamesLoaded = true
        savedGamesFlow.value = list
    }

    private fun refreshRatingPointsLocked() {
        val list = mutableListOf<RatingPointEntity>()
        readableDatabase.query(
            "rating_points",
            null,
            null,
            null,
            null,
            null,
            "timestamp ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    RatingPointEntity(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        elo = cursor.getInt(cursor.getColumnIndexOrThrow("elo")),
                        accuracy = cursor.getInt(cursor.getColumnIndexOrThrow("accuracy")),
                        openingAcc = cursor.getInt(cursor.getColumnIndexOrThrow("openingAcc")),
                        middlegameAcc = cursor.getInt(cursor.getColumnIndexOrThrow("middlegameAcc")),
                        endgameAcc = cursor.getInt(cursor.getColumnIndexOrThrow("endgameAcc")),
                        blunders = cursor.getInt(cursor.getColumnIndexOrThrow("blunders")),
                        mistakes = cursor.getInt(cursor.getColumnIndexOrThrow("mistakes")),
                        won = cursor.getInt(cursor.getColumnIndexOrThrow("won")) != 0,
                        botId = cursor.getString(cursor.getColumnIndexOrThrow("botId"))
                    )
                )
            }
        }
        ratingPointsLoaded = true
        ratingPointsFlow.value = list
    }

    private fun refreshPuzzleRecordsLocked() {
        val list = mutableListOf<PuzzleRecordEntity>()
        readableDatabase.query(
            "puzzle_records",
            null,
            null,
            null,
            null,
            null,
            "solvedAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    PuzzleRecordEntity(
                        puzzleId = cursor.getString(cursor.getColumnIndexOrThrow("puzzleId")),
                        solvedAt = cursor.getLong(cursor.getColumnIndexOrThrow("solvedAt")),
                        usedHint = cursor.getInt(cursor.getColumnIndexOrThrow("usedHint")) != 0,
                        attempts = cursor.getInt(cursor.getColumnIndexOrThrow("attempts"))
                    )
                )
            }
        }
        puzzleRecordsLoaded = true
        puzzleRecordsFlow.value = list
    }

    private fun refreshAchievementsLocked() {
        val list = mutableListOf<AchievementEntity>()
        readableDatabase.query(
            "achievements",
            null,
            null,
            null,
            null,
            null,
            "unlockedAt DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    AchievementEntity(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        unlockedAt = cursor.getLong(cursor.getColumnIndexOrThrow("unlockedAt"))
                    )
                )
            }
        }
        achievementsLoaded = true
        achievementsFlow.value = list
    }

    private fun readSavedGame(cursor: Cursor): SavedGameEntity = SavedGameEntity(
        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
        botId = cursor.getString(cursor.getColumnIndexOrThrow("botId")),
        playerColor = cursor.getString(cursor.getColumnIndexOrThrow("playerColor")),
        currentFen = cursor.getString(cursor.getColumnIndexOrThrow("currentFen")),
        movesEncoded = cursor.getString(cursor.getColumnIndexOrThrow("movesEncoded")),
        moveCount = cursor.getInt(cursor.getColumnIndexOrThrow("moveCount")),
        resultName = cursor.getString(cursor.getColumnIndexOrThrow("resultName")),
        isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")) != 0,
        playerAccuracy = cursor.getInt(cursor.getColumnIndexOrThrow("playerAccuracy")),
        opponentAccuracy = cursor.getInt(cursor.getColumnIndexOrThrow("opponentAccuracy")),
        acpl = cursor.getInt(cursor.getColumnIndexOrThrow("acpl")),
        openingNameAr = cursor.getString(cursor.getColumnIndexOrThrow("openingNameAr")),
        openingAccuracy = cursor.getInt(cursor.getColumnIndexOrThrow("openingAccuracy")),
        middlegameAccuracy = cursor.getInt(cursor.getColumnIndexOrThrow("middlegameAccuracy")),
        endgameAccuracy = cursor.getInt(cursor.getColumnIndexOrThrow("endgameAccuracy")),
        eloDelta = cursor.getInt(cursor.getColumnIndexOrThrow("eloDelta")),
        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")),
        gameMode = cursor.getString(cursor.getColumnIndexOrThrow("gameMode")),
        startFen = cursor.getString(cursor.getColumnIndexOrThrow("startFen")),
        timeControlId = cursor.getString(cursor.getColumnIndexOrThrow("timeControlId")),
        whiteTimeSec = cursor.getLong(cursor.getColumnIndexOrThrow("whiteTimeSec")),
        blackTimeSec = cursor.getLong(cursor.getColumnIndexOrThrow("blackTimeSec")),
        hintsUsed = cursor.getInt(cursor.getColumnIndexOrThrow("hintsUsed")),
        assisted = cursor.getInt(cursor.getColumnIndexOrThrow("assisted")) != 0,
        strengthMixed = cursor.getInt(cursor.getColumnIndexOrThrow("strengthMixed")) != 0,
        edited = cursor.getInt(cursor.getColumnIndexOrThrow("edited")) != 0,
        flipped = cursor.getInt(cursor.getColumnIndexOrThrow("flipped")) != 0,
        reviewCacheKey = cursor.getString(cursor.getColumnIndexOrThrow("reviewCacheKey")),
        reviewCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("reviewCompleted")) != 0
    )

    companion object {
        private const val DATABASE_NAME = "ruqaa_chess_db"
        private const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChessDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
