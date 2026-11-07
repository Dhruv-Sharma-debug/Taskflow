package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val platformOrAuthor: String = "Online Course",
    val totalModules: Int = 10,
    val completedModules: Int = 0,
    val progressPct: Int = 0, // 0 to 100
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val workspaceId: String = "personal"
)
