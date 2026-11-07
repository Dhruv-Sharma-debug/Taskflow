package com.example.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.CalendarEventEntity
import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GoogleCalendarSyncResult(
    val hasPermission: Boolean,
    val calendarId: Long?,
    val calendarDisplayName: String = "TaskFlow",
    val pushedToGoogleCount: Int = 0,
    val pulledFromGoogleCount: Int = 0,
    val importedEvents: List<CalendarEventEntity> = emptyList(),
    val updatedTaskGoogleIds: Map<Long, Long> = emptyMap(),
    val updatedEventGoogleIds: Map<Long, Long> = emptyMap(),
    val message: String = ""
)

object GoogleCalendarSyncHelper {

    const val DEDICATED_CALENDAR_NAME = "TaskFlow"
    private const val DEDICATED_ACCOUNT_NAME = "taskflow.sync@google.com"
    private const val DEDICATED_calendarColor = 0xFF6366F1.toInt()

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    /**
     * Finds or creates the separate dedicated "TaskFlow" Google Calendar on the device's
     * CalendarProvider so all TaskFlow Tasks, Events, and Appointments show up in their own
     * dedicated calendar inside Google Calendar and sync two-way.
     */
    fun getOrCreateDedicatedTaskFlowCalendarId(context: Context): Long? {
        if (!hasCalendarPermissions(context)) return null
        val resolver = context.contentResolver

        try {
            // 1. Look for existing "TaskFlow" calendar
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
            )
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ? OR ${CalendarContract.Calendars.NAME} = ?",
                arrayOf(DEDICATED_CALENDAR_NAME, DEDICATED_CALENDAR_NAME),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }

