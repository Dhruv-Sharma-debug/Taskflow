package com.example

import com.example.data.local.Converters
import com.example.data.model.CourseEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.Subtask
import com.example.data.model.TaskEntity
import com.example.data.model.WorkspaceEntity
import com.example.util.BackupData
import com.example.util.BackupHelper
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TaskFlowLogicTest {

    @Test
    fun testDurationFormatting() {
        assertEquals("0m", DateUtils.formatDurationMins(0))
        assertEquals("5m", DateUtils.formatDurationMins(5))
        assertEquals("45m", DateUtils.formatDurationMins(45))
        assertEquals("1h", DateUtils.formatDurationMins(60))
        assertEquals("1h 30m", DateUtils.formatDurationMins(90))
        assertEquals("2h 15m", DateUtils.formatDurationMins(135))
        assertEquals("8h", DateUtils.formatDurationMins(480))
        assertEquals("8h 30m", DateUtils.formatDurationMins(510))
        assertEquals("10h", DateUtils.formatDurationMins(600))

        // Compact duration format for 7-day Weekly Activity bar chart headers
        assertEquals("0m", DateUtils.formatCompactDurationMins(0))
        assertEquals("45m", DateUtils.formatCompactDurationMins(45))
        assertEquals("4h", DateUtils.formatCompactDurationMins(240))
        assertEquals("4h30m", DateUtils.formatCompactDurationMins(270))
        assertEquals("3h35m", DateUtils.formatCompactDurationMins(215))
    }

    @Test
    fun testRecurrenceCalculation() {
        val nextDate = DateUtils.calculateNextRecurringDate(listOf(1, 2, 3, 4, 5), "2026-10-10")
        assertNotNull(nextDate)
        assertTrue(nextDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun testAmPmTimeParsing() {
        val amResult = NotificationHelper.parseHourAndMinute("09:30 AM")
        assertNotNull(amResult)
        assertEquals(9, amResult!!.first)
        assertEquals(30, amResult.second)

        val pmResult = NotificationHelper.parseHourAndMinute("02:15 PM")
        assertNotNull(pmResult)
        assertEquals(14, pmResult!!.first)
        assertEquals(15, pmResult.second)

        val midnightResult = NotificationHelper.parseHourAndMinute("12:00 AM")
        assertNotNull(midnightResult)
        assertEquals(0, midnightResult!!.first)
        assertEquals(0, midnightResult.second)

        val noonResult = NotificationHelper.parseHourAndMinute("12:45 PM")
        assertNotNull(noonResult)
        assertEquals(12, noonResult!!.first)
        assertEquals(45, noonResult.second)

        val milResult = NotificationHelper.parseHourAndMinute("15:30")
        assertNotNull(milResult)
        assertEquals(15, milResult!!.first)
        assertEquals(30, milResult.second)
    }

    @Test
    fun testConvertersSerialization() {
        val converters = Converters()

        // Test Int List
        val intList = listOf(1, 3, 5)
        val intString = converters.fromIntList(intList)
        val parsedIntList = converters.toIntList(intString)
        assertEquals(intList, parsedIntList)

        // Test Subtask List
        val subtasks = listOf(
            Subtask("id1", "Design mockups", true),
            Subtask("id2", "Implement database schema", false)
        )
        val json = converters.fromSubtaskList(subtasks)
        val parsedSubtasks = converters.toSubtaskList(json)
        assertEquals(2, parsedSubtasks.size)
        assertEquals("Design mockups", parsedSubtasks[0].title)
        assertTrue(parsedSubtasks[0].isCompleted)
        assertEquals("Implement database schema", parsedSubtasks[1].title)
        assertFalse(parsedSubtasks[1].isCompleted)
    }

    @Test
    fun testTodayP1TaskFilteringAndItemKeys() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Critical Bug Fix", priority = 1, isCompleted = false),
            TaskEntity(id = 2, title = "Update Docs", priority = 2, isCompleted = false),
            TaskEntity(id = 3, title = "Server Migration", priority = 1, isCompleted = true),
            TaskEntity(id = 4, title = "Security Patch", priority = 1, isCompleted = false)
        )

        // Only active (uncompleted) P1 tasks should be included in today's P1 list
        val activeP1Tasks = tasks.filter { !it.isCompleted && it.priority == 1 }
        assertEquals(2, activeP1Tasks.size)
        assertEquals(listOf(1L, 4L), activeP1Tasks.map { it.id })

        // Unique item key format prevents any collision
        val keys = activeP1Tasks.map { "task_${it.id}" }
        assertEquals(listOf("task_1", "task_4"), keys)
        assertEquals(keys.distinct().size, keys.size)
    }

    @Test
    fun testCourseModuleAndProgressLogic() {
        val course = CourseEntity(
            id = 1,
            title = "Android Jetpack Compose",
            totalModules = 10,
            completedModules = 3,
            progressPct = 30
        )

        // Increment module by 1
        val incModules = (course.completedModules + 1).coerceIn(0, course.totalModules)
        val incPct = ((incModules.toFloat() / course.totalModules) * 100).toInt()
        assertEquals(4, incModules)
        assertEquals(40, incPct)

        // Decrement module by 1
        val decModules = (course.completedModules - 1).coerceIn(0, course.totalModules)
        val decPct = ((decModules.toFloat() / course.totalModules) * 100).toInt()
        assertEquals(2, decModules)
        assertEquals(20, decPct)

        // Adjust percentage by 1%
        val adjustedPct = (course.progressPct + 1).coerceIn(0, 100)
        assertEquals(31, adjustedPct)
    }

    @Test
    fun testTaskCompletionAndQuickUndoLifecycle() {
        val originalTask = TaskEntity(
            id = 1,
            title = "Code Review",
            durationMins = 60,
            loggedMins = 20,
            isCompleted = false
        )

        // 1. Task Completion
        val completedTask = originalTask.copy(
            isCompleted = true,
            preCompletionLoggedMins = originalTask.loggedMins,
            loggedMins = originalTask.durationMins
        )
        assertTrue(completedTask.isCompleted)
        assertEquals(60, completedTask.loggedMins)
        assertEquals(20, completedTask.preCompletionLoggedMins)

        // 2. Instant Undo via Toast: Reverts to last logged time (preCompletionLoggedMins)
        val undoneTask = completedTask.copy(
            isCompleted = false,
            loggedMins = completedTask.preCompletionLoggedMins
        )
        assertFalse(undoneTask.isCompleted)
        assertEquals(20, undoneTask.loggedMins)

        // Remaining time increases back from 0 to 40
        val remainingAfterUndo = (undoneTask.durationMins - undoneTask.loggedMins).coerceAtLeast(0)
        assertEquals(40, remainingAfterUndo)
    }

    @Test
    fun testTaskManualUncompleteFromCompletedTab() {
        val completedTask = TaskEntity(
            id = 2,
            title = "Morning Workout",
            durationMins = 45,
            loggedMins = 45,
            preCompletionLoggedMins = 15,
            isCompleted = true
        )

        // Manual uncomplete in Completed tab: Reverts to initial stage (0 logged time, full remaining duration)
        val manuallyUncompletedTask = completedTask.copy(
            isCompleted = false,
            loggedMins = 0,
            preCompletionLoggedMins = 0
        )
        assertFalse(manuallyUncompletedTask.isCompleted)
        assertEquals(0, manuallyUncompletedTask.loggedMins)
        assertEquals(0, manuallyUncompletedTask.preCompletionLoggedMins)

        // Remaining duration increases back to the full 45 mins
        val remaining = (manuallyUncompletedTask.durationMins - manuallyUncompletedTask.loggedMins).coerceAtLeast(0)
        assertEquals(45, remaining)
    }

    @Test
    fun testDailyTaskRolloverLogic() {
        val today = DateUtils.getTodayIso()
        val yesterday = "2026-10-18"

        // Daily task completed yesterday should roll over today
        val completedYesterdayDaily = TaskEntity(
            id = 10,
            title = "Morning Jog",
            isRecurring = true,
            repeatDays = listOf(1, 2, 3, 4, 5, 6, 7),
            isCompleted = true,
            lastCompletedDate = yesterday
        )
        assertTrue(DateUtils.isRecurringTaskDueForRollover(completedYesterdayDaily, today))

        // Daily task completed today should NOT roll over today
        val completedTodayDaily = completedYesterdayDaily.copy(lastCompletedDate = today)
        assertFalse(DateUtils.isRecurringTaskDueForRollover(completedTodayDaily, today))
    }

    @Test
    fun testSpecificDayRecurrenceRolloverAndCarryOver() {
        val todayDow = DateUtils.getTodayDayOfWeek()
        val todayIso = DateUtils.getTodayIso()

        // 1. Task scheduled for today (e.g., todayDow in repeatDays)
        val taskScheduledToday = TaskEntity(
            id = 20,
            title = "Scheduled Today Task",
            isRecurring = true,
            repeatDays = listOf(todayDow),
            isCompleted = false
        )
        assertTrue(DateUtils.isTaskActiveForToday(taskScheduledToday, todayIso))

        // If completed today, it should NOT be active for today anymore
        val completedToday = taskScheduledToday.copy(isCompleted = true, lastCompletedDate = todayIso)
        assertFalse(DateUtils.isTaskActiveForToday(completedToday, todayIso))
        assertFalse(DateUtils.isRecurringTaskDueForRollover(completedToday, todayIso))

        // 2. Off-day task: scheduled for a different day of the week
        val otherDow = if (todayDow == 7) 1 else todayDow + 1
        val offDayTask = TaskEntity(
            id = 21,
            title = "Future Scheduled Task",
            isRecurring = true,
            repeatDays = listOf(otherDow),
            isCompleted = false,
            createdAt = System.currentTimeMillis() // created today
        )
        // Created today for future weekday: should NOT be active today
        assertFalse(DateUtils.isTaskActiveForToday(offDayTask, todayIso))

        // 3. Schedule labels
        assertEquals("Daily", DateUtils.getRecurringScheduleLabel(emptyList()))
        assertEquals("Daily", DateUtils.getRecurringScheduleLabel(listOf(1, 2, 3, 4, 5, 6, 7)))
        assertEquals("Weekdays", DateUtils.getRecurringScheduleLabel(listOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun testBackupSerializationAndDeserialization() {
        val workspaces = listOf(
            WorkspaceEntity(id = "personal", name = "Personal", colorHex = "#4F46E5", isDefault = true),
            WorkspaceEntity(id = "work", name = "Work", colorHex = "#059669", isDefault = false)
        )
        val tasks = listOf(
            TaskEntity(id = 1, title = "Task 1", workspaceId = "personal"),
            TaskEntity(id = 2, title = "Task 2", workspaceId = "work")
        )
        val projects = listOf(
            ProjectEntity(id = 10, title = "Project 1", workspaceId = "work")
        )
        val courses = listOf(
            CourseEntity(id = 20, title = "Course 1", workspaceId = "personal")
        )
        val logs = listOf(
            DailyLogEntity(date = "2026-10-19", workspaceId = "personal", totalLoggedMins = 45)
        )

        val originalData = BackupData(
            version = 1,
            exportedAt = 123456789L,
            appName = "TaskFlow",
            workspaces = workspaces,
            tasks = tasks,
            projects = projects,
            courses = courses,
            dailyLogs = logs
        )

        val json = BackupHelper.serializeToJson(originalData)
        assertTrue(json.contains("TaskFlow"))
        assertTrue(json.contains("personal"))
        assertTrue(json.contains("Task 1"))

        val restoredData = BackupHelper.deserializeFromJson(json)
        assertEquals(2, restoredData.workspaces.size)
        assertEquals("Personal", restoredData.workspaces[0].name)
        assertEquals(2, restoredData.tasks.size)
        assertEquals("work", restoredData.tasks[1].workspaceId)
        assertEquals(1, restoredData.projects.size)
        assertEquals(1, restoredData.courses.size)
        assertEquals(1, restoredData.dailyLogs.size)
        assertEquals(45, restoredData.dailyLogs[0].totalLoggedMins)
    }

    @Test
    fun testWorkspaceDataSeparation() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "Personal Gym", workspaceId = "personal"),
            TaskEntity(id = 2, title = "Work Sprint", workspaceId = "work"),
            TaskEntity(id = 3, title = "Grocery Shopping", workspaceId = "personal")
        )

        val personalTasks = tasks.filter { it.workspaceId == "personal" }
        val workTasks = tasks.filter { it.workspaceId == "work" }

        assertEquals(2, personalTasks.size)
        assertEquals(listOf(1L, 3L), personalTasks.map { it.id })

        assertEquals(1, workTasks.size)
        assertEquals(listOf(2L), workTasks.map { it.id })
    }
}

