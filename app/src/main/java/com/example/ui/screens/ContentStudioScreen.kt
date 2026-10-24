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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.ToneType
import com.example.model.VideoFormat
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun ContentStudioScreen(
    viewModel: MainViewModel,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val topic by viewModel.topicInput.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val isGeneratingScript by viewModel.isGeneratingScript.collectAsState()
    val contentLang by viewModel.contentLang.collectAsState()
    val format by viewModel.formatSelection.collectAsState()
    val durationSec by viewModel.durationSec.collectAsState()
    val tone by viewModel.toneSelection.collectAsState()

    val hook by viewModel.hookInput.collectAsState()
    val intro by viewModel.introInput.collectAsState()
    val body by viewModel.bodyInput.collectAsState()
    val cta by viewModel.ctaInput.collectAsState()
    val outro by viewModel.outroInput.collectAsState()

    val title by viewModel.titleInput.collectAsState()
    val description by viewModel.descriptionInput.collectAsState()
    val tags by viewModel.tagsInput.collectAsState()

    // Calculate word count and estimated reading time
    val fullText = "$hook $intro $body $cta $outro"
    val wordCount = if (fullText.isBlank()) 0 else fullText.trim().split("\\s+".toRegex()).size
    val estimatedNarrationSec = (wordCount / 2.5f).toInt() // average ~150 words per minute

    val suggestedTopics = if (lang == AppLanguage.PERSIAN) {
        listOf(
            "۱۰ ابزار انقلابی هوش مصنوعی در سال ۲۰۲۵",
            "کسب درآمد دلاری از کانال یوتیوب بدون چهره",
            "چگونه با تکنیک پومودورو تمرکز را ۱۰ برابر کنیم؟",
            "۵ عادت روزانه افراد فوق‌العاده موفق",
            "راهنمای ساخت پادکست خودکار با هوش مصنوعی"
        )
    } else {
        listOf(
            "10 Mind-Blowing AI Tools for 2025",
            "How to Start a Faceless YouTube Automation Channel",
            "5 Productivity Hacks Used by Top 1% Creators",
            "Mastering YouTube Shorts Algorithm Secrets",
            "Automated Video Editing Workflows"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("content_studio_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Studio Header
            Column {
                Text(
                    text = AppStrings.get("studio_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("studio_subtitle", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Topic Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("studio_topic_label", lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { viewModel.topicInput.value = it },
                        modifier = Modifier.fillMaxWidth().testTag("studio_topic_input"),
                        placeholder = { Text(AppStrings.get("studio_topic_hint", lang), color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = AppStrings.get("studio_topic_suggestions", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestedTopics) { sug ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E283E))
                                    .border(1.dp, Color(0xFF2E3D5C), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.topicInput.value = sug }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(text = sug, color = TextWhite, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Language & Format Selectors Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Content Language selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.get("studio_content_lang", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isFa = contentLang == "fa"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isFa) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E283E))
                                        .border(1.dp, if (isFa) NeonCyan else BorderNavy, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.contentLang.value = "fa" }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "فارسی", color = if (isFa) NeonCyan else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!isFa) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E283E))
                                        .border(1.dp, if (!isFa) NeonCyan else BorderNavy, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.contentLang.value = "en" }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "English", color = if (!isFa) NeonCyan else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Format selector (Shorts vs Long)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.get("studio_format_label", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isShorts = format == VideoFormat.SHORTS
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isShorts) ElectricViolet.copy(alpha = 0.25f) else Color(0xFF1E283E))
                                        .border(1.dp, if (isShorts) ElectricViolet else BorderNavy, RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.formatSelection.value = VideoFormat.SHORTS
                                            viewModel.durationSec.value = 60
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Shorts 9:16", color = if (isShorts) ElectricViolet else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!isShorts) ElectricViolet.copy(alpha = 0.25f) else Color(0xFF1E283E))
                                        .border(1.dp, if (!isShorts) ElectricViolet else BorderNavy, RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.formatSelection.value = VideoFormat.LONG
                                            viewModel.durationSec.value = 300
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Long 16:9", color = if (!isShorts) ElectricViolet else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Duration Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("studio_duration_label", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = if (format == VideoFormat.SHORTS) "${durationSec}s" else "${durationSec / 60}m ${durationSec % 60}s",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Slider(
                        value = durationSec.toFloat(),
                        onValueChange = { viewModel.durationSec.value = it.toInt() },
                        valueRange = if (format == VideoFormat.SHORTS) 15f..60f else 60f..900f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0xFF1E283E)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tone selector
                    Text(
                        text = AppStrings.get("studio_tone_label", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ToneType.values()) { t ->
                            val isSelected = tone == t
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricViolet else Color(0xFF1E283E))
                                    .border(1.dp, if (isSelected) ElectricViolet else BorderNavy, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toneSelection.value = t }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = AppStrings.get(t.labelKey, lang),
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Real Gemini API key (kept in memory only, not saved to disk)
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "کلید API جمینای (برای تولید واقعی)" else "Gemini API key (required for real AI)",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { viewModel.geminiApiKey.value = it },
                        modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_input"),
                        placeholder = { Text("AIza…", color = Color(0xFF64748B)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "کلید در حافظه برنامه می‌ماند و ذخیره نمی‌شود؛ با بستن برنامه باید دوباره وارد شود. کلید را در اختیار دیگران نگذارید و سهمیه/هزینه حساب Google خود را بررسی کنید."
                        else
                            "The key is memory-only and is not saved; re-enter it after closing the app. Never share it, and check your Google account quota/billing.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Generation Action Button
                    Button(
                        onClick = { viewModel.generateAiScript() },
                        enabled = !isGeneratingScript,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_ai_script_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGeneratingScript) { if (lang == AppLanguage.PERSIAN) "در حال تولید با Gemini..." else "Generating with Gemini..." } else AppStrings.get("script_generate_btn", lang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Script Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = AppStrings.get("script_builder_title", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        // Real-time counter badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E283E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$wordCount ${AppStrings.get("script_word_count", lang)} (~${estimatedNarrationSec}s)",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hook Field
                    Text(text = AppStrings.get("script_hook_label", lang), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = hook,
                        onValueChange = { viewModel.hookInput.value = it },
                        modifier = Modifier.fillMaxWidth().testTag("script_hook_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Intro Field
                    Text(text = AppStrings.get("script_intro_label", lang), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = intro,
                        onValueChange = { viewModel.introInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Body Field
                    Text(text = AppStrings.get("script_body_label", lang), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = { viewModel.bodyInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CTA Field
                    Text(text = AppStrings.get("script_cta_label", lang), color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = cta,
                        onValueChange = { viewModel.ctaInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Outro Field
                    Text(text = AppStrings.get("script_outro_label", lang), color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = outro,
                        onValueChange = { viewModel.outroInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // YouTube SEO Metadata Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "YouTube SEO & Metadata",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(text = AppStrings.get("seo_title_label", lang), color = TextWhite, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.titleInput.value = it },
                        modifier = Modifier.fillMaxWidth().testTag("seo_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Description
                    Text(text = AppStrings.get("seo_desc_label", lang), color = TextWhite, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.descriptionInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tags
                    Text(text = AppStrings.get("seo_tags_label", lang), color = TextWhite, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { viewModel.tagsInput.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(AppStrings.get("seo_tags_hint", lang), color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderNavy,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Project Button
            Button(
                onClick = { viewModel.saveProject() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_project_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color(0xFF001F24),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.get("save", lang) + " " + AppStrings.get("studio_title", lang),
                    color = Color(0xFF001F24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
