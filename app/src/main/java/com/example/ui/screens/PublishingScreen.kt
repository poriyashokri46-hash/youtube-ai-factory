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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoProject
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun PublishingScreen(
    viewModel: MainViewModel,
    projects: List<VideoProject>,
    lang: AppLanguage,
    onConnectYouTube: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.isYouTubeConnected.collectAsState()
    val channelName by viewModel.channelName.collectAsState()
    val channelHandle by viewModel.channelHandle.collectAsState()

    val selectedId by viewModel.selectedPublishProjectId.collectAsState()
    val visibility by viewModel.publishVisibility.collectAsState()
    val category by viewModel.publishCategory.collectAsState()
    val scheduleDate by viewModel.publishScheduleDate.collectAsState()

    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    var showOAuthDialog by remember { mutableStateOf(false) }

    val activeProject = projects.find { it.id == selectedId } ?: projects.firstOrNull()

    // OAuth Consent Dialog
    if (showOAuthDialog) {
        AlertDialog(
            onDismissRequest = { showOAuthDialog = false },
            containerColor = Color(0xFF151D33),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "اتصال ایمن Google OAuth 2.0" else "Secure Google OAuth 2.0",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "برای اتصال کانال، گوگل مجوز دسترسی به کانال، آپلود و آمار را درخواست می‌کند. توکن دسترسی فقط در حافظهٔ همین اجرای برنامه نگه‌داری می‌شود؛ با بسته‌شدن برنامه باید دوباره متصل شوید. انتشار عمومی را فقط برای محتوایی انتخاب کنید که آمادهٔ انتشار است."
                        else
                            "Google will ask for permission to access your channel, upload videos and read channel statistics. The access token is held in memory only, so reconnect after restarting the app. Choose public visibility only when your video is ready to publish.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOAuthDialog = false
                        onConnectYouTube()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(text = if (lang == AppLanguage.PERSIAN) "متوجه شدم" else "Got it", color = Color(0xFF001F24), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOAuthDialog = false }) {
                    Text(text = AppStrings.get("cancel", lang), color = TextMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("publishing_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Column {
                Text(
                    text = AppStrings.get("pub_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("pub_subtitle", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Channel Connection Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("yt_connection_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) AppStrings.get("pub_connected", lang) else AppStrings.get("pub_disconnected", lang),
                                color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (isConnected) {
                            Text(
                                text = AppStrings.get("pub_disconnect_btn", lang),
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { viewModel.disconnectYouTube() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1E283E), CircleShape)
                                    .border(1.dp, NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "▶", color = Color(0xFFFF0055), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = channelName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = channelHandle, color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showOAuthDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("connect_oauth_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF001F24), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("pub_connect_btn", lang),
                                color = Color(0xFF001F24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Security Guarantee Callout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D1527), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("pub_security_guarantee", lang),
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Select Video to Upload
            Text(
                text = AppStrings.get("pub_select_video", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects) { p ->
                    val isSelected = activeProject?.id == p.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ElectricViolet.copy(alpha = 0.25f) else SurfaceNavyCard)
                            .border(1.dp, if (isSelected) ElectricViolet else BorderNavy, RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectProjectForPublishing(p.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("publish_select_chip_${p.id}")
                    ) {
                        Column {
                            Text(
                                text = p.title.take(22) + if (p.title.length > 22) "..." else "",
                                color = if (isSelected) ElectricViolet else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${p.format} • ${p.targetDurationSec}s",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (activeProject != null) {
            item {
                // Upload Form Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = AppStrings.get("seo_title_label", lang),
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeProject.title,
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visibility Options
                        Text(text = AppStrings.get("pub_visibility", lang), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val visOptions = listOf(
                                "Public" to AppStrings.get("pub_vis_public", lang),
                                "Unlisted" to AppStrings.get("pub_vis_unlisted", lang),
                                "Private" to AppStrings.get("pub_vis_private", lang)
                            )
                            visOptions.forEach { (key, label) ->
                                val isSel = visibility == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonCyan else Color(0xFF1E283E))
                                        .border(1.dp, if (isSel) NeonCyan else BorderNavy, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.publishVisibility.value = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color(0xFF001F24) else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category
                        Text(text = AppStrings.get("pub_category", lang), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val categories = listOf(
                                "Science & Tech" to AppStrings.get("pub_cat_tech", lang),
                                "Education" to AppStrings.get("pub_cat_education", lang),
                                "Entertainment" to AppStrings.get("pub_cat_entertainment", lang)
                            )
                            categories.forEach { (catKey, label) ->
                                val isSel = category == catKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) ElectricViolet else Color(0xFF1E283E))
                                        .border(1.dp, if (isSel) ElectricViolet else BorderNavy, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.publishCategory.value = catKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label.split(" ").first(),
                                        color = if (isSel) Color.White else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Upload Action / Progress
                        if (isUploading) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = AppStrings.get("pub_uploading", lang), color = Color(0xFF10B981), fontSize = 12.sp)
                                    Text(text = "$uploadProgress%", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { uploadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFF1E283E)
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startYouTubeUpload() },
                                enabled = isConnected && !isUploading && activeProject?.renderedVideoPath?.let { java.io.File(it).isFile && java.io.File(it).length() > 0L } == true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_upload_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "انتشار ویدیو در یوتیوب" else "Upload video to YouTube",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN)
                                    "برای انتشار باید حساب گوگل متصل باشد و پروژه یک فایل MP4 رندرشده داشته باشد. آپلود از طریق YouTube Data API انجام می‌شود."
                                else
                                    "Connect Google and render an MP4 before uploading. The upload uses the YouTube Data API.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Upload Queue & History
                Text(
                    text = AppStrings.get("pub_queue_title", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(10.dp))

                val publishedVideos = projects.filter { project ->
                    project.isUploaded &&
                        project.status.equals("PUBLISHED", ignoreCase = true) &&
                        !project.youtubeVideoId.isNullOrBlank()
                }
                if (publishedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceNavyCard, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderNavy, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (lang == AppLanguage.PERSIAN) "هنوز ویدیویی در یوتیوب منتشر نشده است" else "No published YouTube videos yet", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    publishedVideos.forEach { vid ->
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
                                    Text(text = vid.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "ID: ${vid.youtubeVideoId}", color = NeonCyan, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = if (lang == AppLanguage.PERSIAN) "✓ منتشرشده" else "✓ Published", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
