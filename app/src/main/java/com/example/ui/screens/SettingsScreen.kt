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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.VideoFormat
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showArchitectureModal by remember { mutableStateOf(false) }

    val contentLang by viewModel.contentLang.collectAsState()
    val format by viewModel.formatSelection.collectAsState()

    // Confirmation Dialog for Data Reset
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = Color(0xFF151D33),
            title = {
                Text(
                    text = AppStrings.get("set_clear_data", lang),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = AppStrings.get("set_clear_confirm", lang),
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        viewModel.resetToSampleData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(text = AppStrings.get("delete", lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(text = AppStrings.get("cancel", lang), color = TextMuted)
                }
            }
        )
    }

    // Architecture & Roadmap Modal
    if (showArchitectureModal) {
        AlertDialog(
            onDismissRequest = { showArchitectureModal = false },
            containerColor = Color(0xFF151D33),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "نقشه راه پیاده‌سازی واقعی" else "Production Implementation Roadmap",
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
                            "مراحل بعدی ارتقا به محیط پروداکشن واقعی:\n\n" +
                            "۱. رندر واقعی ویدیو:\nاستفاده از کتابخانه Mobile-FFmpeg یا Android MediaCodec جهت ترکیب فریم‌های گرافیکی و فایل‌های صوتی به فایل استاندارد MP4 با انکودینگ H.264/AAC.\n\n" +
                            "۲. احراز هویت YouTube OAuth 2.0:\nثبت کلاینت اندروید در کنسول Google Cloud با SHA-1 و افزودن دامنه youtube.upload جهت اخذ توکن امن Access Token بدون افشای Client Secret.\n\n" +
                            "۳. آپلود چندتکه‌ای (Resumable Upload):\nارسال مستقیم ویدیوها از طریق YouTube Data API v3 با قابلیت Resume در صورت قطعی اینترنت.\n\n" +
                            "۴. سرور اتوماسیون پس‌زمینه:\nاتصال به Cloud Run یا Firebase Functions برای اجرای وظایف سناریونویسی و رندر حتی در زمان خاموش بودن گوشی."
                        else
                            "Next Steps for Full Production Deployment:\n\n" +
                            "1. Real Video Rendering Engine:\nIntegrate Mobile-FFmpeg or Android MediaCodec to mux vector visual frames, audio waveforms, and dynamic subtitles into standard H.264/AAC MP4 files.\n\n" +
                            "2. Google Cloud OAuth 2.0:\nProvision an Android OAuth 2.0 Client in Google Cloud Console with SHA-1 fingerprint and youtube.upload scope to retrieve access tokens securely without bundling client secrets.\n\n" +
                            "3. Resumable YouTube Uploads:\nDirect integration with YouTube Data API v3 Resumable Upload protocol to handle large video files with auto-retry.\n\n" +
                            "4. Cloud Automation Worker:\nDeploy serverless workers on Cloud Run to auto-generate scripts and batch render on schedule 24/7.",
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showArchitectureModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(text = AppStrings.get("close", lang), color = Color(0xFF001F24), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Column {
                Text(
                    text = AppStrings.get("set_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("set_subtitle", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("set_app_language", lang),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val isFa = lang == AppLanguage.PERSIAN
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isFa) NeonCyan else Color(0xFF1E283E))
                                .border(1.dp, if (isFa) NeonCyan else BorderNavy, RoundedCornerShape(10.dp))
                                .clickable { viewModel.switchLanguage(AppLanguage.PERSIAN) }
                                .padding(vertical = 10.dp)
                                .testTag("set_lang_persian"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "فارسی (RTL)",
                                color = if (isFa) Color(0xFF001F24) else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isFa) NeonCyan else Color(0xFF1E283E))
                                .border(1.dp, if (!isFa) NeonCyan else BorderNavy, RoundedCornerShape(10.dp))
                                .clickable { viewModel.switchLanguage(AppLanguage.ENGLISH) }
                                .padding(vertical = 10.dp)
                                .testTag("set_lang_english"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "English (LTR)",
                                color = if (!isFa) Color(0xFF001F24) else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Default Creation Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("set_default_video_lang", lang),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val isFaDef = contentLang == "fa"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isFaDef) ElectricViolet else Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                .clickable { viewModel.contentLang.value = "fa" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "فارسی", color = if (isFaDef) Color.White else TextMuted, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (!isFaDef) ElectricViolet else Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                .clickable { viewModel.contentLang.value = "en" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "English", color = if (!isFaDef) Color.White else TextMuted, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = AppStrings.get("set_default_format", lang),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val isShorts = format == VideoFormat.SHORTS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isShorts) NeonCyan else Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                .clickable { viewModel.formatSelection.value = VideoFormat.SHORTS }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Shorts (9:16)", color = if (isShorts) Color(0xFF001F24) else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (!isShorts) NeonCyan else Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                .clickable { viewModel.formatSelection.value = VideoFormat.LONG }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Standard (16:9)", color = if (!isShorts) Color(0xFF001F24) else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Architecture & Technical Roadmap Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("set_architecture_title", lang),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "مشاهده معماری ماژولار و مراحل ارتقای پایپلاین رندر و آپلود یوتیوب به پروداکشن."
                        else
                            "Review modular architecture and steps to connect production video rendering and YouTube API.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showArchitectureModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("view_architecture_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = AppStrings.get("set_arch_btn", lang),
                            color = ElectricViolet,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Data Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("set_data_storage", lang),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("reset_sample_data_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261019)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = AppStrings.get("set_clear_data", lang),
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // About App Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("set_about_title", lang),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = AppStrings.get("set_about_desc", lang),
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
