package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AutomationTask
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun AutomationScreen(
    viewModel: MainViewModel,
    tasks: List<AutomationTask>,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val automationEnabled by viewModel.automationEnabled.collectAsState()
    val automationTime by viewModel.automationTime.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("automation_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Column {
                Text(
                    text = AppStrings.get("auto_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("auto_subtitle", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transparent Execution Status Callout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1B2F))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
                    .testTag("automation_notice_box")
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "زمان‌بندی فعلاً غیرفعال است؛ این صفحه هنوز وظیفهٔ واقعی پس‌زمینه ایجاد نمی‌کند و هیچ ویدئویی را خودکار تولید یا منتشر نمی‌کند."
                        else
                            "Scheduling is currently disabled. This screen does not create a real background job or automatically generate or publish videos.",
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automation Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Toggle switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.get("auto_enabled_label", lang),
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "هنوز پیاده‌سازی نشده" else "Not implemented",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = automationEnabled,
                            onCheckedChange = { viewModel.toggleAutomation(it) },
                            enabled = false,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF001F24),
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E283E)
                            ),
                            modifier = Modifier.testTag("automation_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Frequency
                    Text(
                        text = AppStrings.get("auto_frequency", lang),
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppStrings.get("auto_freq_daily", lang),
                            AppStrings.get("auto_freq_3days", lang),
                            AppStrings.get("auto_freq_weekly", lang)
                        ).forEachIndexed { idx, freqLabel ->
                            val isSel = idx == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) ElectricViolet else Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freqLabel.split(" ").first(),
                                    color = if (isSel) Color.White else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Publishing Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = AppStrings.get("auto_preferred_time", lang), color = TextWhite, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = automationTime, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Do not present a non-functional manual trigger as an available action.
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_automation_now_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF1E283E),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "اجرای خودکار هنوز فعال نیست" else "Automation is not available yet",
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Task History & Logs
            Text(
                text = AppStrings.get("auto_task_history", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceNavyCard, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderNavy, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = AppStrings.get("auto_no_tasks", lang), color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceNavyCard, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderNavy, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val statusColor = when (task.status) {
                                "COMPLETED" -> Color(0xFF10B981)
                                "PENDING_APPROVAL" -> Color(0xFFF59E0B)
                                else -> NeonCyan
                            }
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = task.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = task.logMessage,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Scheduled: ${task.scheduledTimeStr}", color = Color(0xFF64748B), fontSize = 10.sp)
                            Text(text = "${task.format} • ${task.language.uppercase()}", color = NeonCyan, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
