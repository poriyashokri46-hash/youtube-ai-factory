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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
    val analytics by viewModel.youtubeAnalytics.collectAsState()
    val isLoadingAnalytics by viewModel.isLoadingAnalytics.collectAsState()
    val views = analytics?.views?.let { String.format("%,d", it) } ?: "--"
    val subscribers = analytics?.subscribersGained?.let { String.format("%,d", it) } ?: "--"
    val watchHours = analytics?.watchHours?.let { String.format("%.1f", it) } ?: "--"
    val avgRetention = analytics?.averageViewPercentage?.let { String.format("%.1f%%", it) } ?: "--"

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
                        text = when {
                            analytics != null && lang == AppLanguage.PERSIAN -> "آمار واقعی کانال «$channelName» برای ۲۸ روز اخیر از YouTube Analytics API دریافت شده است."
                            analytics != null -> "Live YouTube Analytics data for “$channelName” was fetched for the last 28 days."
                            isConnected && lang == AppLanguage.PERSIAN -> "حساب متصل است. برای دریافت آمار زنده، دکمهٔ به‌روزرسانی را بزنید."
                            isConnected -> "YouTube is connected. Tap refresh to fetch live channel analytics."
                            lang == AppLanguage.PERSIAN -> "برای دریافت آمار زنده، ابتدا حساب یوتیوب را متصل کنید."
                            else -> "Connect your YouTube account to fetch live analytics."
                        },
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.refreshYouTubeAnalytics() },
                enabled = isConnected && !isLoadingAnalytics,
                modifier = Modifier.fillMaxWidth().testTag("refresh_analytics_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(
                    text = when {
                        isLoadingAnalytics && lang == AppLanguage.PERSIAN -> "در حال دریافت آمار..."
                        isLoadingAnalytics -> "Fetching analytics..."
                        lang == AppLanguage.PERSIAN -> "به‌روزرسانی آمار زنده"
                        else -> "Refresh live analytics"
                    },
                    color = Color(0xFF001F24),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Key Metric Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = AppStrings.get("anal_metric_views", lang),
                    value = views,
                    icon = Icons.Default.Visibility,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    trendText = null,
                    testTag = "metric_views"
                )
                StatCard(
                    title = if (lang == AppLanguage.PERSIAN) "عضو جدید (۲۸ روز)" else "Subscribers gained (28d)",
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
                    trendText = null,
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
                        text = if (analytics == null) {
                            if (lang == AppLanguage.PERSIAN) "پس از اتصال و دریافت آمار، روند بازدید روزانه در این بخش نمایش داده می‌شود."
                            else "Daily views will appear here after connecting and fetching analytics."
                        } else {
                            if (lang == AppLanguage.PERSIAN) "بازدید روزانه در ۲۸ روز اخیر"
                            else "Daily views over the last 28 days"
                        },
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    val recentDays = analytics?.dailyViews.orEmpty().takeLast(7)
                    if (recentDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val maxDailyViews = recentDays.maxOf { it.second }.coerceAtLeast(1L)
                        recentDays.forEach { (date, dayViews) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = date.takeLast(5), color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(44.dp))
                                LinearProgressIndicator(
                                    progress = { dayViews.toFloat() / maxDailyViews.toFloat() },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = NeonCyan,
                                    trackColor = Color(0xFF1E283E)
                                )
                                Text(text = String.format("%,d", dayViews), color = TextWhite, fontSize = 10.sp, modifier = Modifier.width(58.dp).padding(start = 6.dp))
                            }
                        }
                    }
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

                    val sources = analytics?.trafficSources.orEmpty()
                    if (sources.isEmpty()) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "دادهٔ منبع بازدید در دسترس نیست." else "No traffic-source data is available.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        val totalSourceViews = sources.sumOf { it.second }.coerceAtLeast(1L)
                        sources.take(8).forEachIndexed { index, source ->
                            val label = when (source.first) {
                                "YT_SEARCH" -> if (lang == AppLanguage.PERSIAN) "جستجوی یوتیوب" else "YouTube Search"
                                "RELATED_VIDEO" -> if (lang == AppLanguage.PERSIAN) "ویدیوهای مرتبط" else "Related videos"
                                "BROWSE_FEATURES" -> if (lang == AppLanguage.PERSIAN) "صفحهٔ اصلی و مرور" else "Browse features"
                                "SHORTS" -> "Shorts"
                                "EXT_URL" -> if (lang == AppLanguage.PERSIAN) "منابع خارجی" else "External"
                                "NOTIFICATION" -> if (lang == AppLanguage.PERSIAN) "اعلان‌ها" else "Notifications"
                                else -> source.first.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                            }
                            val percent = source.second.toFloat() / totalSourceViews.toFloat()
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, color = TextWhite, fontSize = 12.sp)
                                    Text(text = "${(percent * 100).toInt()}% • ${String.format("%,d", source.second)}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (index % 2 == 0) NeonCyan else ElectricViolet,
                                    trackColor = Color(0xFF1E283E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
