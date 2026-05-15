package com.tiaosheng.counter.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_records")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val totalCount: Int,
    val mode: String,        // "both_feet" / "alternate"
    val avgBpm: Float,
    val maxBpm: Float,
    val calories: Float,
    val durationSeconds: Int
)
