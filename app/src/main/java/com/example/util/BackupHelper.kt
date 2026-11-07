package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.CalendarEventEntity
import com.example.data.model.CourseEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.Subtask
import com.example.data.model.TaskEntity
import com.example.data.model.WorkspaceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupData(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appName: String = "TaskFlow",
    val workspaces: List<WorkspaceEntity>,
    val tasks: List<TaskEntity>,
    val projects: List<ProjectEntity>,
    val courses: List<CourseEntity>,
    val dailyLogs: List<DailyLogEntity>,
    val calendarEvents: List<CalendarEventEntity> = emptyList()
)

data class BackupSummary(
    val workspacesCount: Int,
    val tasksCount: Int,
    val projectsCount: Int,
    val coursesCount: Int,
    val exportedAt: Long? = null
)

enum class RestoreMode {
    MERGE,
    REPLACE
}

object BackupHelper {

    fun serializeToJson(data: BackupData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("exportedAt", data.exportedAt)
        root.put("appName", data.appName)

        // Workspaces
        val workspacesArr = JSONArray()
        for (w in data.workspaces) {
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("colorHex", w.colorHex)
            obj.put("iconName", w.iconName)
            obj.put("isDefault", w.isDefault)
            obj.put("createdAt", w.createdAt)
            workspacesArr.put(obj)
        }
        root.put("workspaces", workspacesArr)

        // Tasks
        val tasksArr = JSONArray()
        for (t in data.tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("description", t.description)
            obj.put("isRecurring", t.isRecurring)
            obj.put("repeatDays", JSONArray(t.repeatDays))
            obj.put("scheduledDate", t.scheduledDate ?: JSONObject.NULL)
            obj.put("time", t.time ?: JSONObject.NULL)
            obj.put("reminderId", t.reminderId)
            obj.put("durationMins", t.durationMins)
            obj.put("loggedMins", t.loggedMins)
            obj.put("isCompleted", t.isCompleted)
            obj.put("priority", t.priority)
            obj.put("createdAt", t.createdAt)
            obj.put("completedAt", t.completedAt ?: JSONObject.NULL)
            obj.put("lastCompletedDate", t.lastCompletedDate ?: JSONObject.NULL)
            obj.put("preCompletionLoggedMins", t.preCompletionLoggedMins)
            obj.put("workspaceId", t.workspaceId)

            val subtasksArr = JSONArray()
            for (sub in t.subtasks) {
                val subObj = JSONObject()
                subObj.put("id", sub.id)
                subObj.put("title", sub.title)
                subObj.put("isCompleted", sub.isCompleted)
                subtasksArr.put(subObj)
            }
            obj.put("subtasks", subtasksArr)
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        // Projects
        val projectsArr = JSONArray()
        for (p in data.projects) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("description", p.description)
            obj.put("priority", p.priority)
            obj.put("progressPct", p.progressPct)
            obj.put("isCompleted", p.isCompleted)
            obj.put("createdAt", p.createdAt)
            obj.put("workspaceId", p.workspaceId)
            projectsArr.put(obj)
        }
        root.put("projects", projectsArr)

        // Courses
        val coursesArr = JSONArray()
        for (c in data.courses) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("title", c.title)
            obj.put("description", c.description)
            obj.put("platformOrAuthor", c.platformOrAuthor)
            obj.put("totalModules", c.totalModules)
            obj.put("completedModules", c.completedModules)
            obj.put("progressPct", c.progressPct)
            obj.put("isCompleted", c.isCompleted)
            obj.put("createdAt", c.createdAt)
            obj.put("workspaceId", c.workspaceId)
            coursesArr.put(obj)
        }
        root.put("courses", coursesArr)

        // Daily Logs
        val logsArr = JSONArray()
        for (l in data.dailyLogs) {
            val obj = JSONObject()
            obj.put("date", l.date)
            obj.put("workspaceId", l.workspaceId)
            obj.put("totalScheduledMins", l.totalScheduledMins)
            obj.put("totalLoggedMins", l.totalLoggedMins)
            obj.put("tasksCompletedCount", l.tasksCompletedCount)
            obj.put("totalTasksCount", l.totalTasksCount)
            logsArr.put(obj)
        }
        root.put("dailyLogs", logsArr)

        // Calendar Events & Appointments
        val eventsArr = JSONArray()
        for (ev in data.calendarEvents) {
            val obj = JSONObject()
            obj.put("id", ev.id)
            obj.put("title", ev.title)
            obj.put("notes", ev.notes)
            obj.put("dateIso", ev.dateIso)
            obj.put("startTime", ev.startTime)
            obj.put("endTime", ev.endTime)
            obj.put("type", ev.type)
            obj.put("isCompleted", ev.isCompleted)
            obj.put("workspaceId", ev.workspaceId)
            obj.put("createdAt", ev.createdAt)
            eventsArr.put(obj)
        }
        root.put("calendarEvents", eventsArr)