            // 2. Detect existing Google account on the device if present so the dedicated calendar
            // attaches to the user's Google Calendar account, or fall back to LOCAL account type
            var googleAccountName: String? = null
            var googleAccountType: String? = null
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val accName = cursor.getString(2) ?: ""
                    val accType = cursor.getString(3) ?: ""
                    if (accType.contains("com.google", ignoreCase = true)) {
                        googleAccountName = accName
                        googleAccountType = accType
                        break
                    }
                }
            }

            val accountName = googleAccountName ?: DEDICATED_ACCOUNT_NAME
            val accountType = googleAccountType ?: CalendarContract.ACCOUNT_TYPE_LOCAL

            val values = ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                put(CalendarContract.Calendars.NAME, DEDICATED_CALENDAR_NAME)
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, DEDICATED_CALENDAR_NAME)
                put(CalendarContract.Calendars.CALENDAR_COLOR, DEDICATED_calendarColor)
                put(
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.CAL_ACCESS_OWNER
                )
                put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
                put(CalendarContract.Calendars.VISIBLE, 1)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            }

            val syncAdapterUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build()

            val insertedUri = resolver.insert(syncAdapterUri, values)
            return insertedUri?.let { ContentUris.parseId(it) }
        } catch (_: Exception) {
            return null
        }
    }

    fun computeStartAndEndMillis(
        dateIso: String,
        startTimeStr: String?,
        endTimeStr: String?,
        durationMins: Int = 60
    ): Pair<Long, Long> {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val baseDate = try {
            isoFormat.parse(dateIso) ?: Date()
        } catch (_: Exception) {
            Date()
        }

        val startCal = Calendar.getInstance().apply {
            time = baseDate
            val hm = NotificationHelper.parseHourAndMinute(startTimeStr ?: "09:00 AM") ?: Pair(9, 0)
            set(Calendar.HOUR_OF_DAY, hm.first)
            set(Calendar.MINUTE, hm.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            time = baseDate
            val endHm = endTimeStr?.let { NotificationHelper.parseHourAndMinute(it) }
            if (endHm != null) {
                set(Calendar.HOUR_OF_DAY, endHm.first)
                set(Calendar.MINUTE, endHm.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= startCal.timeInMillis) {
                    timeInMillis = startCal.timeInMillis + durationMins.coerceAtLeast(15) * 60_000L
                }
            } else {
                timeInMillis = startCal.timeInMillis + durationMins.coerceAtLeast(1) * 60_000L
            }
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun upsertTaskToGoogleCalendar(context: Context, task: TaskEntity): Long? {
        if (!hasCalendarPermissions(context)) return null
        val calendarId = getOrCreateDedicatedTaskFlowCalendarId(context) ?: return null
        val resolver = context.contentResolver

        return try {
            val dateIso = DateUtils.getEffectiveTaskDateIso(task)
            val (startMillis, endMillis) = computeStartAndEndMillis(
                dateIso = dateIso,
                startTimeStr = task.time ?: "09:00 AM",
                endTimeStr = null,
                durationMins = task.durationMins.coerceIn(1, 720)
            )

            val titlePrefix = if (task.isCompleted) "✓ [Task P${task.priority}] " else "[Task P${task.priority}] "
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "$titlePrefix${task.title}")
                put(
                    CalendarContract.Events.DESCRIPTION,
                    buildString {
                        if (task.description.isNotBlank()) appendLine(task.description)
                        append("TaskFlow Task ID: ${task.id} | Duration: ${DateUtils.formatDurationMins(task.durationMins)}")
                    }
                )
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val existingGoogleId = task.googleCalendarEventId
            if (existingGoogleId != null && existingGoogleId > 0L) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingGoogleId)
                val updatedRows = resolver.update(updateUri, values, null, null)
                if (updatedRows > 0) return existingGoogleId
            }

            val insertedUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            insertedUri?.let { ContentUris.parseId(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteFromGoogleCalendar(context: Context, googleEventId: Long?) {
        if (googleEventId == null || googleEventId <= 0L) return
        if (!hasCalendarPermissions(context)) return
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, googleEventId)
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
        }
    }

    fun upsertCalendarEventToGoogleCalendar(context: Context, event: CalendarEventEntity): Long? {
        if (!hasCalendarPermissions(context)) return null
        val calendarId = getOrCreateDedicatedTaskFlowCalendarId(context) ?: return null
        val resolver = context.contentResolver

        return try {
            val (startMillis, endMillis) = computeStartAndEndMillis(
                dateIso = event.dateIso,
                startTimeStr = event.startTime,
                endTimeStr = event.endTime,
                durationMins = 60
            )

            val prefix = if (event.type == "APPOINTMENT") "[Appointment] " else ""
            val cleanTitle = if (event.title.startsWith("[Appointment]", ignoreCase = true)) {
                event.title
            } else {
                "$prefix${event.title}"
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, cleanTitle)
                put(CalendarContract.Events.DESCRIPTION, event.notes)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val existingGoogleId = event.googleCalendarEventId
            if (existingGoogleId != null && existingGoogleId > 0L) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingGoogleId)
                val updatedRows = resolver.update(updateUri, values, null, null)
                if (updatedRows > 0) return existingGoogleId
            }

            val insertedUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            insertedUri?.let { ContentUris.parseId(it) }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Performs a full two-way sync between TaskFlow and the dedicated "TaskFlow" Google Calendar
     * (plus pulls appointments from the user's Google Calendar).
     */
    fun performTwoWaySync(
        context: Context,
        localTasks: List<TaskEntity>,
        localEvents: List<CalendarEventEntity>,
        activeWorkspaceId: String
    ): GoogleCalendarSyncResult {
        if (!hasCalendarPermissions(context)) {
            return GoogleCalendarSyncResult(
                hasPermission = false,
                calendarId = null,
                message = "Calendar permission required to sync with Google Calendar."
            )
        }

        val taskFlowCalId = getOrCreateDedicatedTaskFlowCalendarId(context)
            ?: return GoogleCalendarSyncResult(
                hasPermission = true,
                calendarId = null,
                message = "Could not access dedicated TaskFlow calendar."
            )

        var pushedCount = 0
        val updatedTaskIds = mutableMapOf<Long, Long>()
        val updatedEventIds = mutableMapOf<Long, Long>()

        // 1. Push any local Tasks that haven't been synced or need updating
        for (task in localTasks) {
            val gId = upsertTaskToGoogleCalendar(context, task)
            if (gId != null && gId != task.googleCalendarEventId) {
                updatedTaskIds[task.id] = gId
                pushedCount++
            }
        }

        // 2. Push any local Events & Appointments to the dedicated TaskFlow Google Calendar
        for (ev in localEvents) {
            val gId = upsertCalendarEventToGoogleCalendar(context, ev)
            if (gId != null && gId != ev.googleCalendarEventId) {
                updatedEventIds[ev.id] = gId
                pushedCount++
            }
        }

        // Known Google Event IDs after push
        val knownGoogleIds = mutableSetOf<Long>()
        localTasks.mapNotNull { it.googleCalendarEventId }.forEach { knownGoogleIds.add(it) }
        localEvents.mapNotNull { it.googleCalendarEventId }.forEach { knownGoogleIds.add(it) }
        knownGoogleIds.addAll(updatedTaskIds.values)
        knownGoogleIds.addAll(updatedEventIds.values)

        // 3. Pull events created directly in the "TaskFlow" Google Calendar or appointments from Google Calendar
        val importedEvents = mutableListOf<CalendarEventEntity>()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        val now = System.currentTimeMillis()
        val windowStart = now - 30L * 24L * 3600_000L // 30 days back
        val windowEnd = now + 90L * 24L * 3600_000L   // 90 days ahead

        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )
            val selection = "(${CalendarContract.Events.CALENDAR_ID} = ? OR ${CalendarContract.Events.TITLE} LIKE ?) AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} = 0"
            val selectionArgs = arrayOf(
                taskFlowCalId.toString(),
                "%Appointment%",
                windowStart.toString(),
                windowEnd.toString()
            )

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val gEventId = cursor.getLong(0)
                    val rawTitle = cursor.getString(2)?.trim() ?: continue
                    val rawDesc = cursor.getString(3)?.trim() ?: ""
                    val dtStart = cursor.getLong(4)
                    val dtEnd = cursor.getLong(5).let { if (it > dtStart) it else dtStart + 3600_000L }

                    // Skip if this is already tracked or is a TaskFlow Task entry
                    if (knownGoogleIds.contains(gEventId)) continue
                    if (rawDesc.contains("TaskFlow Task ID:")) continue
                    if (rawTitle.startsWith("[Task P") || rawTitle.startsWith("✓ [Task P")) continue

                    val isAppointment = rawTitle.contains("Appointment", ignoreCase = true) ||
                            rawDesc.contains("Appointment", ignoreCase = true)
                    val cleanTitle = rawTitle
                        .removePrefix("[Appointment]")
                        .removePrefix("[Event]")
                        .trim()
                        .ifBlank { rawTitle }

                    val dateIso = isoFormat.format(Date(dtStart))
                    val startTimeStr = timeFormat.format(Date(dtStart))
                    val endTimeStr = timeFormat.format(Date(dtEnd))

                    importedEvents.add(
                        CalendarEventEntity(
                            title = cleanTitle,
                            notes = rawDesc,
                            dateIso = dateIso,
                            startTime = startTimeStr,
                            endTime = endTimeStr,
                            type = if (isAppointment) "APPOINTMENT" else "EVENT",
                            isCompleted = false,
                            workspaceId = activeWorkspaceId,
                            googleCalendarEventId = gEventId
                        )
                    )
                    knownGoogleIds.add(gEventId)
                }
            }
        } catch (_: Exception) {
        }

        val pulledCount = importedEvents.size
        val summaryMsg = when {
            pushedCount > 0 && pulledCount > 0 ->
                "Synced with '$DEDICATED_CALENDAR_NAME' Google Calendar ($pushedCount pushed, $pulledCount imported)"
            pushedCount > 0 ->
                "Synced $pushedCount item(s) to '$DEDICATED_CALENDAR_NAME' Google Calendar"
            pulledCount > 0 ->
                "Imported $pulledCount item(s) from '$DEDICATED_CALENDAR_NAME' Google Calendar"
            else ->
                "Up to date with '$DEDICATED_CALENDAR_NAME' Google Calendar"
        }

        return GoogleCalendarSyncResult(
            hasPermission = true,
            calendarId = taskFlowCalId,
            calendarDisplayName = DEDICATED_CALENDAR_NAME,
            pushedToGoogleCount = pushedCount,
            pulledFromGoogleCount = pulledCount,
            importedEvents = importedEvents,
            updatedTaskGoogleIds = updatedTaskIds,
            updatedEventGoogleIds = updatedEventIds,
            message = summaryMsg
        )
    }
}
