package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AutomationTask
import com.example.data.VideoProject
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.VideoStatus
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun DashboardScreen(
    projects: List<VideoProject>,
    tasks: List<AutomationTask>,
    lang: AppLanguage,
    onStartNewVideo: () -> Unit,
    onEditProject: (VideoProject) -> Unit,
    onProduceProject: (Long) -> Unit,
    onPublishProject: (Long) -> Unit,
    onDeleteProject: (VideoProject) -> Unit,
    onOpenAutomation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val inProgressCount = projects.count { it.status == VideoStatus.IN_PROGRESS.name }
    val completedCount = projects.count { it.status == VideoStatus.COMPLETED.name }
    val reviewCount = projects.count { it.status == VideoStatus.REVIEW_PENDING.name }
    val publishedCount = projects.count { it.status == VideoStatus.PUBLISHED.name }

    val filteredProjects = when (selectedFilter) {
        "SHORTS" -> projects.filter { it.format == "SHORTS" }
        "LONG" -> projects.filter { it.format == "LONG" }
        "COMPLETED" -> projects.filter { it.status == VideoStatus.COMPLETED.name || it.status == VideoStatus.PUBLISHED.name }
        else -> projects
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero Banner / Callout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F1E36), Color(0xFF16102E), Color(0xFF0D172A))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), ElectricViolet.copy(alpha = 0.6f))),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
                    .testTag("hero_dashboard_banner")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.get("dash_title", lang),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStrings.get("dash_welcome", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStartNewVideo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_new_video_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF001F24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("dash_new_video", lang),
                            color = Color(0xFF001F24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stat Cards Grid (2x2)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = AppStrings.get("dash_in_progress", lang),
                    value = inProgressCount.toString(),
                    icon = Icons.Default.HourglassTop,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_in_progress"
                )
                StatCard(
                    title = AppStrings.get("dash_completed", lang),
                    value = completedCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    testTag = "stat_completed"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = AppStrings.get("dash_pending_review", lang),
                    value = reviewCount.toString(),
                    icon = Icons.Default.TrendingUp,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    testTag = "stat_review"
                )
                StatCard(
                    title = AppStrings.get("dash_published", lang),
                    value = publishedCount.toString(),
                    icon = Icons.Default.Publish,
                    accentColor = ElectricViolet,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_published"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("dash_recent_projects", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 17.sp
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf(
                        "ALL" to AppStrings.get("dash_filter_all", lang),
                        "SHORTS" to "Shorts",
                        "LONG" to "16:9",
                        "COMPLETED" to AppStrings.get("dash_completed", lang)
                    )
                    items(filters) { (key, title) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan else SurfaceNavyCard)
                                .border(1.dp, if (isSelected) NeonCyan else BorderNavy, RoundedCornerShape(10.dp))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("filter_$key")
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color(0xFF001F24) else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Project Cards
        if (filteredProjects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .background(SurfaceNavyCard, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderNavy, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("dash_no_projects", lang),
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { project ->
                DashboardProjectCard(
                    project = project,
                    lang = lang,
                    onEdit = { onEditProject(project) },
                    onProduce = { onProduceProject(project.id) },
                    onPublish = { onPublishProject(project.id) },
                    onDelete = { onDeleteProject(project) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Recent System Activity Log
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("dash_recent_activities", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 16.sp
                )
                Text(
                    text = AppStrings.get("auto_title", lang),
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onOpenAutomation() }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(tasks.take(3), key = { it.id }) { task ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceNavyCard, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderNavy, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.logMessage,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (task.status == "COMPLETED") Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFF00E5FF).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.scheduledTimeStr,
                            color = if (task.status == "COMPLETED") Color(0xFF10B981) else NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DashboardProjectCard(
    project: VideoProject,
    lang: AppLanguage,
    onEdit: () -> Unit,
    onProduce: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Format, Language, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E283E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E3D5C), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (project.format == "SHORTS") "📱 Shorts (9:16)" else "🖥️ 16:9",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E283E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (project.language == "fa") "فارسی" else "EN",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                StatusBadge(statusStr = project.status, lang = lang)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Topic
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = project.topic,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (project.renderProgress in 1..99) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { project.renderProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = NeonCyan,
                    trackColor = Color(0xFF1E283E)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${project.targetDurationSec}s",
                    color = TextDark,
                    fontSize = 11.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = NeonCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onProduce,
                        modifier = Modifier.size(32.dp).testTag("produce_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = "Produce & Preview",
                            tint = ElectricViolet,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onPublish,
                        modifier = Modifier.size(32.dp).testTag("publish_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Publish,
                            contentDescription = "Publish",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