        return root.toString(2)
    }

    fun deserializeFromJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val appName = root.optString("appName", "TaskFlow")

        // Workspaces
        val workspaces = mutableListOf<WorkspaceEntity>()
        val workspacesArr = root.optJSONArray("workspaces")
        if (workspacesArr != null) {
            for (i in 0 until workspacesArr.length()) {
                val obj = workspacesArr.getJSONObject(i)
                workspaces.add(
                    WorkspaceEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        colorHex = obj.optString("colorHex", "#4F46E5"),
                        iconName = obj.optString("iconName", "folder"),
                        isDefault = obj.optBoolean("isDefault", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Tasks
        val tasks = mutableListOf<TaskEntity>()
        val tasksArr = root.optJSONArray("tasks")
        if (tasksArr != null) {
            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.getJSONObject(i)
                val repeatDaysList = mutableListOf<Int>()
                val repeatArr = obj.optJSONArray("repeatDays")
                if (repeatArr != null) {
                    for (r in 0 until repeatArr.length()) {
                        repeatDaysList.add(repeatArr.getInt(r))
                    }
                }

                val subtasksList = mutableListOf<Subtask>()
                val subArr = obj.optJSONArray("subtasks")
                if (subArr != null) {
                    for (s in 0 until subArr.length()) {
                        val subObj = subArr.getJSONObject(s)
                        subtasksList.add(
                            Subtask(
                                id = subObj.getString("id"),
                                title = subObj.getString("title"),
                                isCompleted = subObj.optBoolean("isCompleted", false)
                            )
                        )
                    }
                }

                tasks.add(
                    TaskEntity(
                        id = obj.optLong("id", 0),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        repeatDays = repeatDaysList,
                        scheduledDate = if (obj.isNull("scheduledDate")) null else obj.optString("scheduledDate").takeIf { it.isNotBlank() },
                        time = if (obj.isNull("time")) null else obj.optString("time"),
                        reminderId = obj.optInt("reminderId", 0),
                        durationMins = obj.optInt("durationMins", 30),
                        loggedMins = obj.optInt("loggedMins", 0),
                        subtasks = subtasksList,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        priority = obj.optInt("priority", 2),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        completedAt = if (obj.isNull("completedAt")) null else obj.optLong("completedAt"),
                        lastCompletedDate = if (obj.isNull("lastCompletedDate")) null else obj.optString("lastCompletedDate"),
                        preCompletionLoggedMins = obj.optInt("preCompletionLoggedMins", 0),
                        workspaceId = obj.optString("workspaceId", "personal")
                    )
                )
            }
        }

        // Projects
        val projects = mutableListOf<ProjectEntity>()
        val projectsArr = root.optJSONArray("projects")
        if (projectsArr != null) {
            for (i in 0 until projectsArr.length()) {
                val obj = projectsArr.getJSONObject(i)
                projects.add(
                    ProjectEntity(
                        id = obj.optLong("id", 0),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        priority = obj.optInt("priority", 1),
                        progressPct = obj.optInt("progressPct", 0),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        workspaceId = obj.optString("workspaceId", "personal")
                    )
                )
            }
        }

        // Courses
        val courses = mutableListOf<CourseEntity>()
        val coursesArr = root.optJSONArray("courses")
        if (coursesArr != null) {
            for (i in 0 until coursesArr.length()) {
                val obj = coursesArr.getJSONObject(i)
                courses.add(
                    CourseEntity(
                        id = obj.optLong("id", 0),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        platformOrAuthor = obj.optString("platformOrAuthor", "Online Course"),
                        totalModules = obj.optInt("totalModules", 10),
                        completedModules = obj.optInt("completedModules", 0),
                        progressPct = obj.optInt("progressPct", 0),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        workspaceId = obj.optString("workspaceId", "personal")
                    )
                )
            }
        }

        // Daily Logs
        val dailyLogs = mutableListOf<DailyLogEntity>()
        val logsArr = root.optJSONArray("dailyLogs")
        if (logsArr != null) {
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.getJSONObject(i)
                dailyLogs.add(
                    DailyLogEntity(
                        date = obj.getString("date"),
                        workspaceId = obj.optString("workspaceId", "personal"),
                        totalScheduledMins = obj.optInt("totalScheduledMins", 0),
                        totalLoggedMins = obj.optInt("totalLoggedMins", 0),
                        tasksCompletedCount = obj.optInt("tasksCompletedCount", 0),
                        totalTasksCount = obj.optInt("totalTasksCount", 0)
                    )
                )
            }
        }

        // Calendar Events
        val calendarEvents = mutableListOf<CalendarEventEntity>()
        val eventsArr = root.optJSONArray("calendarEvents")
        if (eventsArr != null) {
            for (i in 0 until eventsArr.length()) {
                val obj = eventsArr.getJSONObject(i)
                calendarEvents.add(
                    CalendarEventEntity(
                        id = obj.optLong("id", 0),
                        title = obj.getString("title"),
                        notes = obj.optString("notes", ""),
                        dateIso = obj.optString("dateIso", DateUtils.getTodayIso()),
                        startTime = obj.optString("startTime", "09:00 AM"),
                        endTime = obj.optString("endTime", "10:00 AM"),
                        type = obj.optString("type", "EVENT"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        workspaceId = obj.optString("workspaceId", "personal"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return BackupData(
            version = version,
            exportedAt = exportedAt,
            appName = appName,
            workspaces = workspaces,
            tasks = tasks,
            projects = projects,
            courses = courses,
            dailyLogs = dailyLogs,
            calendarEvents = calendarEvents
        )
    }

    fun getSummary(data: BackupData): BackupSummary {
        return BackupSummary(
            workspacesCount = data.workspaces.size,
            tasksCount = data.tasks.size,
            projectsCount = data.projects.size,
            coursesCount = data.courses.size,
            exportedAt = data.exportedAt
        )
    }

    fun exportToUri(context: Context, uri: Uri, data: BackupData): Result<Unit> {
        return try {
            val json = serializeToJson(data)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            } ?: return Result.failure(IllegalStateException("Unable to open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readFromUri(context: Context, uri: Uri): Result<BackupData> {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
            } ?: return Result.failure(IllegalStateException("Unable to open input stream"))

            val parsed = deserializeFromJson(sb.toString())
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
