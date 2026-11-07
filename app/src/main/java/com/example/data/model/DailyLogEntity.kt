package com.example.data.model

import androidx.room.Entity

@Entity(tableName = "daily_logs", primaryKeys = ["date", "workspaceId"])
data class DailyLogEntity(
    val date: String, // YYYY-MM-DD
    val workspaceId: String = "personal",
    val totalScheduledMins: Int = 0,
    val totalLoggedMins: Int = 0,
    val tasksCompletedCount: Int = 0,
    val totalTasksCount: Int = 0
)
