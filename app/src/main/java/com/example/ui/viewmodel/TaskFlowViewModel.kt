package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CalendarEventEntity
import com.example.data.model.CourseEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.Subtask
import com.example.data.model.TaskEntity
import com.example.data.model.WorkspaceEntity
import com.example.data.repository.TaskFlowRepository
import com.example.util.BackupData
import com.example.util.BackupHelper
import com.example.util.DateUtils
import com.example.util.RestoreMode
import com.example.util.SoundHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab(val title: String) {
    TODAY("Today"),
    TASKS("Tasks"),
    CALENDAR("Calendar"),
    PROJECTS("Projects"),
    COURSES("Courses")
}

data class DailyCapacity(
    val totalScheduledMins: Int,
    val totalLoggedMins: Int,
    val remainingMins: Int,
    val progressPct: Int,
    val tasksCount: Int,
    val completedCount: Int
)

data class DayHistoryItem(
    val dateIso: String,
    val shortDay: String,
    val scheduledMins: Int,
    val loggedMins: Int,
    val progressPct: Int,
    val isToday: Boolean
)

data class TaskFlowUiState(
    val currentTab: AppTab = AppTab.TODAY,
    val allWorkspaces: List<WorkspaceEntity> = emptyList(),
    val currentWorkspace: WorkspaceEntity? = null,
    val isAddWorkspaceDialogOpen: Boolean = false,
    val isBackupDialogOpen: Boolean = false,
    val backupOperationMessage: String? = null,
    val pendingRestoreBackup: BackupData? = null,
    val allTasks: List<TaskEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val todayP1Tasks: List<TaskEntity> = emptyList(),
    val allCalendarEvents: List<CalendarEventEntity> = emptyList(),
    val todayCalendarEvents: List<CalendarEventEntity> = emptyList(),
    val selectedCalendarDateIso: String = DateUtils.getTodayIso(),
    val calendarFilter: String = "All", // "All", "Events", "Appointments", "Tasks"
    val calendarSyncMessage: String? = null,
    val allProjects: List<ProjectEntity> = emptyList(),
    val p1Projects: List<ProjectEntity> = emptyList(),
    val allCourses: List<CourseEntity> = emptyList(),
    val activeCourses: List<CourseEntity> = emptyList(),
    val dailyCapacity: DailyCapacity = DailyCapacity(0, 0, 0, 0, 0, 0),
    val weeklyHistory: List<DayHistoryItem> = emptyList(),
    val currentStreak: Int = 0,
    val taskFilter: String = "Today", // "Today", "Upcoming", "Priority P1", "Completed", "All"
    val taskSearchQuery: String = "",
    val recentCompletedTask: TaskEntity? = null
)

private data class TaskData(
    val tasks: List<TaskEntity>,
    val logs: List<DailyLogEntity>,
    val calendarEvents: List<CalendarEventEntity>,
    val recentCompletedTask: TaskEntity? = null
)

private data class ProjectsAndCourses(
    val allProjects: List<ProjectEntity>,
    val p1Projects: List<ProjectEntity>,
    val allCourses: List<CourseEntity>,
    val activeCourses: List<CourseEntity>
)

