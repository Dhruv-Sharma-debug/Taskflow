package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TaskEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {

    const val CHANNEL_ID = "taskflow_reminders_alert_v3"
    private const val CHANNEL_NAME = "Task Reminders"
    const val ACTION_TASK_REMINDER = "com.example.taskflow.ACTION_TASK_REMINDER"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESC = "extra_task_desc"
    const val EXTRA_TASK_PRIORITY = "extra_task_priority"
    const val EXTRA_TASK_DURATION = "extra_task_duration"
    const val EXTRA_TASK_TIME = "extra_task_time"

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled task reminders with sound and vibration"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 150, 350)
            setSound(defaultSoundUri, audioAttributes)
        }

        manager.createNotificationChannel(channel)
    }

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

    /**
     * Computes the next trigger timestamp in epoch milliseconds for the given task.
     * - If [startFromTomorrow] is true (used after a recurring reminder just fired today),
     *   it searches starting from tomorrow.
     * - If the user sets a reminder for the exact current minute (e.g. testing right now),
     *   schedules it 3 seconds into the future so it triggers immediately instead of skipping to tomorrow.
     */
    fun computeNextTriggerMillis(
        task: TaskEntity,
        nowMillis: Long = System.currentTimeMillis(),
        startFromTomorrow: Boolean = false
    ): Long? {
        val rawTime = task.time?.takeIf { it.isNotBlank() } ?: return null
        val (hour24, minute) = parseHourAndMinute(rawTime) ?: return null

        val zone = ZoneId.systemDefault()
        val nowDateTime = LocalDateTime.now(zone)
        val today = nowDateTime.toLocalDate()
        val targetLocalTime = LocalTime.of(hour24, minute, 0)

        if (!task.isRecurring) {
            // Non-repeating task: check scheduledDate (defaults to today if null/past)
            val parsedScheduledDate = task.scheduledDate?.takeIf { it.isNotBlank() }?.let {
                try {
                    LocalDate.parse(it, isoDateFormatter)
                } catch (_: Exception) {
                    null
                }
            }

            val baseDate = if (parsedScheduledDate != null && parsedScheduledDate.isAfter(today)) {
                parsedScheduledDate
            } else {
                today
            }

            val candidateDateTime = LocalDateTime.of(baseDate, targetLocalTime)
            val candidateMillis = candidateDateTime.atZone(zone).toInstant().toEpochMilli()

            return when {
                candidateMillis > nowMillis -> candidateMillis
                // If user just saved a task for the current minute (within 60s), fire in 3 seconds
                !startFromTomorrow && baseDate == today && (nowMillis - candidateMillis) in 0..59_999L -> {
                    nowMillis + 3_000L
                }
                else -> {
                    // Time already passed today -> schedule for tomorrow at that time
                    LocalDateTime.of(today.plusDays(1), targetLocalTime)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                }
            }
        } else {
            // Repeating task: find next matching day of week (1 = Mon .. 7 = Sun)
            val startOffset = if (startFromTomorrow) 1 else 0
            for (offset in startOffset..7) {
                val candidateDate = today.plusDays(offset.toLong())
                val dow = candidateDate.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                val matchesDay = task.repeatDays.isEmpty() || task.repeatDays.contains(dow)
                if (!matchesDay) continue

                val candidateDateTime = LocalDateTime.of(candidateDate, targetLocalTime)
                val candidateMillis = candidateDateTime.atZone(zone).toInstant().toEpochMilli()

                if (candidateMillis > nowMillis) {
                    return candidateMillis
                }
                if (offset == 0 && !startFromTomorrow && (nowMillis - candidateMillis) in 0..59_999L) {
                    return nowMillis + 3_000L
                }
            }
            return null
        }
    }

    fun scheduleTaskReminder(
        context: Context,
        task: TaskEntity,
        startFromTomorrow: Boolean = false
    ) {
        if (task.id <= 0L || task.isCompleted || task.time.isNullOrBlank()) {
            if (task.id > 0L) cancelTaskReminder(context, task.id)
            return
        }

        createNotificationChannel(context)
        val triggerAtMillis = computeNextTriggerMillis(
            task = task,
            nowMillis = System.currentTimeMillis(),
            startFromTomorrow = startFromTomorrow
        ) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildReminderPendingIntent(context, task)

        // Cancel any previous alarm for this task ID before setting the updated one
        alarmManager.cancel(pendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback to inexact wake alarm if exact alarm permission is restricted
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun buildReminderPendingIntent(context: Context, task: TaskEntity): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_DESC, task.description)
            putExtra(EXTRA_TASK_PRIORITY, task.priority)
            putExtra(EXTRA_TASK_DURATION, task.durationMins)
            putExtra(EXTRA_TASK_TIME, task.time ?: "")
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        priority: Int,
        durationMins: Int,
        timeStr: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = DateUtils.formatDurationMins(durationMins)
        val subtitle = buildString {
            append("Priority P$priority • $durationFormatted")
            if (timeStr.isNotBlank()) {
                append(" • $timeStr")
            }
        }
        val bodyText = if (description.isNotBlank()) {
            "$subtitle\n$description"
        } else {
            subtitle
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Task Reminder: $title")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
    }
}
