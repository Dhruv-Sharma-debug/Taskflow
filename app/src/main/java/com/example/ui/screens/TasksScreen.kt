package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.components.TaskCard
import com.example.ui.viewmodel.TaskFlowUiState
import com.example.util.DateUtils

@Composable
fun TasksScreen(
    uiState: TaskFlowUiState,
    onFilterChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddTaskClick: () -> Unit,
    onToggleTaskCompletion: (TaskEntity) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIso = DateUtils.getTodayIso()
    val filteredTasks = uiState.allTasks.filter { task ->
        val matchesQuery = if (uiState.taskSearchQuery.isBlank()) true
        else task.title.contains(uiState.taskSearchQuery, ignoreCase = true) ||
                task.description.contains(uiState.taskSearchQuery, ignoreCase = true)

        val matchesFilter = when (uiState.taskFilter) {
            "Today" -> !task.isCompleted && DateUtils.isTaskActiveForToday(task, todayIso)
            "Upcoming" -> DateUtils.isTaskUpcoming(task, todayIso)
            "Priority P1" -> task.priority == 1 && !task.isCompleted && DateUtils.isTaskActiveForToday(task, todayIso)
            "Completed" -> task.isCompleted
            "All" -> true
            else -> true
        }

        matchesQuery && matchesFilter
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("tasks_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage schedule, durations, and reminders",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.taskSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search tasks...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.taskSearchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Filter Chips Row: Today, Upcoming, Priority P1, Completed, All
        item {
            val filterOptions = listOf("Today", "Upcoming", "Priority P1", "Completed", "All")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = uiState.taskFilter == filter
                    val count = when (filter) {
                        "Today" -> uiState.allTasks.count {
                            !it.isCompleted && DateUtils.isTaskActiveForToday(it, todayIso)
                        }
                        "Upcoming" -> uiState.allTasks.count { DateUtils.isTaskUpcoming(it, todayIso) }
                        "Priority P1" -> uiState.allTasks.count { it.priority == 1 && !it.isCompleted && DateUtils.isTaskActiveForToday(it, todayIso) }
                        "Completed" -> uiState.allTasks.count { it.isCompleted }
                        else -> uiState.allTasks.size
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (uiState.taskFilter == "Upcoming") Icons.Default.DateRange else Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val emptyTitle = when {
                            uiState.taskSearchQuery.isNotBlank() -> "No matching tasks"
                            uiState.taskFilter == "Today" -> "No tasks scheduled for today"
                            uiState.taskFilter == "Upcoming" -> "No upcoming tasks for next week"
                            uiState.taskFilter == "Priority P1" -> "No active Priority 1 tasks"
                            uiState.taskFilter == "Completed" -> "No completed tasks yet"
                            else -> "No tasks created yet"
                        }
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onAddTaskClick, shape = RoundedCornerShape(8.dp)) {
                            Text("+ Create Task", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            items(filteredTasks, key = { "task_${it.id}" }) { task ->
                TaskCard(
                    task = task,
                    onToggleCompletion = onToggleTaskCompletion,
                    onTaskClick = onTaskClick
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
