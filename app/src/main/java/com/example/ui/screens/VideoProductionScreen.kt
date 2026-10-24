package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VideoProject
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.SubtitleStyle
import com.example.model.VideoFormat
import com.example.model.VisualTheme
import com.example.model.VoiceStyle
import com.example.ui.components.RealVideoPlayerView
import com.example.ui.components.VideoPlayerCanvas
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.MainViewModel

@Composable
fun VideoProductionScreen(
    viewModel: MainViewModel,
    projects: List<VideoProject>,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val selectedId by viewModel.selectedProductionProjectId.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()
    val renderProgress by viewModel.renderProgress.collectAsState()
    val renderStepMessage by viewModel.renderStepMessage.collectAsState()

    val voiceStyle by viewModel.prodVoiceStyle.collectAsState()
    val speechRate by viewModel.prodSpeechRate.collectAsState()
    val visualTheme by viewModel.prodVisualTheme.collectAsState()
    val subtitleStyle by viewModel.prodSubtitleStyle.collectAsState()

    val selectedImages by viewModel.selectedImageUris.collectAsState()
    val selectedAudioUri by viewModel.selectedAudioUri.collectAsState()
    val selectedAudioName by viewModel.selectedAudioName.collectAsState()
    val renderedVideoUri by viewModel.renderedVideoUri.collectAsState()
    val renderingError by viewModel.renderingError.collectAsState()

    var showCanvasSimulator by remember { mutableStateOf(false) }

    // Multi-Image Picker using Android Photo Picker (zero storage permissions)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
        }
    }

    // Audio file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAudioSelected(uri)
        }
    }

    // Find active project or first available
    val activeProject = projects.find { it.id == selectedId } ?: projects.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("video_production_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Column {
                Text(
                    text = AppStrings.get("prod_title", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("prod_subtitle", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Selector Chips
            Text(
                text = AppStrings.get("prod_select_project", lang),
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects) { p ->
                    val isSelected = activeProject?.id == p.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceNavyCard)
                            .border(1.dp, if (isSelected) NeonCyan else BorderNavy, RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectProjectForProduction(p.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("select_project_chip_${p.id}")
                    ) {
                        Text(
                            text = p.title.take(24) + if (p.title.length > 24) "..." else "",
                            color = if (isSelected) NeonCyan else TextWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (activeProject != null) {
            // 1. User Media Selection Card (Images & Narration Audio)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("user_media_card"),
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
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("prod_pick_images", lang),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            if (selectedImages.isNotEmpty()) {
                                Text(
                                    text = "${selectedImages.size} ${AppStrings.get("prod_images_selected", lang)}",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected Images Horizontal Strip with delete buttons
                        if (selectedImages.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedImages) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black)
                                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(22.dp)
                                                .background(Color(0xCC000000), CircleShape)
                                                .clickable { viewModel.removeSelectedImage(uri) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Pick Images Action Button
                        Button(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("pick_images_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedImages.isEmpty())
                                    AppStrings.get("prod_pick_images", lang)
                                else
                                    "+ افزودن تصاویر بیشتر",
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF172236))
                                .padding(10.dp)
                                .testTag("audio_format_notice")
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN)
                                    "فرمت قابل قبول برای ترکیب مستقیم با MP4: صدای AAC داخل فایل M4A/MP4. فایل‌های MP3 و WAV در نسخهٔ فعلی تبدیل نمی‌شوند و ممکن است رندر با خطا متوقف شود."
                                else
                                    "Direct MP4 muxing currently supports AAC audio in M4A/MP4 files. MP3 and WAV are not transcoded and may cause rendering to fail.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Audio Narration File Selector
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get("prod_pick_audio", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedAudioUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ElectricViolet.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, ElectricViolet, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedAudioName ?: "audio_narration.mp3",
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onAudioSelected(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear audio", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("pick_audio_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.get("prod_pick_audio", lang),
                                    color = TextWhite,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Production Pipeline & Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("production_config_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceNavyCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Be explicit: these controls do not synthesize narration yet.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2A1A24))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                                .testTag("tts_not_implemented_notice")
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN)
                                    "توجه: تولید صدای گفتاری از متن هنوز پیاده‌سازی نشده است. تنظیمات نوع صدا و سرعت فعلاً روی فایل MP4 اثر ندارند. برای افزودن صدا، فایل صوتی جداگانه انتخاب کنید."
                                else
                                    "Note: text-to-speech narration is not implemented yet. Voice style and speech-rate settings do not affect the MP4. Select a separate audio file to add sound.",
                                color = TextWhite,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Profile
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get("prod_voice_style", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(VoiceStyle.values()) { v ->
                                val isSelected = voiceStyle == v
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonCyan else Color(0xFF1E283E))
                                        .border(1.dp, if (isSelected) NeonCyan else BorderNavy, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.prodVoiceStyle.value = v }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = AppStrings.get(v.labelKey, lang),
                                        color = if (isSelected) Color(0xFF001F24) else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Speech Rate Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = AppStrings.get("prod_speech_rate", lang), color = TextMuted, fontSize = 12.sp)
                            Text(text = "${String.format("%.1f", speechRate)}x", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = speechRate,
                            onValueChange = { viewModel.prodSpeechRate.value = it },
                            valueRange = 0.8f..1.4f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color(0xFF1E283E)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Aesthetic Theme
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get("prod_visual_style", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VisualTheme.values().forEach { vt ->
                                val isSelected = visualTheme == vt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricViolet else Color(0xFF1E283E))
                                        .border(1.dp, if (isSelected) ElectricViolet else BorderNavy, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.prodVisualTheme.value = vt }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = AppStrings.get(vt.labelKey, lang),
                                        color = if (isSelected) Color.White else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Subtitle Style Preset
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get("prod_subtitle_style", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SubtitleStyle.values().forEach { st ->
                                val isSelected = subtitleStyle == st
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFFFD600) else Color(0xFF1E283E))
                                        .border(1.dp, if (isSelected) Color(0xFFFFD600) else BorderNavy, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.prodSubtitleStyle.value = st }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = AppStrings.get(st.labelKey, lang),
                                        color = if (isSelected) Color.Black else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Error Banner if any
                        if (renderingError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF33141E), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${AppStrings.get("prod_error_title", lang)}: $renderingError",
                                        color = Color(0xFFFFB3B3),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Real MP4 Video Render Action Button & Stepper
                        if (isRendering) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = AppStrings.get("prod_rendering_progress", lang), color = NeonCyan, fontSize = 12.sp)
                                    Text(text = "$renderProgress%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { renderProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = NeonCyan,
                                    trackColor = Color(0xFF1E283E)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = renderStepMessage, color = TextMuted, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startRenderPipeline() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_rendering_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color(0xFF001F24), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("prod_start_rendering", lang),
                                    color = Color(0xFF001F24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Video Player Area (Real ExoPlayer vs Interactive Canvas)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (renderedVideoUri != null && !showCanvasSimulator)
                            AppStrings.get("prod_real_player_title", lang)
                        else
                            AppStrings.get("prod_live_player", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )

                    if (renderedVideoUri != null) {
                        Text(
                            text = if (showCanvasSimulator) "▶ پخش ویدیوی واقعی MP4" else "⚙ شبیه‌ساز گرافیکی",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showCanvasSimulator = !showCanvasSimulator }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // If real MP4 exists and not toggled to simulator, play with native Media3 ExoPlayer!
                if (renderedVideoUri != null && !showCanvasSimulator) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF00363D), RoundedCornerShape(10.dp))
                                .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.get("prod_real_mp4_ready", lang),
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        RealVideoPlayerView(
                            videoUri = renderedVideoUri!!,
                            isShorts = activeProject.format == "SHORTS"
                        )
                    }
                } else {
                    // Interactive Canvas Player Preview
                    VideoPlayerCanvas(
                        title = activeProject.title,
                        hookText = activeProject.scriptHook,
                        fullScript = activeProject.fullScript,
                        format = VideoFormat.fromString(activeProject.format),
                        visualTheme = visualTheme,
                        subtitleStyle = subtitleStyle,
                        durationSec = activeProject.targetDurationSec
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save/Export MP4 Video to Device Storage Button
                Button(
                    onClick = { viewModel.exportVideoToDevice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_video_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderNavy)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("prod_save_device", lang),
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
