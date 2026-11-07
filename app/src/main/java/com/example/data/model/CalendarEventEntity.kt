package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dateIso: String, // "YYYY-MM-DD"
    val startTime: String = "09:00 AM", // "hh:mm AM/PM"
    val endTime: String = "10:00 AM", // "hh:mm AM/PM"
    val type: String = "EVENT", // "EVENT" or "APPOINTMENT"
    val isCompleted: Boolean = false,
    val workspaceId: String = "personal",
    val googleCalendarEventId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
