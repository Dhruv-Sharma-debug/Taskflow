package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: Int = 1, // 1 to 4 (1 = P1 highest)
    val progressPct: Int = 0, // 0 to 100
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val workspaceId: String = "personal"
)
