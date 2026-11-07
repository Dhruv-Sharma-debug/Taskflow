package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 14): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE workspaceId = :workspaceId ORDER BY date DESC LIMIT :limit")
    fun getRecentLogsForWorkspace(workspaceId: String, limit: Int = 14): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE date = :date AND workspaceId = :workspaceId")
    suspend fun getLogForDate(date: String, workspaceId: String = "personal"): DailyLogEntity?

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllLogsList(): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs")
    suspend fun clearAllLogs()
}
