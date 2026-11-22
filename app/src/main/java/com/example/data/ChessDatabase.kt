package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedGameEntity::class,
        RatingPointEntity::class,
        PuzzleRecordEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun savedGameDao(): SavedGameDao
    abstract fun ratingPointDao(): RatingPointDao
    abstract fun puzzleRecordDao(): PuzzleRecordDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "elite_chess_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
