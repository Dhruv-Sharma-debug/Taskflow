package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            NotificationHelper.ACTION_TASK_REMINDER -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, 0L)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Scheduled Task"
                val desc = intent.getStringExtra(NotificationHelper.EXTRA_TASK_DESC) ?: ""
                val priority = intent.getIntExtra(NotificationHelper.EXTRA_TASK_PRIORITY, 2)
                val durationMins = intent.getIntExtra(NotificationHelper.EXTRA_TASK_DURATION, 30)
                val timeStr = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TIME) ?: ""

                // Play completion/reminder chime + post standard notification with sound & vibration
                SoundHelper.playCompletionChime(context)
                NotificationHelper.showTaskReminderNotification(
                    context = context,
                    taskId = taskId,
                    title = title,
                    description = desc,
                    priority = priority,
                    durationMins = durationMins,
                    timeStr = timeStr
                )

                // If the task is recurring and still active, schedule its next occurrence
                if (taskId > 0L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val task = db.taskDao().getTaskById(taskId)
                            if (task != null && task.isRecurring && !task.isCompleted && !task.time.isNullOrBlank()) {
                                NotificationHelper.scheduleTaskReminder(
                                    context = context,
                                    task = task,
                                    startFromTomorrow = true
                                )
                            }
                        } catch (_: Exception) {
                            // Ignore DB errors in background receiver
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val allTasks = db.taskDao().getAllTasksList()
                        for (task in allTasks) {
                            if (!task.isCompleted && !task.time.isNullOrBlank()) {
                                NotificationHelper.scheduleTaskReminder(context, task)
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore DB errors during boot receiver
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
