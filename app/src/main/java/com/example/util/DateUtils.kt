package com.example.util

import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CalendarDayChip(
    val dateIso: String,
    val shortDayName: String,
    val dayOfMonth: String,
    val isToday: Boolean
)

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val friendlyFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val shortDayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())

    fun getTodayIso(): String {
        return isoFormat.format(Calendar.getInstance().time)
    }

    fun formatIsoFromMillis(millis: Long): String {
        return try {
            isoFormat.format(Date(millis))
        } catch (_: Exception) {
            getTodayIso()
        }
    }

    fun parseIsoToUtcMillis(isoDate: String): Long {
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            utcFormat.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatUtcMillisToIso(utcMillis: Long): String {
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            utcFormat.format(Date(utcMillis))
        } catch (_: Exception) {
            getTodayIso()
        }
    }

    fun addDaysToIso(isoDate: String, days: Int): String {
        return try {
            val parsed = isoFormat.parse(isoDate) ?: return getTodayIso()
            val cal = Calendar.getInstance()
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, days)
            isoFormat.format(cal.time)
        } catch (_: Exception) {
            getTodayIso()
        }
    }

    fun formatMonthYearHeader(isoDate: String): String {
        return try {
            val parsed = isoFormat.parse(isoDate) ?: return isoDate
            monthYearFormat.format(parsed)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun getWeekDaysAround(centerDateIso: String): List<CalendarDayChip> {
        val todayIso = getTodayIso()
        val baseDate = try {
            isoFormat.parse(centerDateIso) ?: Calendar.getInstance().time
        } catch (_: Exception) {
            Calendar.getInstance().time
        }
        val list = mutableListOf<CalendarDayChip>()
        for (offset in -3..3) {
            val cal = Calendar.getInstance()
            cal.time = baseDate
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dateStr = isoFormat.format(cal.time)
            list.add(
                CalendarDayChip(
                    dateIso = dateStr,
                    shortDayName = shortDayFormat.format(cal.time),
                    dayOfMonth = dayOfMonthFormat.format(cal.time),
                    isToday = dateStr == todayIso
                )
            )
        }
        return list
    }

    fun getEffectiveTaskDateIso(task: TaskEntity): String {
        return task.scheduledDate?.takeIf { it.isNotBlank() }
            ?: formatIsoFromMillis(task.createdAt)
    }

    fun getTodayDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        val javaDow = cal.get(Calendar.DAY_OF_WEEK)
        return if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1 // 1 = Mon .. 7 = Sun
    }

    fun isTodayInRepeatDays(repeatDays: List<Int>): Boolean {
        if (repeatDays.isEmpty() || repeatDays.size == 7) return true
        return repeatDays.contains(getTodayDayOfWeek())
    }

    fun isDayScheduledOnDate(repeatDays: List<Int>, dateIso: String): Boolean {
        if (repeatDays.isEmpty() || repeatDays.size == 7) return true
        return try {
            val date = isoFormat.parse(dateIso) ?: return false
            val cal = Calendar.getInstance()
            cal.time = date
            val javaDow = cal.get(Calendar.DAY_OF_WEEK)
            val isoDow = if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1
            repeatDays.contains(isoDow)
        } catch (_: Exception) {
            false
        }
    }

    fun hasScheduledDayOccurredBetween(repeatDays: List<Int>, fromDateIso: String, toDateIso: String): Boolean {
        return try {
            val fromDate = isoFormat.parse(fromDateIso) ?: return false
            val toDate = isoFormat.parse(toDateIso) ?: return false
            if (!fromDate.before(toDate)) return false

            val cal = Calendar.getInstance()
            cal.time = fromDate
            cal.add(Calendar.DAY_OF_YEAR, 1)

            val endCal = Calendar.getInstance()
            endCal.time = toDate

            while (cal.before(endCal)) {
                val javaDow = cal.get(Calendar.DAY_OF_WEEK)
                val isoDow = if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1
                if (repeatDays.isEmpty() || repeatDays.size == 7 || repeatDays.contains(isoDow)) {
                    return true
                }
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Determines whether a completed recurring task should roll over to uncompleted for today.
     * True if completed on a previous date AND (today is scheduled OR a scheduled day passed since completion).
     */
    fun isRecurringTaskDueForRollover(task: TaskEntity, todayIso: String = getTodayIso()): Boolean {
        if (!task.isRecurring || !task.isCompleted) return false
        val lastCompleted = task.lastCompletedDate ?: return true
        if (lastCompleted == todayIso) return false // Already completed today, do not roll over today!

        // If today is a scheduled day for this task, it rolls over today!
        if (isTodayInRepeatDays(task.repeatDays)) return true

        // If a scheduled day was missed between the last completion and today, it rolls over to carry over today
        return hasScheduledDayOccurredBetween(task.repeatDays, lastCompleted, todayIso)
    }

    /**
     * Determines if a task is active and should appear in Today's task list.
     * - Completed tasks completed today are handled separately (capacity/stats).
     * - Non-repeating tasks with a future scheduledDate (> todayIso) do NOT appear on Today until their scheduled date arrives.
     * - Non-repeating tasks scheduled for today or earlier (overdue) appear on Today until completed.
     * - Uncompleted repeating tasks appear if today is scheduled, or if uncompleted from past scheduled days (carry over).
     */
    fun isTaskActiveForToday(task: TaskEntity, todayIso: String = getTodayIso()): Boolean {
        // If marked completed:
        if (task.isCompleted) {
            // If it's a recurring task due to roll over today, it is active for today
            return isRecurringTaskDueForRollover(task, todayIso)
        }

        // Uncompleted non-recurring tasks: check scheduledDate if set
        if (!task.isRecurring) {
            val schedDate = task.scheduledDate?.takeIf { it.isNotBlank() }
            return if (schedDate != null) {
                schedDate <= todayIso
            } else {
                true
            }
        }

        // Daily recurring tasks are always active if uncompleted
        if (task.repeatDays.isEmpty() || task.repeatDays.size == 7) {
            return true
        }

        // If today is one of the designated repeat days
        if (isTodayInRepeatDays(task.repeatDays)) {
            return true
        }

        // If user already logged time towards it today
        if (task.loggedMins > 0) {
            return true
        }

        // Carry-over check: if task was scheduled in the past and left uncompleted,
        // it carries over each day until completed
        val createdDateIso = try {
            isoFormat.format(Date(task.createdAt))
        } catch (_: Exception) {
            todayIso
        }
        val referenceDateIso = task.lastCompletedDate ?: createdDateIso

        if (referenceDateIso < todayIso) {
            return isDayScheduledOnDate(task.repeatDays, referenceDateIso) ||
                   hasScheduledDayOccurredBetween(task.repeatDays, referenceDateIso, todayIso)
        }

        return false
    }

    /**
     * Determines if a task is Upcoming:
     * - Non-recurring uncompleted task scheduled for a future date (> todayIso), OR
     * - Recurring task that repeats on upcoming days.
     */
    fun isTaskUpcoming(task: TaskEntity, todayIso: String = getTodayIso()): Boolean {
        if (!task.isRecurring) {
            val schedDate = task.scheduledDate?.takeIf { it.isNotBlank() } ?: return false
            return !task.isCompleted && schedDate > todayIso
        }
        return !task.isCompleted || task.lastCompletedDate == todayIso
    }

    /**
     * Determines if a task should be shown on a specific date in the Calendar view.
     */
    fun isTaskScheduledForDate(task: TaskEntity, targetDateIso: String): Boolean {
        return if (!task.isRecurring) {
            val effectiveDate = getEffectiveTaskDateIso(task)
            effectiveDate == targetDateIso
        } else {
            isDayScheduledOnDate(task.repeatDays, targetDateIso)
        }
    }

    fun getRecurringScheduleLabel(repeatDays: List<Int>): String {
        if (repeatDays.isEmpty() || repeatDays.size == 7) return "Daily"
        if (repeatDays == listOf(1, 2, 3, 4, 5)) return "Weekdays"
        val dayNames = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        return repeatDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }

    fun getNextUpcomingDayName(repeatDays: List<Int>): String {
        if (repeatDays.isEmpty() || repeatDays.size == 7) return "Daily"
        if (repeatDays == listOf(1, 2, 3, 4, 5)) return "Weekdays"
        val dayNames = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        val todayDow = getTodayDayOfWeek()
        for (offset in 1..7) {
            val testDow = ((todayDow - 1 + offset) % 7) + 1
            if (repeatDays.contains(testDow)) {
                return if (offset == 1) "Tomorrow" else dayNames[testDow] ?: ""
            }
        }
        return repeatDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }

    fun getUpcomingScheduleDescription(repeatDays: List<Int>): String {
        if (repeatDays.isEmpty() || repeatDays.size == 7) return "Next week: Every Day"
        if (repeatDays == listOf(1, 2, 3, 4, 5)) return "Next week: Mon – Fri"
        val dayNames = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        val daysList = repeatDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
        return "Next week: $daysList"
    }

    fun formatFriendly(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            val today = getTodayIso()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrow = isoFormat.format(cal.time)

            when (isoDate) {
                today -> "Today"
                tomorrow -> "Tomorrow"
                else -> friendlyFormat.format(date)
            }
        } catch (_: Exception) {
            isoDate
        }
    }

    fun getShortDayName(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return ""
            shortDayFormat.format(date)
        } catch (_: Exception) {
            ""
        }
    }

    fun getLast7DaysIso(): List<String> {
        val list = mutableListOf<String>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            list.add(isoFormat.format(c.time))
        }
        return list
    }

    fun calculateNextRecurringDate(repeatDays: List<Int>, fromDateIso: String = getTodayIso()): String {
        val cal = Calendar.getInstance()
        try {
            val parsed = isoFormat.parse(fromDateIso)
            if (parsed != null) {
                cal.time = parsed
            }
        } catch (_: Exception) {
        }

        if (repeatDays.isEmpty()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return isoFormat.format(cal.time)
        }

        for (offset in 1..14) {
            val testCal = Calendar.getInstance()
            testCal.time = cal.time
            testCal.add(Calendar.DAY_OF_YEAR, offset)

            val javaDow = testCal.get(Calendar.DAY_OF_WEEK)
            val isoDow = if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1

            if (repeatDays.contains(isoDow)) {
                return isoFormat.format(testCal.time)
            }
        }

        cal.add(Calendar.DAY_OF_YEAR, 1)
        return isoFormat.format(cal.time)
    }

    fun formatDurationMins(mins: Int): String {
        if (mins <= 0) return "0m"
        val hours = mins / 60
        val remainingMins = mins % 60
        return when {
            hours > 0 && remainingMins > 0 -> "${hours}h ${remainingMins}m"
            hours > 0 -> "${hours}h"
            else -> "${remainingMins}m"
        }
    }

    fun formatMinutes(mins: Int): String = formatDurationMins(mins)

    /**
     * Compact single-token duration format for 7-day bar chart headers so adjacent
     * multi-part labels (e.g. "4h30m" and "3h35m") never collide horizontally.
     */
    fun formatCompactDurationMins(mins: Int): String {
        if (mins <= 0) return "0m"
        val hours = mins / 60
        val remainingMins = mins % 60
        return when {
            hours > 0 && remainingMins > 0 -> "${hours}h${remainingMins}m"
            hours > 0 -> "${hours}h"
            else -> "${remainingMins}m"
        }
    }
}
