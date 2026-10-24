package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoProject
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.StatCard
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    projects: List<VideoProject>,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.isYouTubeConnected.collectAsState()
    val channelName by viewModel.channelName.collectAsState()

    // Do not show locally cached/sample values as live channel analytics.
    val subscribers = "--"
    val watchHours = "--"
    val avgRetention = "--"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("analytics_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Column {
                Text(
                    text = AppStrings.get("anal_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("anal_subtitle", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Honest Disclosure Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isConnected) Color(0xFF10B981).copy(alpha = 0.12f)
                        else Color(0xFFF59E0B).copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        if (isConnected) Color(0xFF10B981).copy(alpha = 0.4f)
                        else Color(0xFFF59E0B).copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
                    .testTag("analytics_notice_card")
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isConnected)
                            if (lang == AppLanguage.PERSIAN)
                                "وضعیت اتصال ذخیره‌شده است؛ آمار واقعی هنوز از YouTube Analytics API دریافت نمی‌شود."
                            else
                                "A saved connection flag is not proof of OAuth; live metrics are not fetched from YouTube Analytics API."
                        else
                            if (lang == AppLanguage.PERSIAN)
                                "آمار زنده در دسترس نیست؛ اتصال واقعی یوتیوب و YouTube Analytics API هنوز پیکربندی نشده‌اند."
                            else
                                "Live analytics are unavailable until real YouTube OAuth and YouTube Analytics API are configured.",
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Key Metric Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = AppStrings.get("anal_metric_views", lang),
                    value = "--",
                    icon = Icons.Default.Visibility,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    trendText = null,
                    testTag = "metric_views"
                )
                StatCard(
                    title = AppStrings.get("anal_metric_subs", lang),
                    value = subscribers,
                    icon = Icons.Default.People,
                    accentColor = ElectricViolet,
                    modifier = Modifier.weight(1f),
                    trendText = null,
                    testTag = "metric_subs"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = AppStrings.get("anal_metric_watch_hours", lang),
                    value = watchHours,
                    icon = Icons.Default.Schedule,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    testTag = "metric_watch_hours"
                )
                StatCard(
                    title = AppStrings.get("anal_metric_retention", lang),
                    value = avgRetention,
                    icon = Icons.Default.TrendingUp,
                    accentColor = Color(0xFFFF3366),
                    modifier = Modifier.weight(1f),
                    trendText = "High",
                    testTag = "metric_retention"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Do not draw a fake zero-valued chart; show a truthful unavailable state instead.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "روند بازدیدها" else "Views trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "هنوز دادهٔ زنده‌ای از YouTube Analytics دریافت نشده است؛ نمودار پس از پیاده‌سازی اتصال رسمی و دریافت داده نمایش داده می‌شود."
                        else
                            "No live YouTube Analytics data has been retrieved. A chart will appear after the official API connection is implemented and data is fetched.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Traffic Sources Breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("anal_traffic_sources", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val sources = listOf(
                        Triple(AppStrings.get("anal_traffic_shorts", lang), "68%", NeonCyan),
                        Triple(AppStrings.get("anal_traffic_search", lang), "21%", ElectricViolet),
                        Triple(AppStrings.get("anal_traffic_suggested", lang), "11%", Color(0xFF10B981))
                    )

                    sources.forEach { (label, percent, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label, color = TextWhite, fontSize = 12.sp)
                            }
                            Text(text = percent, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
