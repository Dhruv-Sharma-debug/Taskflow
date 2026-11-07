package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEventEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.CalendarEventCard
import com.example.ui.components.TaskCard
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandIndigoDark
import com.example.ui.viewmodel.TaskFlowUiState
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: TaskFlowUiState,
    hasCalendarPermissions: Boolean,
    onSelectDate: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onAddEventClick: (String) -> Unit,
    onAddAppointmentClick: (String) -> Unit,
    onAddTaskForDateClick: (String) -> Unit,
    onEventClick: (CalendarEventEntity) -> Unit,
    onToggleEvent: (CalendarEventEntity) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onSyncGoogleCalendarClick: () -> Unit,
    onDismissSyncMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIso = remember { DateUtils.getTodayIso() }
    val selectedDateIso = uiState.selectedCalendarDateIso
    val weekDays = remember(selectedDateIso) {
        DateUtils.getWeekDaysAround(selectedDateIso)
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    val eventsForSelectedDate = remember(uiState.allCalendarEvents, selectedDateIso, uiState.calendarFilter) {
        uiState.allCalendarEvents.filter { event ->
            event.dateIso == selectedDateIso && when (uiState.calendarFilter) {
                "Events" -> event.type == "EVENT"
                "Appointments" -> event.type == "APPOINTMENT"
                "Tasks" -> false
                else -> true
            }
        }.sortedBy { it.startTime }
    }

    val tasksForSelectedDate = remember(uiState.allTasks, selectedDateIso, uiState.calendarFilter) {
        if (uiState.calendarFilter == "Events" || uiState.calendarFilter == "Appointments") {
            emptyList()
        } else {
            uiState.allTasks.filter { task ->
                DateUtils.isTaskScheduledForDate(task, selectedDateIso)
            }.sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenBy { it.priority }
                    .thenBy { it.time ?: "99:99" }
            )
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseIsoToUtcMillis(selectedDateIso)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onSelectDate(DateUtils.formatUtcMillisToIso(millis))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select Date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Pick Date button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Calendar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = DateUtils.formatMonthYearHeader(selectedDateIso),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedDateIso != todayIso) {
                        FilledTonalButton(
                            onClick = { onSelectDate(todayIso) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("calendar_today_jump_button")
                        ) {
                            Text("Today", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("calendar_pick_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Google Calendar 2-Way Sync Banner Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandIndigo.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, BrandIndigo.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandIndigo.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Google Calendar Sync",
                                    tint = BrandIndigoDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "TaskFlow Google Calendar Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasCalendarPermissions) {
                                        "Two-way sync with dedicated \"TaskFlow\" calendar & Google Appointments"
                                    } else {
                                        "Tap Sync Now to connect your device Google Calendar (\"TaskFlow\")"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onSyncGoogleCalendarClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("sync_google_calendar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sync Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = uiState.calendarSyncMessage != null) {
                        uiState.calendarSyncMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismissSyncMessage() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive 7-Day Week Strip
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSelectDate(DateUtils.addDaysToIso(selectedDateIso, -7)) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("calendar_prev_week_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Week"
                            )
                        }

                        Text(
                            text = DateUtils.formatFriendly(selectedDateIso),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { onSelectDate(DateUtils.addDaysToIso(selectedDateIso, 7)) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("calendar_next_week_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Week"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        weekDays.forEach { dayItem ->
                            val isSelected = dayItem.dateIso == selectedDateIso
                            val hasEvents = uiState.allCalendarEvents.any { it.dateIso == dayItem.dateIso }
                            val hasTasks = uiState.allTasks.any { DateUtils.isTaskScheduledForDate(it, dayItem.dateIso) }

                            Surface(
                                onClick = { onSelectDate(dayItem.dateIso) },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isSelected -> BrandIndigo
                                    dayItem.isToday -> BrandIndigo.copy(alpha = 0.14f)
                                    else -> Color.Transparent
                                },
                                border = if (dayItem.isToday && !isSelected) {
                                    BorderStroke(1.dp, BrandIndigo.copy(alpha = 0.5f))
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .testTag("calendar_day_${dayItem.dateIso}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = dayItem.shortDayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = dayItem.dayOfMonth,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.height(6.dp)
                                    ) {
                                        if (hasEvents) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else BrandCyan)
                                            )
                                        }
                                        if (hasTasks) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.85f) else BrandAmber)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Add Buttons Row (+ Event, + Appointment, + Scheduled Task)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onAddEventClick(selectedDateIso) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BrandIndigo.copy(alpha = 0.16f),
                        contentColor = BrandIndigoDark
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_add_event_button")
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "+ Event",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { onAddAppointmentClick(selectedDateIso) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BrandCyan.copy(alpha = 0.16f),
                        contentColor = BrandCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_add_appointment_button")
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "+ Appointment",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { onAddTaskForDateClick(selectedDateIso) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BrandEmerald.copy(alpha = 0.16f),
                        contentColor = BrandEmerald
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_add_task_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "+ Task",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Filter Chips
        item {
            val filters = listOf("All", "Events", "Appointments", "Tasks")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val selected = uiState.calendarFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterSelect(filter) },
                        label = { Text(filter, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandIndigo.copy(alpha = 0.22f),
                            selectedLabelColor = BrandIndigoDark
                        ),
                        modifier = Modifier.testTag("calendar_filter_${filter.lowercase()}")
                    )
                }
            }
        }

        // Events & Appointments Section
        if (eventsForSelectedDate.isNotEmpty()) {
            item {
                Text(
                    text = "Events & Appointments (${eventsForSelectedDate.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(eventsForSelectedDate, key = { "cal_event_${it.id}" }) { event ->
                CalendarEventCard(
                    event = event,
                    onToggleCompletion = { onToggleEvent(event) },
                    onEventClick = { onEventClick(event) }
                )
            }
        }

        // Scheduled Tasks Section
        if (tasksForSelectedDate.isNotEmpty()) {
            item {
                Text(
                    text = "Scheduled Tasks (${tasksForSelectedDate.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(tasksForSelectedDate, key = { "cal_task_${it.id}" }) { task ->
                TaskCard(
                    task = task,
                    onToggleCompletion = { onToggleTask(task) },
                    onTaskClick = { onTaskClick(task) }
                )
            }
        }

        // Empty State for Selected Day
        if (eventsForSelectedDate.isEmpty() && tasksForSelectedDate.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = BrandIndigoDark,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Nothing scheduled for ${DateUtils.formatFriendly(selectedDateIso)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add an Event, Appointment, or scheduled Task — it will automatically sync with your TaskFlow Google Calendar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
