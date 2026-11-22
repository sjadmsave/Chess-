package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_games WHERE isCompleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestOngoingGame(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_games WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun observeCompletedGames(): Flow<List<SavedGameEntity>>

    @Query("SELECT * FROM saved_games ORDER BY updatedAt DESC")
    fun observeAllGames(): Flow<List<SavedGameEntity>>

    @Query("SELECT * FROM saved_games WHERE id = :id")
    suspend fun getById(id: Long): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: SavedGameEntity): Long

    @Query("DELETE FROM saved_games WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RatingPointDao {
    @Query("SELECT * FROM rating_points ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<RatingPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: RatingPointEntity)
}

@Dao
interface PuzzleRecordDao {
    @Query("SELECT * FROM puzzle_records ORDER BY solvedAt DESC")
    fun observeAll(): Flow<List<PuzzleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PuzzleRecordEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity)
}
