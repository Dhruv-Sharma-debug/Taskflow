package com.example

import com.example.data.model.TaskEntity
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testParseHourAndMinute12HourFormat() {
        // Morning AM
        val amResult = NotificationHelper.parseHourAndMinute("09:30 AM")
        assertNotNull(amResult)
        assertEquals(9, amResult!!.first)
        assertEquals(30, amResult.second)

        // Afternoon PM
        val pmResult = NotificationHelper.parseHourAndMinute("02:15 PM")
        assertNotNull(pmResult)
        assertEquals(14, pmResult!!.first)
        assertEquals(15, pmResult.second)

        // Noon 12:00 PM
        val noonResult = NotificationHelper.parseHourAndMinute("12:00 PM")
        assertNotNull(noonResult)
        assertEquals(12, noonResult!!.first)
        assertEquals(0, noonResult.second)

        // Midnight 12:00 AM
        val midnightResult = NotificationHelper.parseHourAndMinute("12:00 AM")
        assertNotNull(midnightResult)
        assertEquals(0, midnightResult!!.first)
        assertEquals(0, midnightResult.second)
    }

    @Test
    fun testFormatDurationMins() {
        assertEquals("1m", DateUtils.formatDurationMins(1))
        assertEquals("30m", DateUtils.formatDurationMins(30))
        assertEquals("1h", DateUtils.formatDurationMins(60))
        assertEquals("1h 30m", DateUtils.formatDurationMins(90))
        assertEquals("8h", DateUtils.formatDurationMins(480))
        assertEquals("12h", DateUtils.formatDurationMins(720))
    }

    @Test
    fun testNonRepeatingTaskScheduledDateFiltering() {
        val todayIso = "2026-11-06"
        val tomorrowIso = "2026-11-07"
        val yesterdayIso = "2026-11-05"

        val todayTask = TaskEntity(
            id = 1,
            title = "Today Task",
            isRecurring = false,
            scheduledDate = todayIso
        )
        val futureTask = TaskEntity(
            id = 2,
            title = "Future Task",
            isRecurring = false,
            scheduledDate = tomorrowIso
        )
        val overdueTask = TaskEntity(
            id = 3,
            title = "Overdue Task",
            isRecurring = false,
            scheduledDate = yesterdayIso
        )

        // Active for today
        assertTrue(DateUtils.isTaskActiveForToday(todayTask, todayIso))
        assertTrue(DateUtils.isTaskActiveForToday(overdueTask, todayIso))
        assertFalse(DateUtils.isTaskActiveForToday(futureTask, todayIso))

        // Upcoming
        assertFalse(DateUtils.isTaskUpcoming(todayTask, todayIso))
        assertTrue(DateUtils.isTaskUpcoming(futureTask, todayIso))

        // Scheduled for specific calendar date
        assertTrue(DateUtils.isTaskScheduledForDate(todayTask, todayIso))
        assertFalse(DateUtils.isTaskScheduledForDate(todayTask, tomorrowIso))
        assertTrue(DateUtils.isTaskScheduledForDate(futureTask, tomorrowIso))

        // Week days helper returns 7 days centered on target date
        val weekDays = DateUtils.getWeekDaysAround(todayIso)
        assertEquals(7, weekDays.size)
        assertEquals(todayIso, weekDays[3].dateIso)
    }
}
