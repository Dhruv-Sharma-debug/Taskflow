package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.CourseEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.Subtask
import com.example.data.model.TaskEntity
import com.example.data.model.WorkspaceEntity
import com.example.util.BackupData
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.RestoreMode
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.roundToInt

class TaskFlowRepository(
    private val database: AppDatabase,
    private val appContext: Context
) {
    private val workspaceDao = database.workspaceDao()
    private val taskDao = database.taskDao()
    private val projectDao = database.projectDao()
    private val courseDao = database.courseDao()
    private val dailyLogDao = database.dailyLogDao()

    val allWorkspaces: Flow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val p1Projects: Flow<List<ProjectEntity>> = projectDao.getP1Projects()
    val allCourses: Flow<List<CourseEntity>> = courseDao.getAllCourses()
    val activeCourses: Flow<List<CourseEntity>> = courseDao.getActiveCourses()
    val recentDailyLogs: Flow<List<DailyLogEntity>> = dailyLogDao.getRecentLogs(60)

    suspend fun ensureDefaultWorkspaces() {
        val existing = workspaceDao.getAllWorkspacesList()
        if (existing.isEmpty()) {
            val personal = WorkspaceEntity(
                id = "personal",
                name = "Personal",
                colorHex = "#4F46E5",
                iconName = "person",
                isDefault = true
            )
            val work = WorkspaceEntity(
                id = "work",
                name = "Work",
                colorHex = "#059669",
                iconName = "work",
                isDefault = false
            )
            workspaceDao.insertWorkspaces(listOf(personal, work))
        }
    }

    suspend fun saveWorkspace(workspace: WorkspaceEntity) {
        workspaceDao.insertWorkspace(workspace)
    }

    suspend fun deleteWorkspace(workspace: WorkspaceEntity) {
        taskDao.deleteTasksByWorkspace(workspace.id)
        projectDao.deleteProjectsByWorkspace(workspace.id)
        courseDao.deleteCoursesByWorkspace(workspace.id)
        workspaceDao.deleteWorkspace(workspace)
        ensureDefaultWorkspaces()
    }

    suspend fun saveTask(task: TaskEntity): Long {
        val id = taskDao.insertTask(task)
        val savedTask = if (task.id == 0L) task.copy(id = id) else task
        if (!savedTask.isCompleted && !savedTask.time.isNullOrBlank()) {
            NotificationHelper.scheduleTaskReminder(appContext, savedTask)
        }
        return id
    }

    suspend fun deleteTask(task: TaskEntity) {
        NotificationHelper.cancelTaskReminder(appContext, task.id)
        taskDao.deleteTask(task)
    }

    suspend fun rollRecurringTasksIfNeeded() {
        val today = DateUtils.getTodayIso()
        val allTasks = taskDao.getAllTasksList()
        for (task in allTasks) {
            if (task.isRecurring && DateUtils.isRecurringTaskDueForRollover(task, today)) {
                val reset = task.copy(
                    isCompleted = false,
                    loggedMins = 0,
                    completedAt = null,
                    subtasks = task.subtasks.map { it.copy(isCompleted = false) }
                )
                taskDao.updateTask(reset)
                if (!reset.time.isNullOrBlank()) {
                    NotificationHelper.scheduleTaskReminder(appContext, reset)
                }
            }
        }
    }

    suspend fun completeTask(task: TaskEntity): TaskEntity {
        val today = DateUtils.getTodayIso()
        NotificationHelper.cancelTaskReminder(appContext, task.id)
        val addedMins = (task.durationMins - task.loggedMins).coerceAtLeast(0)
        val updated = task.copy(
            isCompleted = true,
            preCompletionLoggedMins = task.loggedMins,
            loggedMins = task.durationMins,
            completedAt = System.currentTimeMillis(),
            lastCompletedDate = today
        )
        taskDao.updateTask(updated)
        recordTodayCompletion(addedMins, task.workspaceId)
        return updated
    }

    suspend fun undoRecentCompletion(task: TaskEntity): TaskEntity {
        val restoredLoggedMins = task.preCompletionLoggedMins
        val removedMins = (task.durationMins - restoredLoggedMins).coerceAtLeast(0)
        val updated = task.copy(
            isCompleted = false,
            loggedMins = restoredLoggedMins,
            completedAt = null,
            lastCompletedDate = null
        )
        taskDao.updateTask(updated)
        if (!updated.time.isNullOrBlank()) {
            NotificationHelper.scheduleTaskReminder(appContext, updated)
        }
        revertTodayCompletion(removedMins, task.workspaceId)
        return updated
    }

    suspend fun uncompleteTaskManually(task: TaskEntity): TaskEntity {
        val removedMins = task.loggedMins.coerceAtLeast(0)
        val updated = task.copy(
            isCompleted = false,
            loggedMins = 0,
            preCompletionLoggedMins = 0,
            completedAt = null,
            lastCompletedDate = null
        )
        taskDao.updateTask(updated)
        if (!updated.time.isNullOrBlank()) {
            NotificationHelper.scheduleTaskReminder(appContext, updated)
        }
        revertTodayCompletion(removedMins, task.workspaceId)
        return updated
    }

    suspend fun toggleTaskCompletion(task: TaskEntity): TaskEntity {
        return if (task.isCompleted) {
            uncompleteTaskManually(task)
        } else {
            completeTask(task)
        }
    }

    suspend fun logTimeForTask(task: TaskEntity, addedMins: Int) {
        val newLogged = (task.loggedMins + addedMins).coerceAtLeast(0)
        val autoCompleted = newLogged >= task.durationMins
        val today = DateUtils.getTodayIso()

        if (autoCompleted) {
            NotificationHelper.cancelTaskReminder(appContext, task.id)
            val completed = task.copy(
                loggedMins = newLogged,
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                lastCompletedDate = today
            )
            taskDao.updateTask(completed)
            recordTodayCompletion(addedMins, task.workspaceId)
        } else {
            val updated = task.copy(loggedMins = newLogged)
            taskDao.updateTask(updated)
            recordTodayLoggedTime(addedMins, task.workspaceId)
        }
    }

    suspend fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val updated = task.copy(subtasks = updatedSubtasks)
        taskDao.updateTask(updated)
    }

    suspend fun addSubtask(task: TaskEntity, title: String) {
        if (title.isBlank()) return
        val newSub = Subtask(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            isCompleted = false
        )
        val updated = task.copy(subtasks = task.subtasks + newSub)
        taskDao.updateTask(updated)
    }

    // Projects
    suspend fun saveProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    suspend fun adjustProjectProgress(project: ProjectEntity, deltaPct: Int) {
        setProjectProgress(project, project.progressPct + deltaPct)
    }

    suspend fun setProjectProgress(project: ProjectEntity, targetPct: Int) {
        val newPct = targetPct.coerceIn(0, 100)
        val isCompleted = newPct >= 100
        val updated = project.copy(
            progressPct = newPct,
            isCompleted = isCompleted
        )
        projectDao.updateProject(updated)
    }

    suspend fun toggleProjectCompletion(project: ProjectEntity) {
        val isCompleted = !project.isCompleted
        val newPct = if (isCompleted) 100 else (if (project.progressPct >= 100) 50 else project.progressPct)
        val updated = project.copy(
            isCompleted = isCompleted,
            progressPct = newPct
        )
        projectDao.updateProject(updated)
    }

    // Courses
    suspend fun saveCourse(course: CourseEntity): Long {
        return courseDao.insertCourse(course)
    }

    suspend fun deleteCourse(course: CourseEntity) {
        courseDao.deleteCourse(course)
    }

    suspend fun adjustCourseProgress(course: CourseEntity, deltaPct: Int) {
        setCourseProgress(course, course.progressPct + deltaPct)
    }

    suspend fun setCourseProgress(course: CourseEntity, targetPct: Int) {
        val newPct = targetPct.coerceIn(0, 100)
        val isCompleted = newPct >= 100
        val updatedModules = if (course.totalModules > 0) {
            ((newPct.toFloat() / 100f) * course.totalModules).roundToInt().coerceIn(0, course.totalModules)
        } else 0
        val updated = course.copy(
            progressPct = newPct,
            completedModules = updatedModules,
            isCompleted = isCompleted
        )
        courseDao.updateCourse(updated)
    }

    suspend fun adjustCourseModules(course: CourseEntity, deltaModules: Int) {
        val newModules = (course.completedModules + deltaModules).coerceIn(0, course.totalModules)
        val newPct = if (course.totalModules > 0) {
            ((newModules.toFloat() / course.totalModules) * 100).toInt().coerceIn(0, 100)
        } else 0
        val isCompleted = newModules >= course.totalModules && course.totalModules > 0
        val updated = course.copy(
            completedModules = newModules,
            progressPct = newPct,
            isCompleted = isCompleted
        )
        courseDao.updateCourse(updated)
    }

    suspend fun toggleCourseCompletion(course: CourseEntity) {
        val isCompleted = !course.isCompleted
        val newPct = if (isCompleted) 100 else (if (course.progressPct >= 100) 50 else course.progressPct)
        val updated = course.copy(
            isCompleted = isCompleted,
            progressPct = newPct,
            completedModules = if (isCompleted) course.totalModules else (course.totalModules / 2)
        )
        courseDao.updateCourse(updated)
    }

    // Backup & Restore
    suspend fun createBackupData(): BackupData {
        ensureDefaultWorkspaces()
        return BackupData(
            workspaces = workspaceDao.getAllWorkspacesList(),
            tasks = taskDao.getAllTasksList(),
            projects = projectDao.getAllProjectsList(),
            courses = courseDao.getAllCoursesList(),
            dailyLogs = dailyLogDao.getAllLogsList()
        )
    }

    suspend fun restoreBackupData(backupData: BackupData, mode: RestoreMode) {
        if (mode == RestoreMode.REPLACE) {
            taskDao.clearAllTasks()
            projectDao.clearAllProjects()
            courseDao.clearAllCourses()
            dailyLogDao.clearAllLogs()
            workspaceDao.clearAllWorkspaces()
        }

        // Restore Workspaces
        if (backupData.workspaces.isNotEmpty()) {
            workspaceDao.insertWorkspaces(backupData.workspaces)
        }
        ensureDefaultWorkspaces()

        // Restore Tasks
        if (backupData.tasks.isNotEmpty()) {
            taskDao.insertTasks(backupData.tasks)
            for (task in backupData.tasks) {
                if (!task.isCompleted && !task.time.isNullOrBlank()) {
                    NotificationHelper.scheduleTaskReminder(appContext, task)
                }
            }
        }

        // Restore Projects
        if (backupData.projects.isNotEmpty()) {
            projectDao.insertProjects(backupData.projects)
        }

        // Restore Courses
        if (backupData.courses.isNotEmpty()) {
            courseDao.insertCourses(backupData.courses)
        }

        // Restore Daily Logs
        for (log in backupData.dailyLogs) {
            dailyLogDao.insertOrUpdateLog(log)
        }
    }

    private suspend fun recordTodayCompletion(addedMins: Int, workspaceId: String = "personal") {
        val today = DateUtils.getTodayIso()
        val existing = dailyLogDao.getLogForDate(today, workspaceId)
        if (existing != null) {
            dailyLogDao.insertOrUpdateLog(
                existing.copy(
                    totalLoggedMins = existing.totalLoggedMins + addedMins,
                    tasksCompletedCount = existing.tasksCompletedCount + 1
                )
            )
        } else {
            dailyLogDao.insertOrUpdateLog(
                DailyLogEntity(
                    date = today,
                    workspaceId = workspaceId,
                    totalScheduledMins = 0,
                    totalLoggedMins = addedMins,
                    tasksCompletedCount = 1,
                    totalTasksCount = 1
                )
            )
        }
    }

    private suspend fun revertTodayCompletion(removedMins: Int, workspaceId: String = "personal") {
        val today = DateUtils.getTodayIso()
        val existing = dailyLogDao.getLogForDate(today, workspaceId)
        if (existing != null) {
            dailyLogDao.insertOrUpdateLog(
                existing.copy(
                    totalLoggedMins = (existing.totalLoggedMins - removedMins).coerceAtLeast(0),
                    tasksCompletedCount = (existing.tasksCompletedCount - 1).coerceAtLeast(0)
                )
            )
        }
    }

    private suspend fun recordTodayLoggedTime(addedMins: Int, workspaceId: String = "personal") {
        val today = DateUtils.getTodayIso()
        val existing = dailyLogDao.getLogForDate(today, workspaceId)
        if (existing != null) {
            dailyLogDao.insertOrUpdateLog(
                existing.copy(totalLoggedMins = existing.totalLoggedMins + addedMins)
            )
        } else {
            dailyLogDao.insertOrUpdateLog(
                DailyLogEntity(
                    date = today,
                    workspaceId = workspaceId,
                    totalScheduledMins = 0,
                    totalLoggedMins = addedMins,
                    tasksCompletedCount = 0,
                    totalTasksCount = 0
                )
            )
        }
    }
}