private data class CalendarViewConfig(
    val selectedDateIso: String,
    val calendarFilter: String,
    val syncMessage: String?
)

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskFlowRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskFlowRepository(db, application)
        viewModelScope.launch {
            repository.ensureDefaultWorkspaces()
            repository.rollRecurringTasksIfNeeded()
            repository.syncWithGoogleCalendar(_selectedWorkspaceId.value)
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                repository.rollRecurringTasksIfNeeded()
            }
        }
    }

    fun refreshRecurringTasks() {
        viewModelScope.launch {
            repository.rollRecurringTasksIfNeeded()
        }
    }

    private val _currentTab = MutableStateFlow(AppTab.TODAY)
    private val _taskFilter = MutableStateFlow("Today")
    private val _taskSearchQuery = MutableStateFlow("")
    private val _selectedWorkspaceId = MutableStateFlow("personal")

    // Calendar UI states
    private val _selectedCalendarDateIso = MutableStateFlow(DateUtils.getTodayIso())
    private val _calendarFilter = MutableStateFlow("All")
    private val _calendarSyncMessage = MutableStateFlow<String?>(null)

    // Backup & Workspace UI states
    private val _isAddWorkspaceDialogOpen = MutableStateFlow(false)
    private val _isBackupDialogOpen = MutableStateFlow(false)
    private val _backupOperationMessage = MutableStateFlow<String?>(null)
    private val _pendingRestoreBackup = MutableStateFlow<BackupData?>(null)

    // Dialog & Interaction states
    var editingTask = MutableStateFlow<TaskEntity?>(null)
        private set
    var isTaskDialogOpen = MutableStateFlow(false)
        private set

    var loggingTimeTask = MutableStateFlow<TaskEntity?>(null)
        private set

    var editingCalendarEvent = MutableStateFlow<CalendarEventEntity?>(null)
        private set
    var isCalendarEventDialogOpen = MutableStateFlow(false)
        private set
    var newCalendarEventDefaultType = MutableStateFlow("EVENT")
        private set
    var newCalendarEventDefaultDate = MutableStateFlow(DateUtils.getTodayIso())
        private set

    var editingProject = MutableStateFlow<ProjectEntity?>(null)
        private set
    var isProjectDialogOpen = MutableStateFlow(false)
        private set

    var editingCourse = MutableStateFlow<CourseEntity?>(null)
        private set
    var isCourseDialogOpen = MutableStateFlow(false)
        private set

    var selectedTaskId = MutableStateFlow<Long?>(null)
        private set
    var selectedCalendarEventId = MutableStateFlow<Long?>(null)
        private set
    var selectedProjectId = MutableStateFlow<Long?>(null)
        private set
    var selectedCourseId = MutableStateFlow<Long?>(null)
        private set

    fun openTaskDetail(task: TaskEntity) {
        selectedTaskId.value = task.id
    }

    fun closeTaskDetail() {
        selectedTaskId.value = null
    }

    fun openCalendarEventDetail(event: CalendarEventEntity) {
        selectedCalendarEventId.value = event.id
    }

    fun closeCalendarEventDetail() {
        selectedCalendarEventId.value = null
    }

    fun openProjectDetail(project: ProjectEntity) {
        selectedProjectId.value = project.id
    }

    fun closeProjectDetail() {
        selectedProjectId.value = null
    }

    fun openCourseDetail(course: CourseEntity) {
        selectedCourseId.value = course.id
    }

    fun closeCourseDetail() {
        selectedCourseId.value = null
    }

    private val _recentCompletedTask = MutableStateFlow<TaskEntity?>(null)
    private var clearToastJob: Job? = null

    private val taskDataFlow = combine(
        repository.allTasks,
        repository.recentDailyLogs,
        repository.allCalendarEvents,
        _recentCompletedTask
    ) { tasks, logs, events, recent ->
        TaskData(tasks, logs, events, recent)
    }

    private val projectsAndCoursesFlow = combine(
        repository.allProjects,
        repository.p1Projects,
        repository.allCourses,
        repository.activeCourses
    ) { allProjects, p1Projects, allCourses, activeCourses ->
        ProjectsAndCourses(allProjects, p1Projects, allCourses, activeCourses)
    }

    private val calendarConfigFlow = combine(
        _selectedCalendarDateIso,
        _calendarFilter,
        _calendarSyncMessage
    ) { selectedDate, filter, msg ->
        CalendarViewConfig(selectedDate, filter, msg)
    }

    val uiState: StateFlow<TaskFlowUiState> = combine(
        combine(
            _currentTab,
            _taskFilter,
            _taskSearchQuery,
            _selectedWorkspaceId,
            repository.allWorkspaces
        ) { tab, filter, query, workspaceId, workspaces ->
            Tuple5(tab, filter, query, workspaceId, workspaces)
        },
        combine(
            _isAddWorkspaceDialogOpen,
            _isBackupDialogOpen,
            _backupOperationMessage,
            _pendingRestoreBackup,
            calendarConfigFlow
        ) { addW, backup, msg, pending, calCfg ->
            Tuple5(addW, backup, msg, pending, calCfg)
        },
        taskDataFlow,
        projectsAndCoursesFlow
    ) { t1, t2, taskData, pcData ->
        val currentTab = t1.a
        val filter = t1.b
        val query = t1.c
        val workspaceId = t1.d
        val workspaces = t1.e

        val isAddWorkspaceDialogOpen = t2.a
        val isBackupDialogOpen = t2.b
        val backupOperationMessage = t2.c
        val pendingRestoreBackup = t2.d
        val calConfig = t2.e

        val currentWorkspace = workspaces.find { it.id == workspaceId }
            ?: workspaces.firstOrNull { it.isDefault }
            ?: workspaces.firstOrNull()
        val effectiveWorkspaceId = currentWorkspace?.id ?: workspaceId

        // Filter all entities by effective workspace
        val tasks = taskData.tasks.filter { it.workspaceId == effectiveWorkspaceId }
        val logs = taskData.logs.filter { it.workspaceId == effectiveWorkspaceId }
        val calendarEvents = taskData.calendarEvents.filter { it.workspaceId == effectiveWorkspaceId }
        val projects = pcData.allProjects.filter { it.workspaceId == effectiveWorkspaceId }
        val p1Projects = pcData.p1Projects.filter { it.workspaceId == effectiveWorkspaceId }
        val courses = pcData.allCourses.filter { it.workspaceId == effectiveWorkspaceId }
        val activeCourses = pcData.activeCourses.filter { it.workspaceId == effectiveWorkspaceId }

        val todayIso = DateUtils.getTodayIso()
        val todayCalendarEvents = calendarEvents.filter { it.dateIso == todayIso }

        val completedTodayTasks = tasks.filter { it.isCompleted && it.lastCompletedDate == todayIso }
        // Today's active tasks: ONLY uncompleted tasks active for today (scheduled or overdue carry-over)
        val activeTodayTasks = tasks.filter { task ->
            !task.isCompleted && DateUtils.isTaskActiveForToday(task, todayIso)
        }

        // Daily Capacity Tracker calculations
        val allTodayTasksForStats = activeTodayTasks + completedTodayTasks
        val totalScheduledMins = allTodayTasksForStats.sumOf { it.durationMins }
        val totalLoggedMins = allTodayTasksForStats.sumOf { it.loggedMins }
        val remainingMins = activeTodayTasks.sumOf { (it.durationMins - it.loggedMins).coerceAtLeast(0) }
        val progressPct = if (totalScheduledMins > 0) {
            ((totalLoggedMins.toFloat() / totalScheduledMins) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val completedCount = completedTodayTasks.size

        val capacity = DailyCapacity(
            totalScheduledMins = totalScheduledMins,
            totalLoggedMins = totalLoggedMins,
            remainingMins = remainingMins,
            progressPct = progressPct,
            tasksCount = allTodayTasksForStats.size,
            completedCount = completedCount
        )

        // Past 7 Days History
        val last7Days = DateUtils.getLast7DaysIso()
        val logsMap = logs.associateBy { it.date }
        val historyItems = last7Days.map { dateStr ->
            val isToday = dateStr == todayIso
            val sched = if (isToday) totalScheduledMins else (logsMap[dateStr]?.totalScheduledMins ?: 0)
            val logged = if (isToday) totalLoggedMins else (logsMap[dateStr]?.totalLoggedMins ?: 0)
            val pct = if (sched > 0) ((logged.toFloat() / sched) * 100).toInt().coerceIn(0, 100) else if (logged > 0) 100 else 0

            DayHistoryItem(
                dateIso = dateStr,
                shortDay = DateUtils.getShortDayName(dateStr),
                scheduledMins = sched,
                loggedMins = logged,
                progressPct = pct,
                isToday = isToday
            )
        }

        // Streak calculation
        var streak = 0
        for (item in historyItems.reversed()) {
            if (item.loggedMins > 0 || item.progressPct > 0) {
                streak++
            } else if (!item.isToday) {
                break
            }
        }

        val todayP1Tasks = activeTodayTasks.filter { it.priority == 1 }

        TaskFlowUiState(
            currentTab = currentTab,
            allWorkspaces = workspaces,
            currentWorkspace = currentWorkspace,
            isAddWorkspaceDialogOpen = isAddWorkspaceDialogOpen,
            isBackupDialogOpen = isBackupDialogOpen,
            backupOperationMessage = backupOperationMessage,
            pendingRestoreBackup = pendingRestoreBackup,
            allTasks = tasks,
            todayTasks = activeTodayTasks,
            todayP1Tasks = todayP1Tasks,
            allCalendarEvents = calendarEvents,
            todayCalendarEvents = todayCalendarEvents,
            selectedCalendarDateIso = calConfig.selectedDateIso,
            calendarFilter = calConfig.calendarFilter,
            calendarSyncMessage = calConfig.syncMessage,
            allProjects = projects,
            p1Projects = p1Projects,
            allCourses = courses,
            activeCourses = activeCourses,
            dailyCapacity = capacity,
            weeklyHistory = historyItems,
            currentStreak = streak,
            taskFilter = filter,
            taskSearchQuery = query,
            recentCompletedTask = taskData.recentCompletedTask
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskFlowUiState()
    )

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.TODAY || tab == AppTab.TASKS || tab == AppTab.CALENDAR) {
            refreshRecurringTasks()
        }
    }

    fun setTaskFilter(filter: String) {
        _taskFilter.value = filter
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    // Calendar actions
    fun setSelectedCalendarDate(dateIso: String) {
        _selectedCalendarDateIso.value = dateIso
    }

    fun setCalendarFilter(filter: String) {
        _calendarFilter.value = filter
    }

    fun syncGoogleCalendarNow() {
        viewModelScope.launch {
            val result = repository.syncWithGoogleCalendar(_selectedWorkspaceId.value)
            _calendarSyncMessage.value = result.message
        }
    }

    fun clearCalendarSyncMessage() {
        _calendarSyncMessage.value = null
    }

    fun openAddCalendarEventDialog(
        defaultType: String = "EVENT",
        defaultDateIso: String? = null
    ) {
        newCalendarEventDefaultType.value = defaultType
        newCalendarEventDefaultDate.value = defaultDateIso ?: _selectedCalendarDateIso.value
        editingCalendarEvent.value = null
        isCalendarEventDialogOpen.value = true
    }

    fun openEditCalendarEventDialog(event: CalendarEventEntity) {
        editingCalendarEvent.value = event
        isCalendarEventDialogOpen.value = true
    }

    fun closeCalendarEventDialog() {
        editingCalendarEvent.value = null
        isCalendarEventDialogOpen.value = false
    }

    fun saveCalendarEvent(
        title: String,
        notes: String,
        dateIso: String,
        startTime: String,
        endTime: String,
        type: String
    ) {
        viewModelScope.launch {
            val existing = editingCalendarEvent.value
            val currentWorkspaceId = _selectedWorkspaceId.value
            val toSave = if (existing != null) {
                existing.copy(
                    title = title,
                    notes = notes,
                    dateIso = dateIso,
                    startTime = startTime,
                    endTime = endTime,
                    type = type,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                CalendarEventEntity(
                    title = title,
                    notes = notes,
                    dateIso = dateIso,
                    startTime = startTime,
                    endTime = endTime,
                    type = type,
                    workspaceId = currentWorkspaceId
                )
            }
            repository.saveCalendarEvent(toSave)
            closeCalendarEventDialog()
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(event)
        }
    }

    fun toggleCalendarEventCompletion(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.toggleCalendarEventCompletion(event)
        }
    }

    // Workspace actions
    fun selectWorkspace(workspaceId: String) {
        _selectedWorkspaceId.value = workspaceId
    }

    fun openAddWorkspaceDialog() {
        _isAddWorkspaceDialogOpen.value = true
    }

    fun closeAddWorkspaceDialog() {
        _isAddWorkspaceDialogOpen.value = false
    }

    fun createWorkspace(name: String, colorHex: String, iconName: String) {
        if (name.isBlank()) return
        val newId = UUID.randomUUID().toString()
        val workspace = WorkspaceEntity(
            id = newId,
            name = name.trim(),
            colorHex = colorHex,
            iconName = iconName,
            isDefault = false
        )
        viewModelScope.launch {
            repository.saveWorkspace(workspace)
            _selectedWorkspaceId.value = newId
            closeAddWorkspaceDialog()
        }
    }

    fun deleteWorkspace(workspace: WorkspaceEntity) {
        viewModelScope.launch {
            repository.deleteWorkspace(workspace)
            _selectedWorkspaceId.value = "personal"
        }
    }

    // Backup & Restore actions
    fun openBackupDialog() {
        _backupOperationMessage.value = null
        _pendingRestoreBackup.value = null
        _isBackupDialogOpen.value = true
    }

    fun closeBackupDialog() {
        _isBackupDialogOpen.value = false
        _backupOperationMessage.value = null
        _pendingRestoreBackup.value = null
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val backupData = repository.createBackupData()
            val result = BackupHelper.exportToUri(context, uri, backupData)
            if (result.isSuccess) {
                _backupOperationMessage.value = "Backup successfully exported!"
                SoundHelper.playCompletionChime(getApplication())
            } else {
                _backupOperationMessage.value = "Export failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun prepareRestore(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = BackupHelper.readFromUri(context, uri)
            if (result.isSuccess) {
                _pendingRestoreBackup.value = result.getOrNull()
                _backupOperationMessage.value = null
            } else {
                _backupOperationMessage.value = "Failed to parse backup: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun confirmRestore(mode: RestoreMode) {
        val data = _pendingRestoreBackup.value ?: return
        viewModelScope.launch {
            repository.restoreBackupData(data, mode)
            _pendingRestoreBackup.value = null
            _backupOperationMessage.value = "Backup restored successfully (${if (mode == RestoreMode.MERGE) "Merged" else "Replaced"})!"
            SoundHelper.playCompletionChime(getApplication())
        }
    }

    fun cancelRestorePreview() {
        _pendingRestoreBackup.value = null
    }

    // Task actions
    var newTaskDefaultPriority = MutableStateFlow(2)
        private set
    var newTaskDefaultDateIso = MutableStateFlow<String?>(null)
        private set

    fun openAddTaskDialog(priority: Int = 2, scheduledDate: String? = null) {
        newTaskDefaultPriority.value = priority
        newTaskDefaultDateIso.value = scheduledDate
        editingTask.value = null
        isTaskDialogOpen.value = true
    }

    fun openEditTaskDialog(task: TaskEntity) {
        editingTask.value = task
        isTaskDialogOpen.value = true
    }

    fun closeTaskDialog() {
        editingTask.value = null
        isTaskDialogOpen.value = false
    }

    fun saveTask(
        title: String,
        description: String,
        time: String?,
        durationMins: Int,
        priority: Int,
        isRecurring: Boolean,
        repeatDays: List<Int>,
        subtasks: List<Subtask>,
        scheduledDate: String? = DateUtils.getTodayIso()
    ) {
        viewModelScope.launch {
            val existing = editingTask.value
            val currentWorkspaceId = _selectedWorkspaceId.value
            val effectiveScheduledDate = if (isRecurring) null else (scheduledDate ?: DateUtils.getTodayIso())
            val taskToSave = if (existing != null) {
                existing.copy(
                    title = title,
                    description = description,
                    time = time,
                    durationMins = durationMins,
                    priority = priority,
                    isRecurring = isRecurring,
                    repeatDays = repeatDays,
                    scheduledDate = effectiveScheduledDate,
                    subtasks = subtasks
                )
            } else {
                TaskEntity(
                    title = title,
                    description = description,
                    time = time,
                    durationMins = durationMins,
                    priority = priority,
                    isRecurring = isRecurring,
                    repeatDays = repeatDays,
                    scheduledDate = effectiveScheduledDate,
                    subtasks = subtasks,
                    workspaceId = currentWorkspaceId
                )
            }
            repository.saveTask(taskToSave)
            closeTaskDialog()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            if (task.isCompleted) {
                if (_recentCompletedTask.value?.id == task.id) {
                    clearToastJob?.cancel()
                    _recentCompletedTask.value = null
                }
                repository.uncompleteTaskManually(task)
            } else {
                completeTaskWithCelebration(task)
            }
        }
    }

    fun completeTaskWithCelebration(task: TaskEntity) {
        viewModelScope.launch {
            SoundHelper.playCompletionChime(getApplication())
            val completed = repository.completeTask(task)
            _recentCompletedTask.value = completed
            clearToastJob?.cancel()
            clearToastJob = viewModelScope.launch {
                delay(2000)
                _recentCompletedTask.value = null
            }
        }
    }

    fun undoRecentTaskCompletion() {
        val task = _recentCompletedTask.value ?: return
        clearToastJob?.cancel()
        _recentCompletedTask.value = null
        viewModelScope.launch {
            repository.undoRecentCompletion(task)
        }
    }

    fun dismissCompletionToast() {
        clearToastJob?.cancel()
        _recentCompletedTask.value = null
    }

    fun uncompleteTaskManually(task: TaskEntity) {
        viewModelScope.launch {
            if (_recentCompletedTask.value?.id == task.id) {
                clearToastJob?.cancel()
                _recentCompletedTask.value = null
            }
            repository.uncompleteTaskManually(task)
        }
    }

    fun openLogTimeDialog(task: TaskEntity) {
        loggingTimeTask.value = task
    }

    fun closeLogTimeDialog() {
        loggingTimeTask.value = null
    }

    fun logTimeForTask(task: TaskEntity, addedMins: Int) {
        viewModelScope.launch {
            repository.logTimeForTask(task, addedMins)
            closeLogTimeDialog()
        }
    }

    fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(task, subtaskId)
        }
    }

    fun addSubtask(task: TaskEntity, title: String) {
        viewModelScope.launch {
            repository.addSubtask(task, title)
        }
    }

    // Project actions
    fun openAddProjectDialog() {
        editingProject.value = null
        isProjectDialogOpen.value = true
    }

    fun openEditProjectDialog(project: ProjectEntity) {
        editingProject.value = project
        isProjectDialogOpen.value = true
    }

    fun closeProjectDialog() {
        editingProject.value = null
        isProjectDialogOpen.value = false
    }

    fun saveProject(
        title: String,
        description: String,
        priority: Int,
        progressPct: Int
    ) {
        viewModelScope.launch {
            val existing = editingProject.value
            val currentWorkspaceId = _selectedWorkspaceId.value
            val projectToSave = if (existing != null) {
                existing.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    progressPct = progressPct,
                    isCompleted = progressPct >= 100
                )
            } else {
                ProjectEntity(
                    title = title,
                    description = description,
                    priority = priority,
                    progressPct = progressPct,
                    isCompleted = progressPct >= 100,
                    workspaceId = currentWorkspaceId
                )
            }
            repository.saveProject(projectToSave)
            closeProjectDialog()
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun adjustProjectProgress(project: ProjectEntity, deltaPct: Int) {
        viewModelScope.launch {
            repository.adjustProjectProgress(project, deltaPct)
        }
    }

    fun setProjectProgress(project: ProjectEntity, targetPct: Int) {
        viewModelScope.launch {
            repository.setProjectProgress(project, targetPct)
        }
    }

    fun toggleProjectCompletion(project: ProjectEntity) {
        viewModelScope.launch {
            repository.toggleProjectCompletion(project)
        }
    }

    // Course actions
    fun openAddCourseDialog() {
        editingCourse.value = null
        isCourseDialogOpen.value = true
    }

    fun openEditCourseDialog(course: CourseEntity) {
        editingCourse.value = course
        isCourseDialogOpen.value = true
    }

    fun closeCourseDialog() {
        editingCourse.value = null
        isCourseDialogOpen.value = false
    }

    fun saveCourse(
        title: String,
        description: String,
        platformOrAuthor: String,
        totalModules: Int,
        progressPct: Int
    ) {
        viewModelScope.launch {
            val existing = editingCourse.value
            val currentWorkspaceId = _selectedWorkspaceId.value
            val completedModules = ((progressPct.toFloat() / 100f) * totalModules).toInt()
            val courseToSave = if (existing != null) {
                existing.copy(
                    title = title,
                    description = description,
                    platformOrAuthor = platformOrAuthor,
                    totalModules = totalModules,
                    completedModules = completedModules,
                    progressPct = progressPct,
                    isCompleted = progressPct >= 100
                )
            } else {
                CourseEntity(
                    title = title,
                    description = description,
                    platformOrAuthor = platformOrAuthor,
                    totalModules = totalModules,
                    completedModules = completedModules,
                    progressPct = progressPct,
                    isCompleted = progressPct >= 100,
                    workspaceId = currentWorkspaceId
                )
            }
            repository.saveCourse(courseToSave)
            closeCourseDialog()
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    fun adjustCourseProgress(course: CourseEntity, deltaPct: Int) {
        viewModelScope.launch {
            repository.adjustCourseProgress(course, deltaPct)
        }
    }

    fun setCourseProgress(course: CourseEntity, targetPct: Int) {
        viewModelScope.launch {
            repository.setCourseProgress(course, targetPct)
        }
    }

    fun toggleCourseCompletion(course: CourseEntity) {
        viewModelScope.launch {
            repository.toggleCourseCompletion(course)
        }
    }

    fun adjustCourseModules(course: CourseEntity, deltaModules: Int) {
        viewModelScope.launch {
            repository.adjustCourseModules(course, deltaModules)
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
