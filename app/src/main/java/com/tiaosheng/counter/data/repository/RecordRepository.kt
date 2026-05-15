package com.tiaosheng.counter.data.repository

import com.tiaosheng.counter.data.db.ExerciseDao
import com.tiaosheng.counter.data.db.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class RecordRepository(private val dao: ExerciseDao) {

    fun getAllRecords(): Flow<List<ExerciseEntity>> = dao.getAllRecords()

    fun getRecentRecords(limit: Int = 50): Flow<List<ExerciseEntity>> =
        dao.getRecentRecords(limit)

    fun getRecordsForDay(timestampMs: Long): Flow<List<ExerciseEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis
        return dao.getRecordsInRange(start, end)
    }

    fun getRecordsForWeek(timestampMs: Long): Flow<List<ExerciseEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return dao.getRecordsSince(cal.timeInMillis)
    }

    fun getRecordsForMonth(timestampMs: Long): Flow<List<ExerciseEntity>> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return dao.getRecordsSince(cal.timeInMillis)
    }

    suspend fun save(record: ExerciseEntity): Long = dao.insert(record)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
