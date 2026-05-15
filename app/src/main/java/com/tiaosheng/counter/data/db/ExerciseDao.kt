package com.tiaosheng.counter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExerciseEntity): Long

    @Query("SELECT * FROM exercise_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_records ORDER BY startTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 50): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_records WHERE startTime >= :sinceMs ORDER BY startTime DESC")
    fun getRecordsSince(sinceMs: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_records WHERE startTime >= :sinceMs AND startTime < :untilMs ORDER BY startTime DESC")
    fun getRecordsInRange(sinceMs: Long, untilMs: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT SUM(totalCount) FROM exercise_records WHERE startTime >= :sinceMs")
    suspend fun getTotalCountSince(sinceMs: Long): Int?

    @Query("DELETE FROM exercise_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
