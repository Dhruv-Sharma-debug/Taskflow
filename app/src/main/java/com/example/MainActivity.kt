package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddEditCourseDialog
import com.example.ui.components.AddEditProjectDialog
import com.example.ui.components.AddEditTaskDialog
import com.example.ui.components.AddWorkspaceDialog
import com.example.ui.components.BackupDialog
import com.example.ui.components.CourseDetailDialog
import com.example.ui.components.LogTimeDialog
import com.example.ui.components.ProjectDetailDialog
import com.example.ui.components.TaskCompletionToast
import com.example.ui.components.TaskDetailDialog
import com.example.ui.components.WorkspaceDropdown
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TodayDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TaskFlowViewModel
import com.example.util.DateUtils
import com.example.util.HomeScreenShortcutHelper
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TaskFlowApp()
            }
        }
    }
}

@Composable
fun TaskFlowApp(viewModel: TaskFlowViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled gracefully */ }

    // File pickers for JSON backup export & restore
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(context, it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.prepareRestore(context, it) }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isNotifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isNotifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Refresh recurring tasks whenever app is resumed or brought to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                viewModel.refreshRecurringTasks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle Back Press to return to Today tab
    if (uiState.currentTab != AppTab.TODAY) {
        BackHandler {
            viewModel.selectTab(AppTab.TODAY)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TaskFlow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Workspace Dropdown Switcher
                    WorkspaceDropdown(
                        currentWorkspace = uiState.currentWorkspace,
                        allWorkspaces = uiState.allWorkspaces,
                        onSelectWorkspace = { viewModel.selectWorkspace(it) },
                        onAddWorkspaceClick = { viewModel.openAddWorkspaceDialog() },
                        onDeleteWorkspace = { viewModel.deleteWorkspace(it) }
                    )

                    // Pin to Home Screen Action Button
                    IconButton(
                        onClick = { HomeScreenShortcutHelper.requestPinToHomeScreen(context) },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("top_bar_pin_home_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddToHomeScreen,
                            contentDescription = "Pin to Home Screen",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Data Backup & Restore Action Button
                    IconButton(
                        onClick = { viewModel.openBackupDialog() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("top_bar_backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup & Restore",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Today Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.TODAY,
                    onClick = { viewModel.selectTab(AppTab.TODAY) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.TODAY) Icons.Filled.Today else Icons.Outlined.Today,
                            contentDescription = "Today",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_today")
                )

                // Tasks Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.TASKS,
                    onClick = { viewModel.selectTab(AppTab.TASKS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.TASKS) Icons.Filled.TaskAlt else Icons.Outlined.TaskAlt,
                            contentDescription = "Tasks",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Tasks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_tasks")
                )

                // Projects Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.PROJECTS,
                    onClick = { viewModel.selectTab(AppTab.PROJECTS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.PROJECTS) Icons.Filled.Folder else Icons.Outlined.Folder,
                            contentDescription = "Projects",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Projects", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_projects")
                )

                // Courses Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.COURSES,
                    onClick = { viewModel.selectTab(AppTab.COURSES) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.COURSES) Icons.Filled.School else Icons.Outlined.School,
                            contentDescription = "Courses",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Courses", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_courses")
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (uiState.currentTab) {
                        AppTab.TODAY, AppTab.TASKS -> viewModel.openAddTaskDialog()
                        AppTab.PROJECTS -> viewModel.openAddProjectDialog()
                        AppTab.COURSES -> viewModel.openAddCourseDialog()
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = {
                    Text(
                        when (uiState.currentTab) {
                            AppTab.TODAY, AppTab.TASKS -> "New Task"
                            AppTab.PROJECTS -> "New Project"
                            AppTab.COURSES -> "New Course"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("fab_add_action")
            )
        },
        snackbarHost = {
            TaskCompletionToast(
                task = uiState.recentCompletedTask,
                onUndoClick = { viewModel.undoRecentTaskCompletion() }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_transition",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.TODAY -> TodayDashboardScreen(
                    uiState = uiState,
                    onAddTaskClick = { viewModel.openAddTaskDialog(priority = 1) },
                    onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
                    onTaskClick = { viewModel.openTaskDetail(it) }
                )

                AppTab.TASKS -> TasksScreen(
                    uiState = uiState,
                    onFilterChange = { viewModel.setTaskFilter(it) },
                    onSearchQueryChange = { viewModel.setTaskSearchQuery(it) },
                    onAddTaskClick = { viewModel.openAddTaskDialog() },
                    onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
                    onTaskClick = { viewModel.openTaskDetail(it) }
                )

                AppTab.PROJECTS -> ProjectsScreen(
                    uiState = uiState,
                    onAddProjectClick = { viewModel.openAddProjectDialog() },
                    onAdjustProjectProgress = { p, d -> viewModel.adjustProjectProgress(p, d) },
                    onProjectClick = { viewModel.openProjectDetail(it) },
                    onToggleProjectCompletion = { viewModel.toggleProjectCompletion(it) }
                )

                AppTab.COURSES -> CoursesScreen(
                    uiState = uiState,
                    onAddCourseClick = { viewModel.openAddCourseDialog() },
                    onAdjustCourseProgress = { c, d -> viewModel.adjustCourseProgress(c, d) },
                    onAdjustCourseModules = { c, d -> viewModel.adjustCourseModules(c, d) },
                    onCourseClick = { viewModel.openCourseDetail(it) },
                    onToggleCourseCompletion = { viewModel.toggleCourseCompletion(it) }
                )
            }
        }
    }

    // Detail Dialogs (Clicked items)
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val taskDetail = uiState.allTasks.find { it.id == selectedTaskId }
    taskDetail?.let { task ->
        TaskDetailDialog(
            task = task,
            onDismiss = { viewModel.closeTaskDetail() },
            onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
            onToggleSubtask = { t, id -> viewModel.toggleSubtask(t, id) },
            onLogTimeClick = {
                viewModel.closeTaskDetail()
                viewModel.openLogTimeDialog(it)
            },
            onEditClick = {
                viewModel.closeTaskDetail()
                viewModel.openEditTaskDialog(it)
            },
            onDeleteClick = {
                viewModel.closeTaskDetail()
                viewModel.deleteTask(it)
            },
            onAddSubtask = { t, title -> viewModel.addSubtask(t, title) }
        )
    }

    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val projectDetail = uiState.allProjects.find { it.id == selectedProjectId }
    projectDetail?.let { project ->
        ProjectDetailDialog(
            project = project,
            onDismiss = { viewModel.closeProjectDetail() },
            onToggleCompletion = { viewModel.toggleProjectCompletion(it) },
            onAdjustProgress = { p, delta -> viewModel.adjustProjectProgress(p, delta) },
            onSetProgress = { p, newPct -> viewModel.setProjectProgress(p, newPct) },
            onEditClick = {
                viewModel.closeProjectDetail()
                viewModel.openEditProjectDialog(it)
            },
            onDeleteClick = {
                viewModel.closeProjectDetail()
                viewModel.deleteProject(it)
            }
        )
    }

    val selectedCourseId by viewModel.selectedCourseId.collectAsState()
    val courseDetail = uiState.allCourses.find { it.id == selectedCourseId }
    courseDetail?.let { course ->
        CourseDetailDialog(
            course = course,
            onDismiss = { viewModel.closeCourseDetail() },
            onToggleCompletion = { viewModel.toggleCourseCompletion(it) },
            onAdjustProgress = { c, delta -> viewModel.adjustCourseProgress(c, delta) },
            onAdjustModules = { c, delta -> viewModel.adjustCourseModules(c, delta) },
            onSetProgress = { c, newPct -> viewModel.setCourseProgress(c, newPct) },
            onEditClick = {
                viewModel.closeCourseDetail()
                viewModel.openEditCourseDialog(it)
            },
            onDeleteClick = {
                viewModel.closeCourseDetail()
                viewModel.deleteCourse(it)
            }
        )
    }

    // Add / Edit Task Dialog
    val isTaskDialogOpen by viewModel.isTaskDialogOpen.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val newTaskDefaultPriority by viewModel.newTaskDefaultPriority.collectAsState()
    val newTaskDefaultDateIso by viewModel.newTaskDefaultDateIso.collectAsState()
    if (isTaskDialogOpen) {
        AddEditTaskDialog(
            initialTask = editingTask,
            defaultPriority = newTaskDefaultPriority,
            defaultScheduledDate = newTaskDefaultDateIso,
            onDismiss = { viewModel.closeTaskDialog() },
            onSave = { title, desc, time, dur, pri, isRec, repDays, subtasks, scheduledDate ->
                viewModel.saveTask(title, desc, time, dur, pri, isRec, repDays, subtasks, scheduledDate)
            }
        )
    }

    // Log Time Dialog
    val loggingTimeTask by viewModel.loggingTimeTask.collectAsState()
    loggingTimeTask?.let { task ->
        LogTimeDialog(
            task = task,
            onDismiss = { viewModel.closeLogTimeDialog() },
            onConfirm = { addedMins -> viewModel.logTimeForTask(task, addedMins) }
        )
    }

    // Add / Edit Project Dialog
    val isProjectDialogOpen by viewModel.isProjectDialogOpen.collectAsState()
    val editingProject by viewModel.editingProject.collectAsState()
    if (isProjectDialogOpen) {
        AddEditProjectDialog(
            initialProject = editingProject,
            onDismiss = { viewModel.closeProjectDialog() },
            onSave = { title, desc, priority, progress ->
                viewModel.saveProject(title, desc, priority, progress)
            }
        )
    }

    // Add / Edit Course Dialog
    val isCourseDialogOpen by viewModel.isCourseDialogOpen.collectAsState()
    val editingCourse by viewModel.editingCourse.collectAsState()
    if (isCourseDialogOpen) {
        AddEditCourseDialog(
            initialCourse = editingCourse,
            onDismiss = { viewModel.closeCourseDialog() },
            onSave = { title, desc, platform, modules, progress ->
                viewModel.saveCourse(title, desc, platform, modules, progress)
            }
        )
    }

    // Add Workspace Dialog
    if (uiState.isAddWorkspaceDialogOpen) {
        AddWorkspaceDialog(
            onDismiss = { viewModel.closeAddWorkspaceDialog() },
            onConfirm = { name, colorHex, iconName ->
                viewModel.createWorkspace(name, colorHex, iconName)
            }
        )
    }

    // Backup & Restore Dialog
    if (uiState.isBackupDialogOpen) {
        BackupDialog(
            allWorkspaces = uiState.allWorkspaces,
            allTasks = uiState.allTasks,
            allProjects = uiState.allProjects,
            allCourses = uiState.allCourses,
            pendingRestore = uiState.pendingRestoreBackup,
            operationMessage = uiState.backupOperationMessage,
            onDismiss = { viewModel.closeBackupDialog() },
            onExportClick = {
                exportBackupLauncher.launch("taskflow_backup_${DateUtils.getTodayIso()}.json")
            },
            onImportClick = {
                importBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
            },
            onConfirmRestore = { mode ->
                viewModel.confirmRestore(mode)
            },
            onCancelRestore = {
                viewModel.cancelRestorePreview()
            }
        )
    }
}
