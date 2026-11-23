package com.example.data

import kotlinx.coroutines.flow.Flow

interface SavedGameDao {
    fun observeLatestOngoingGame(): Flow<SavedGameEntity?>
    fun observeCompletedGames(): Flow<List<SavedGameEntity>>
    fun observeAllGames(): Flow<List<SavedGameEntity>>
    suspend fun getById(id: Long): SavedGameEntity?
    suspend fun upsert(game: SavedGameEntity): Long
    suspend fun deleteById(id: Long)
}

interface RatingPointDao {
    fun observeAll(): Flow<List<RatingPointEntity>>
    suspend fun insert(point: RatingPointEntity)
}

interface PuzzleRecordDao {
    fun observeAll(): Flow<List<PuzzleRecordEntity>>
    suspend fun upsert(record: PuzzleRecordEntity)
}

interface AchievementDao {
    fun observeAll(): Flow<List<AchievementEntity>>
    suspend fun insert(achievement: AchievementEntity)
}
