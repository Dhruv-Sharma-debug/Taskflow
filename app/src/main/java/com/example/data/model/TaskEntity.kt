package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isRecurring: Boolean = false,
    val repeatDays: List<Int> = emptyList(), // 1 = Mon, ..., 7 = Sun
    val scheduledDate: String? = null, // "YYYY-MM-DD" scheduled date for non-repeating tasks (defaults to creation day)
    val time: String? = null, // "hh:mm AM/PM" scheduled reminder time
    val reminderId: Int = 0,
    val durationMins: Int = 30, // Custom duration: 1 min to 720 mins (12 hours)
    val loggedMins: Int = 0, // Total logged progress in minutes
    val subtasks: List<Subtask> = emptyList(),
    val isCompleted: Boolean = false,
    val priority: Int = 2, // 1 (Highest) to 4 (Lowest)
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val lastCompletedDate: String? = null, // "YYYY-MM-DD" of last completion for repeating tasks
    val preCompletionLoggedMins: Int = 0, // Stored progress prior to completion for exact undo
    val workspaceId: String = "personal",
    val googleCalendarEventId: Long? = null // Linked event ID in dedicated TaskFlow Google Calendar
)
