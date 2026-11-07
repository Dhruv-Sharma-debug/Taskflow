package com.example.util

import android.content.Context
import com.example.data.model.TaskEntity

object NotificationHelper {

    fun parseHourAndMinute(timeStr: String): Pair<Int, Int>? {
        val trimmed = timeStr.trim().uppercase()
        if (trimmed.isEmpty()) return null

        return try {
            val isPm = trimmed.endsWith("PM")
            val isAm = trimmed.endsWith("AM")
            val cleanTime = trimmed.removeSuffix("PM").removeSuffix("AM").trim()
            val parts = cleanTime.split(":")
            if (parts.size != 2) return null

            var hour = parts[0].trim().toInt()
            val minute = parts[1].trim().toInt()

            if (isPm && hour < 12) {
                hour += 12
            } else if (isAm && hour == 12) {
                hour = 0
            }

            if (hour !in 0..23 || minute !in 0..59) return null
            Pair(hour, minute)
        } catch (_: Exception) {
            null
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        // Reminder scheduling helper
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        // Reminder cancellation helper
    }
}
