package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CourseEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.Subtask
import com.example.data.model.TaskEntity
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.Priority1Color
import com.example.ui.theme.Priority2Color
import com.example.ui.theme.Priority3Color
import com.example.ui.theme.Priority4Color
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import java.util.Locale
import java.util.UUID

private val AccentPurple = Color(0xFF9333EA)
private val AccentPurpleLight = Color(0xFFA855F7)

internal fun parseInitialTime12h(raw: String?): Triple<Int, Int, Boolean> {
    if (raw.isNullOrBlank()) return Triple(9, 0, true) // 09:00 AM default
    val parsed24 = NotificationHelper.parseHourAndMinute(raw) ?: return Triple(9, 0, true)
    val hour24 = parsed24.first
    val minute = parsed24.second
    val isAm = hour24 < 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return Triple(hour12, minute, isAm)
}

internal fun formatTime12h(hour12: Int, minute: Int, isAm: Boolean): String {
    val safeHour = hour12.coerceIn(1, 12)
    val safeMin = minute.coerceIn(0, 59)
    val suffix = if (isAm) "AM" else "PM"
    return String.format(Locale.US, "%02d:%02d %s", safeHour, safeMin, suffix)
}

@Composable
fun TaskCompletionToast(
    task: TaskEntity?,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = task != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("task_completion_toast")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = BrandEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Completed \"${task?.title ?: "Task"}\"",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = onUndoClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFBBF24)
                    ),
                    modifier = Modifier.testTag("toast_undo_btn")
                ) {
                    Text(
                        text = "UNDO",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    initialTask: TaskEntity?,
    defaultPriority: Int = 2,
    defaultScheduledDate: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        time: String?,
        durationMins: Int,
        priority: Int,
        isRecurring: Boolean,
        repeatDays: List<Int>,
        subtasks: List<Subtask>,
        scheduledDate: String?
    ) -> Unit
) {
    val todayIso = remember { DateUtils.getTodayIso() }
    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var description by remember(initialTask) { mutableStateOf(initialTask?.description ?: "") }

    // Scheduled Date state for non-repeating tasks (defaults to today / task creation day)
    var scheduledDateIso by remember(initialTask, defaultScheduledDate) {
        mutableStateOf(
            initialTask?.scheduledDate?.takeIf { it.isNotBlank() }
                ?: defaultScheduledDate?.takeIf { it.isNotBlank() }
                ?: (if (initialTask != null) DateUtils.formatIsoFromMillis(initialTask.createdAt) else todayIso)
        )
    }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Reminder AM/PM states
    var hasReminder by remember(initialTask) { mutableStateOf(!initialTask?.time.isNullOrBlank()) }
    val initialTimeParsed = remember(initialTask) { parseInitialTime12h(initialTask?.time) }
    var reminderHour12 by remember(initialTask) { mutableIntStateOf(initialTimeParsed.first) }
    var reminderMinute by remember(initialTask) { mutableIntStateOf(initialTimeParsed.second) }
    var reminderIsAm by remember(initialTask) { mutableStateOf(initialTimeParsed.third) }
    var hourInputText by remember(initialTask) {
        mutableStateOf(String.format(Locale.US, "%02d", initialTimeParsed.first))
    }
    var minuteInputText by remember(initialTask) {
        mutableStateOf(String.format(Locale.US, "%02d", initialTimeParsed.second))
    }
    var showClockDialPicker by remember { mutableStateOf(false) }

    fun updateReminderTime(newHour12: Int, newMinute: Int, newIsAm: Boolean) {
        val h = newHour12.coerceIn(1, 12)
        val m = newMinute.coerceIn(0, 59)
        reminderHour12 = h
        reminderMinute = m
        reminderIsAm = newIsAm
        hourInputText = String.format(Locale.US, "%02d", h)
        minuteInputText = String.format(Locale.US, "%02d", m)
    }

    val formattedReminderTime = remember(reminderHour12, reminderMinute, reminderIsAm) {
        formatTime12h(reminderHour12, reminderMinute, reminderIsAm)
    }

    // Flexible Task Duration states: minimum 1 minute (1m), maximum 12 hours (720m)
    val initialDuration = (initialTask?.durationMins ?: 30).coerceIn(1, 720)
    var durationHours by remember(initialTask) { mutableIntStateOf(initialDuration / 60) }
    var durationMinutes by remember(initialTask) { mutableIntStateOf(initialDuration % 60) }
    var hoursText by remember(initialTask) { mutableStateOf((initialDuration / 60).toString()) }
    var minsText by remember(initialTask) { mutableStateOf((initialDuration % 60).toString()) }

    fun applyDurationMinutes(totalMins: Int) {
        val clamped = totalMins.coerceIn(1, 720)
        val h = clamped / 60
        val m = clamped % 60
        durationHours = h
        durationMinutes = m
        hoursText = h.toString()
        minsText = m.toString()
    }

    val durationMins = remember(durationHours, durationMinutes) {
        (durationHours * 60 + durationMinutes).coerceIn(1, 720)
    }

    var priority by remember(initialTask, defaultPriority) {
        mutableIntStateOf(initialTask?.priority ?: defaultPriority)
    }
    var isRecurring by remember(initialTask) { mutableStateOf(initialTask?.isRecurring ?: false) }
    val repeatDays = remember(initialTask) {
        mutableStateListOf<Int>().apply {
            addAll(initialTask?.repeatDays ?: listOf(1, 2, 3, 4, 5, 6, 7))
        }
    }
    val subtasks = remember(initialTask) {
        mutableStateListOf<Subtask>().apply {
            if (initialTask != null) addAll(initialTask.subtasks)
        }
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("add_edit_task_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialTask == null) "New Task" else "Edit Task",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Task Title *
                Text(
                    text = "Task Title *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        focusedBorderColor = AccentPurpleLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = "Description",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Add details, notes, or links...", fontSize = 14.sp) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        focusedBorderColor = AccentPurpleLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Estimated Duration Header + Total Badge (1 min to 12 hours)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Duration",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Min 1m · Max 12h",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentPurple.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("task_duration_badge")
                    ) {
                        Text(
                            text = DateUtils.formatDurationMins(durationMins),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentPurpleLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hours (0..12) and Minutes (0..59) Stepper + Input Cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hours Card (0..12)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Hours (0–12)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val nextH = (durationHours - 1).coerceIn(0, 12)
                                        val nextM = if (nextH == 0 && durationMinutes == 0) 1 else durationMinutes
                                        applyDurationMinutes(nextH * 60 + nextM)
                                    }
                                    .testTag("duration_hours_minus")
                            ) {
                                Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            BasicTextField(
                                value = hoursText,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(2)
                                    hoursText = digits
                                    val parsedH = (digits.toIntOrNull() ?: 0).coerceIn(0, 12)
                                    durationHours = parsedH
                                    if (parsedH == 12) {
                                        durationMinutes = 0
                                        minsText = "0"
                                    } else if (parsedH == 0 && durationMinutes == 0) {
                                        durationMinutes = 1
                                        minsText = "1"
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(AccentPurpleLight),
                                modifier = Modifier
                                    .width(44.dp)
                                    .testTag("duration_hours_input")
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentPurple.copy(alpha = 0.2f))
                                    .border(
                                        1.dp,
                                        AccentPurpleLight.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val nextH = (durationHours + 1).coerceIn(0, 12)
                                        val nextM = if (nextH == 12) 0 else durationMinutes
                                        applyDurationMinutes(nextH * 60 + nextM)
                                    }
                                    .testTag("duration_hours_plus")
                            ) {
                                Text(
                                    text = "+",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AccentPurpleLight
                                )
                            }
                        }
                    }

                    // Minutes Card (0..59, or 1..59 when hours == 0)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Minutes (0–59)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        applyDurationMinutes(durationMins - 1)
                                    }
                                    .testTag("duration_mins_minus")
                            ) {
                                Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            BasicTextField(
                                value = minsText,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(2)
                                    minsText = digits
                                    val parsedM = (digits.toIntOrNull() ?: 0).coerceIn(0, 59)
                                    if (durationHours == 12) {
                                        durationMinutes = 0
                                        minsText = "0"
                                    } else if (durationHours == 0 && parsedM == 0 && digits.isNotEmpty()) {
                                        durationMinutes = 1
                                        minsText = "1"
                                    } else {
                                        durationMinutes = parsedM
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(AccentPurpleLight),
                                modifier = Modifier
                                    .width(44.dp)
                                    .testTag("duration_mins_input")
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentPurple.copy(alpha = 0.2f))
                                    .border(
                                        1.dp,
                                        AccentPurpleLight.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        applyDurationMinutes(durationMins + 1)
                                    }
                                    .testTag("duration_mins_plus")
                            ) {
                                Text(
                                    text = "+",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AccentPurpleLight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preset Duration Chips (1m, 5m, 15m, 30m, 45m, 1h, 2h, 4h, 8h, 12h)
                val durationPresets = listOf(1, 5, 15, 30, 45, 60, 120, 240, 480, 720)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 5,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    durationPresets.forEach { preset ->
                        val isSelected = durationMins == preset
                        val label = when {
                            preset >= 60 -> "${preset / 60}h"
                            else -> "${preset}m"
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) AccentPurple.copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentPurpleLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { applyDurationMinutes(preset) }
                                .padding(vertical = 7.dp)
                                .testTag("duration_preset_${label}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AccentPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reminder Notification Card with AM/PM Time Picker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = if (hasReminder) AccentPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reminder Notification",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasReminder) "Scheduled at $formattedReminderTime" else "Notify at scheduled time",
                                    fontSize = 11.sp,
                                    fontWeight = if (hasReminder) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (hasReminder) AccentPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPurple
                            ),
                            modifier = Modifier.testTag("reminder_toggle_switch")
                        )
                    }

                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Inline Hour : Minute + AM / PM Picker Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hour (1..12) Stepper Box
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "HOUR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .clickable {
                                                val prevH = if (reminderHour12 <= 1) 12 else reminderHour12 - 1
                                                updateReminderTime(prevH, reminderMinute, reminderIsAm)
                                            }
                                            .testTag("reminder_hour_minus")
                                    ) {
                                        Text("−", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    BasicTextField(
                                        value = hourInputText,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(2)
                                            hourInputText = digits
                                            val parsed = digits.toIntOrNull()
                                            if (parsed != null && parsed in 1..12) {
                                                reminderHour12 = parsed
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(AccentPurpleLight),
                                        modifier = Modifier
                                            .width(34.dp)
                                            .testTag("reminder_hour_input")
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(AccentPurple.copy(alpha = 0.2f))
                                            .clickable {
                                                val nextH = if (reminderHour12 >= 12) 1 else reminderHour12 + 1
                                                updateReminderTime(nextH, reminderMinute, reminderIsAm)
                                            }
                                            .testTag("reminder_hour_plus")
                                    ) {
                                        Text(
                                            text = "+",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AccentPurpleLight
                                        )
                                    }
                                }
                            }

                            Text(
                                text = ":",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Minute (00..59) Stepper Box
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "MIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .clickable {
                                                val prevM = if (reminderMinute <= 0) 59 else reminderMinute - 1
                                                updateReminderTime(reminderHour12, prevM, reminderIsAm)
                                            }
                                            .testTag("reminder_min_minus")
                                    ) {
                                        Text("−", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    BasicTextField(
                                        value = minuteInputText,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(2)
                                            minuteInputText = digits
                                            val parsed = digits.toIntOrNull()
                                            if (parsed != null && parsed in 0..59) {
                                                reminderMinute = parsed
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(AccentPurpleLight),
                                        modifier = Modifier
                                            .width(34.dp)
                                            .testTag("reminder_min_input")
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(AccentPurple.copy(alpha = 0.2f))
                                            .clickable {
                                                val nextM = if (reminderMinute >= 59) 0 else reminderMinute + 1
                                                updateReminderTime(reminderHour12, nextM, reminderIsAm)
                                            }
                                            .testTag("reminder_min_plus")
                                    ) {
                                        Text(
                                            text = "+",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AccentPurpleLight
                                        )
                                    }
                                }
                            }

                            // AM / PM Segmented Toggle Column
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (reminderIsAm) AccentPurple else Color.Transparent)
                                        .clickable { updateReminderTime(reminderHour12, reminderMinute, true) }
                                        .testTag("reminder_am_btn")
                                ) {
                                    Text(
                                        text = "AM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (reminderIsAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!reminderIsAm) AccentPurple else Color.Transparent)
                                        .clickable { updateReminderTime(reminderHour12, reminderMinute, false) }
                                        .testTag("reminder_pm_btn")
                                ) {
                                    Text(
                                        text = "PM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (!reminderIsAm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Time Presets + Clock Dial Picker Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val timePresets = listOf(
                                Triple(9, 0, true) to "9 AM",
                                Triple(12, 0, false) to "12 PM",
                                Triple(3, 0, false) to "3 PM",
                                Triple(6, 0, false) to "6 PM"
                            )
                            timePresets.forEach { (triple, label) ->
                                val isPresetSelected = reminderHour12 == triple.first &&
                                        reminderMinute == triple.second &&
                                        reminderIsAm == triple.third
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isPresetSelected) AccentPurple.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isPresetSelected) AccentPurpleLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            updateReminderTime(triple.first, triple.second, triple.third)
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPresetSelected) AccentPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Open Clock Dial Picker Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentPurple.copy(alpha = 0.18f))
                                    .border(
                                        1.dp,
                                        AccentPurpleLight.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showClockDialPicker = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("open_clock_dial_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Clock Dial",
                                        tint = AccentPurpleLight,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Dial",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AccentPurpleLight
                                    )
                                }
                            }
                        }
                    }
                }

                // Material 3 Clock Dial TimePicker Modal Dialog
                if (showClockDialPicker) {
                    val initial24Hour = remember(reminderHour12, reminderIsAm) {
                        when {
                            reminderIsAm && reminderHour12 == 12 -> 0
                            !reminderIsAm && reminderHour12 < 12 -> reminderHour12 + 12
                            else -> reminderHour12
                        }
                    }
                    val timePickerState = rememberTimePickerState(
                        initialHour = initial24Hour,
                        initialMinute = reminderMinute,
                        is24Hour = false
                    )

                    Dialog(onDismissRequest = { showClockDialPicker = false }) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Select Reminder Time",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                TimePicker(
                                    state = timePickerState,
                                    colors = TimePickerDefaults.colors(
                                        selectorColor = AccentPurple,
                                        periodSelectorSelectedContainerColor = AccentPurple,
                                        periodSelectorSelectedContentColor = Color.White,
                                        timeSelectorSelectedContainerColor = AccentPurple.copy(alpha = 0.25f),
                                        timeSelectorSelectedContentColor = AccentPurpleLight
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { showClockDialPicker = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            val h24 = timePickerState.hour
                                            val m = timePickerState.minute
                                            val isAmPicked = h24 < 12
                                            val h12Picked = when {
                                                h24 == 0 -> 12
                                                h24 > 12 -> h24 - 12
                                                else -> h24
                                            }
                                            updateReminderTime(h12Picked, m, isAmPicked)
                                            showClockDialPicker = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPurple,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Set Time", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority Selector (P1..P4)
                Text(
                    text = "Priority",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..4).forEach { p ->
                        val isSelected = priority == p
                        val pColor = when (p) {
                            1 -> Priority1Color
                            2 -> Priority2Color
                            3 -> Priority3Color
                            else -> Priority4Color
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) pColor.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) pColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { priority = p }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "P$p",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Repeat Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (isRecurring) AccentPurpleLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Repeat",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Recurring schedule",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPurple
                            ),
                            modifier = Modifier.testTag("repeat_toggle_switch")
                        )
                    }

                    if (isRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val dayLabels = listOf(
                            7 to "Sun", 1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat"
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            dayLabels.forEach { (dow, label) ->
                                val selected = repeatDays.contains(dow)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) AccentPurple
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable {
                                            if (selected && repeatDays.size > 1) repeatDays.remove(dow)
                                            else if (!selected) repeatDays.add(dow)
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Scheduled Date Card (ONLY shown for non-repeating tasks!)
                if (!isRecurring) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val tomorrowIso = remember(todayIso) { DateUtils.addDaysToIso(todayIso, 1) }
                    val nextWeekIso = remember(todayIso) { DateUtils.addDaysToIso(todayIso, 7) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("scheduled_date_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Scheduled Date",
                                    tint = BrandCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Scheduled Date",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${DateUtils.formatFriendly(scheduledDateIso)} ($scheduledDateIso)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandCyan
                                    )
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandCyan.copy(alpha = 0.18f))
                                    .border(
                                        1.dp,
                                        BrandCyan.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { showDatePickerDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("pick_scheduled_date_btn")
                            ) {
                                Text(
                                    text = "Pick Date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Date Chips: Today, Tomorrow, Next Week
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val datePresets = listOf(
                                todayIso to "Today",
                                tomorrowIso to "Tomorrow",
                                nextWeekIso to "Next Week"
                            )
                            datePresets.forEach { (presetIso, label) ->
                                val isSelected = scheduledDateIso == presetIso
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) BrandCyan.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { scheduledDateIso = presetIso }
                                        .padding(vertical = 7.dp)
                                        .testTag("date_preset_$label")
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Material 3 DatePickerDialog for non-repeating task date selection
                if (showDatePickerDialog) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = DateUtils.parseIsoToUtcMillis(scheduledDateIso)
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        scheduledDateIso = DateUtils.formatUtcMillisToIso(millis)
                                    }
                                    showDatePickerDialog = false
                                }
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Subtasks Section
                Text(
                    text = "Subtasks (${subtasks.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                subtasks.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = sub.isCompleted,
                                onCheckedChange = { checked ->
                                    val idx = subtasks.indexOfFirst { it.id == sub.id }
                                    if (idx >= 0) subtasks[idx] = sub.copy(isCompleted = checked)
                                }
                            )
                            Text(
                                text = sub.title,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { subtasks.removeAll { it.id == sub.id } }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Add subtask", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            focusedBorderColor = AccentPurpleLight
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                subtasks.add(
                                    Subtask(
                                        id = UUID.randomUUID().toString(),
                                        title = newSubtaskTitle.trim(),
                                        isCompleted = false
                                    )
                                )
                                newSubtaskTitle = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Subtask")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Cancel + Create/Save Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    if (hasReminder) formattedReminderTime else null,
                                    durationMins,
                                    priority,
                                    isRecurring,
                                    repeatDays.sorted(),
                                    subtasks.toList(),
                                    if (isRecurring) null else scheduledDateIso
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_task_btn")
                    ) {
                        Text(
                            text = if (initialTask == null) "Create" else "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogTimeDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val remaining = (task.durationMins - task.loggedMins).coerceAtLeast(1)
    var minsToAdd by remember(task) { mutableIntStateOf(remaining.coerceIn(1, 720)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "Log Focus Time",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current: ${DateUtils.formatDurationMins(task.loggedMins)} / ${DateUtils.formatDurationMins(task.durationMins)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time to Add",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { minsToAdd = (minsToAdd - 1).coerceIn(1, 720) }
                        ) {
                            Text("−", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "+${DateUtils.formatDurationMins(minsToAdd)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentPurpleLight
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(AccentPurple.copy(alpha = 0.2f))
                                .clickable { minsToAdd = (minsToAdd + 1).coerceIn(1, 720) }
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = AccentPurpleLight)
                        }
                    }
                }

                Slider(
                    value = minsToAdd.toFloat(),
                    onValueChange = { minsToAdd = it.toInt().coerceIn(1, 720) },
                    valueRange = 1f..720f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurpleLight,
                        activeTrackColor = AccentPurple
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(1, 5, 15, 25, 30, 60, 120).forEach { preset ->
                        FilterChip(
                            selected = minsToAdd == preset,
                            onClick = { minsToAdd = preset },
                            label = { Text("+${DateUtils.formatDurationMins(preset)}", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onConfirm(minsToAdd) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Log Time", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditProjectDialog(
    initialProject: ProjectEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, priority: Int, progressPct: Int) -> Unit
) {
    var title by remember(initialProject) { mutableStateOf(initialProject?.title ?: "") }
    var description by remember(initialProject) { mutableStateOf(initialProject?.description ?: "") }
    var priority by remember(initialProject) { mutableIntStateOf(initialProject?.priority ?: 1) }
    var progressPct by remember(initialProject) { mutableIntStateOf(initialProject?.progressPct ?: 0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialProject == null) "New Project" else "Edit Project",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Project Title *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Portfolio Redesign", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        focusedBorderColor = AccentPurpleLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Description",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Key milestones, deliverables, or notes...", fontSize = 14.sp) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        focusedBorderColor = AccentPurpleLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Priority",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..4).forEach { p ->
                        val isSelected = priority == p
                        val pColor = when (p) {
                            1 -> Priority1Color
                            2 -> Priority2Color
                            3 -> Priority3Color
                            else -> Priority4Color
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) pColor.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) pColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { priority = p }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "P$p",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) pColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Initial Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$progressPct%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentPurpleLight
                    )
                }

                Slider(
                    value = progressPct.toFloat(),
                    onValueChange = { progressPct = it.toInt().coerceIn(0, 100) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurpleLight,
                        activeTrackColor = AccentPurple
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title.trim(), description.trim(), priority, progressPct)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (initialProject == null) "Create" else "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDetailDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onToggleCompletion: (ProjectEntity) -> Unit,
    onAdjustProgress: (ProjectEntity, Int) -> Unit,
    onSetProgress: (ProjectEntity, Int) -> Unit,
    onEditClick: (ProjectEntity) -> Unit,
    onDeleteClick: (ProjectEntity) -> Unit
) {
    val priorityColor = when (project.priority) {
        1 -> Priority1Color
        2 -> Priority2Color
        3 -> Priority3Color
        else -> Priority4Color
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Check Circle, Title + Priority Badge, and Close Button
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onToggleCompletion(project) },
                            modifier = Modifier
                                .size(32.dp)
                                .padding(top = 2.dp)
                        ) {
                            if (project.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = BrandEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Mark Complete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (project.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (project.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(priorityColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Priority P${project.priority}",
                                    color = priorityColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { onDismiss() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Description Box
                if (project.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Milestone Progress Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Milestone Progress",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${project.progressPct}%",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (project.isCompleted) BrandEmerald else AccentPurpleLight
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = project.progressPct.toFloat(),
                            onValueChange = { onSetProgress(project, it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (project.isCompleted) BrandEmerald else AccentPurpleLight,
                                activeTrackColor = if (project.isCompleted) BrandEmerald else AccentPurple,
                                inactiveTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Fine Tune (±1%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onAdjustProgress(project, -1) }
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onAdjustProgress(project, 1) }
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mark Project as Completed Green Button
                Button(
                    onClick = { onToggleCompletion(project) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (project.isCompleted) MaterialTheme.colorScheme.surfaceVariant else BrandEmerald,
                        contentColor = if (project.isCompleted) MaterialTheme.colorScheme.onSurface else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (project.isCompleted) "Completed — Tap to Reopen" else "Mark Project as Completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Edit & Delete Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onEditClick(project) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { onDeleteClick(project) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Priority1Color.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Priority1Color.copy(alpha = 0.1f),
                            contentColor = Priority1Color
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCourseDialog(
    initialCourse: CourseEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, platformOrAuthor: String, totalModules: Int, progressPct: Int) -> Unit
) {
    var title by remember(initialCourse) { mutableStateOf(initialCourse?.title ?: "") }
    var description by remember(initialCourse) { mutableStateOf(initialCourse?.description ?: "") }
    var platform by remember(initialCourse) { mutableStateOf(initialCourse?.platformOrAuthor ?: "") }
    var totalModulesText by remember(initialCourse) {
        mutableStateOf((initialCourse?.totalModules ?: 12).toString())
    }
    var progressPct by remember(initialCourse) { mutableIntStateOf(initialCourse?.progressPct ?: 0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialCourse == null) "New Course" else "Edit Course",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Course Title *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Advanced TypeScript Patterns", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        focusedBorderColor = BrandCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = "Platform / Author",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = platform,
                            onValueChange = { platform = it },
                            placeholder = { Text("e.g. Coursera", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                focusedBorderColor = BrandCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Modules",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = totalModulesText,
                            onValueChange = { totalModulesText = it.filter { ch -> ch.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                focusedBorderColor = BrandCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Description / Notes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Topics covered, syllabus, or goals...", fontSize = 14.sp) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        focusedBorderColor = BrandCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Initial Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$progressPct%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandCyan
                    )
                }
                Slider(
                    value = progressPct.toFloat(),
                    onValueChange = { progressPct = it.toInt().coerceIn(0, 100) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandCyan,
                        activeTrackColor = BrandCyan
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val mods = (totalModulesText.toIntOrNull() ?: 10).coerceAtLeast(1)
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    platform.trim().ifBlank { "Online Course" },
                                    mods,
                                    progressPct
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (initialCourse == null) "Create" else "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseDetailDialog(
    course: CourseEntity,
    onDismiss: () -> Unit,
    onToggleCompletion: (CourseEntity) -> Unit,
    onAdjustProgress: (CourseEntity, Int) -> Unit,
    onAdjustModules: (CourseEntity, Int) -> Unit,
    onSetProgress: (CourseEntity, Int) -> Unit,
    onEditClick: (CourseEntity) -> Unit,
    onDeleteClick: (CourseEntity) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Check Circle, Course Title + Platform Badge, and Close Button
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onToggleCompletion(course) },
                            modifier = Modifier
                                .size(32.dp)
                                .padding(top = 2.dp)
                        ) {
                            if (course.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = BrandEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Mark Complete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (course.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (course.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (course.platformOrAuthor.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BrandCyan.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = course.platformOrAuthor,
                                        color = Color(0xFF22D3EE),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { onDismiss() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Description Box
                if (course.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = course.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modules Completed & Fine Tune Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Modules Completed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${course.completedModules} / ${course.totalModules} · ${course.progressPct}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (course.isCompleted) BrandEmerald else Color(0xFF22D3EE)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = course.progressPct.toFloat(),
                            onValueChange = { onSetProgress(course, it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (course.isCompleted) BrandEmerald else Color(0xFF22D3EE),
                                activeTrackColor = if (course.isCompleted) BrandEmerald else BrandCyan,
                                inactiveTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // -1 Module and +1 Module Buttons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onAdjustModules(course, -1) }
                            ) {
                                Text(
                                    text = "−1 Module",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandCyan.copy(alpha = 0.18f))
                                    .border(
                                        1.dp,
                                        Color(0xFF22D3EE).copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onAdjustModules(course, 1) }
                            ) {
                                Text(
                                    text = "+1 Module",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF22D3EE)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fine Tune (±1%) Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Fine Tune (±1%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onAdjustProgress(course, -1) }
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onAdjustProgress(course, 1) }
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mark Course as Completed Green Button
                Button(
                    onClick = { onToggleCompletion(course) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (course.isCompleted) MaterialTheme.colorScheme.surfaceVariant else BrandEmerald,
                        contentColor = if (course.isCompleted) MaterialTheme.colorScheme.onSurface else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (course.isCompleted) "Completed — Tap to Reopen" else "Mark Course as Completed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Edit & Delete Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onEditClick(course) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { onDeleteClick(course) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Priority1Color.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Priority1Color.copy(alpha = 0.1f),
                            contentColor = Priority1Color
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
