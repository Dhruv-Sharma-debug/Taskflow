package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandIndigoDark
import com.example.ui.theme.Priority1Color
import com.example.ui.theme.Priority2Color
import com.example.ui.theme.Priority3Color
import com.example.ui.theme.Priority4Color
import com.example.ui.viewmodel.TaskFlowUiState

private val ProjectAccentIndigo = Color(0xFF6366F1)
private val ProjectAccentPurple = Color(0xFFA855F7)
private val ProjectActiveAmber = Color(0xFFF59E0B)

@Composable
fun ProjectsScreen(
    uiState: TaskFlowUiState,
    onAddProjectClick: () -> Unit,
    onAdjustProjectProgress: (ProjectEntity, Int) -> Unit,
    onProjectClick: (ProjectEntity) -> Unit,
    onToggleProjectCompletion: (ProjectEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeProjects = uiState.allProjects.filter { !it.isCompleted }
    val completedProjects = uiState.allProjects.filter { it.isCompleted }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("projects_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Milestones and progress tracking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Top 3 Summary Stat Cards: Total Projects, Active, Completed
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("projects_summary_row")
            ) {
                ProjectSummaryStatCard(
                    value = uiState.allProjects.size.toString(),
                    label = "Total Projects",
                    valueColor = ProjectAccentPurple,
                    modifier = Modifier.weight(1f)
                )
                ProjectSummaryStatCard(
                    value = activeProjects.size.toString(),
                    label = "Active",
                    valueColor = ProjectActiveAmber,
                    modifier = Modifier.weight(1f)
                )
                ProjectSummaryStatCard(
                    value = completedProjects.size.toString(),
                    label = "Completed",
                    valueColor = BrandEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (uiState.allProjects.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "No projects",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No projects created yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onAddProjectClick, shape = RoundedCornerShape(10.dp)) {
                            Text("+ Create Project", fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            if (activeProjects.isNotEmpty()) {
                item {
                    Text(
                        text = "Active (${activeProjects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(activeProjects, key = { "project_${it.id}" }) { project ->
                    ProjectListCard(
                        project = project,
                        onProjectClick = { onProjectClick(project) },
                        onAdjustProgress = { delta -> onAdjustProjectProgress(project, delta) }
                    )
                }
            }

            if (completedProjects.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed (${completedProjects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(completedProjects, key = { "project_${it.id}" }) { project ->
                    ProjectListCard(
                        project = project,
                        onProjectClick = { onProjectClick(project) },
                        onAdjustProgress = { delta -> onAdjustProjectProgress(project, delta) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProjectSummaryStatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectListCard(
    project: ProjectEntity,
    onProjectClick: () -> Unit,
    onAdjustProgress: (Int) -> Unit
) {
    val priorityColor = when (project.priority) {
        1 -> Priority1Color
        2 -> Priority2Color
        3 -> Priority3Color
        else -> Priority4Color
    }
    val barColor = if (project.isCompleted) BrandEmerald else ProjectAccentIndigo

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onProjectClick() }
            .testTag("project_card_${project.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Top Row: Priority-colored Folder Icon + Title + Priority Badge + Percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Priority-colored Folder Icon Box
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(priorityColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Project Priority P${project.priority}",
                            tint = priorityColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = project.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (project.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (project.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Priority Pill Badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "P${project.priority}",
                            color = priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "${project.progressPct}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Row: Progress Bar on left + Inline [-] and [+] buttons on right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { project.progressPct / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                // [-] Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onAdjustProgress(-1) }
                        .testTag("project_minus_btn_${project.id}")
                ) {
                    Text(
                        text = "−",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // [+] Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(BrandIndigoDark.copy(alpha = 0.2f))
                        .border(
                            width = 1.dp,
                            color = BrandIndigoDark.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onAdjustProgress(1) }
                        .testTag("project_plus_btn_${project.id}")
                ) {
                    Text(
                        text = "+",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )
                }
            }
        }
    }
}
