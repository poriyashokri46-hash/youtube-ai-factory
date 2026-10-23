package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SubtitleStyle
import com.example.model.VideoFormat
import com.example.model.VisualTheme
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun VideoPlayerCanvas(
    title: String,
    hookText: String,
    fullScript: String,
    format: VideoFormat,
    visualTheme: VisualTheme,
    subtitleStyle: SubtitleStyle,
    durationSec: Int = 60,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSeconds by remember { mutableFloatStateOf(0f) }

    // Playback loop
    LaunchedEffect(isPlaying, durationSec) {
        while (isPlaying) {
            delay(100)
            currentSeconds += 0.1f
            if (currentSeconds >= durationSec) {
                currentSeconds = 0f
            }
        }
    }

    // Animation for ambient waves and sound visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "player_animation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Dynamic subtitle line based on current time
    val scriptLines = remember(fullScript, hookText) {
        val raw = fullScript.split("\n", ".").filter { it.isNotBlank() }
        if (raw.isNotEmpty()) raw else listOf(hookText, "YouTube AI Factory", "Subscribe for more!")
    }

    val currentLineIndex = remember(currentSeconds, scriptLines) {
        val total = scriptLines.size.coerceAtLeast(1)
        val index = ((currentSeconds / durationSec) * total).toInt().coerceIn(0, total - 1)
        index
    }
    val currentSubtitleText = scriptLines.getOrNull(currentLineIndex) ?: hookText

    // Ratio styling
    val containerAspectRatio = if (format == VideoFormat.SHORTS) 9f / 14f else 16f / 9f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceNavy)
            .border(1.dp, Color(0xFF263352), RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag("video_player_container")
    ) {
        // Video Screen Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(containerAspectRatio)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.5.dp, Color(0xFF1E283E), RoundedCornerShape(16.dp))
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            // Visual Theme Background Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Background Gradient
                val bgColors = when (visualTheme) {
                    VisualTheme.CYBERPUNK -> listOf(
                        Color(0xFF0F051D),
                        Color(0xFF001B2E),
                        Color(0xFF003847)
                    )
                    VisualTheme.MINIMAL_CLEAN -> listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF21262D)
                    )
                    VisualTheme.DARK_STUDIO -> listOf(
                        Color(0xFF050505),
                        Color(0xFF140D24),
                        Color(0xFF210926)
                    )
                }

                drawRect(
                    brush = Brush.radialGradient(
                        colors = bgColors,
                        center = Offset(canvasWidth * 0.5f, canvasHeight * 0.45f),
                        radius = canvasWidth * 0.9f
                    )
                )

                // Neon Grid / Glowing Ambient Rays
                if (visualTheme == VisualTheme.CYBERPUNK) {
                    val beamColor = NeonCyan.copy(alpha = 0.12f)
                    for (i in 0..6) {
                        val x = canvasWidth * (i / 6f)
                        drawLine(
                            color = beamColor,
                            start = Offset(x, 0f),
                            end = Offset(canvasWidth * 0.5f, canvasHeight),
                            strokeWidth = 2f
                        )
                    }
                }

                // Audio Visualizer Wave Bars at bottom center
                if (isPlaying) {
                    val barCount = 18
                    val barWidth = 6f
                    val spacing = 8f
                    val totalWidth = barCount * (barWidth + spacing)
                    val startX = (canvasWidth - totalWidth) / 2f
                    val baseY = canvasHeight * 0.72f

                    for (i in 0 until barCount) {
                        val barHeight = (sin(waveOffset + i * 0.6f) * 24f + 30f).coerceIn(8f, 65f)
                        val barColor = if (i % 2 == 0) NeonCyan.copy(alpha = 0.85f) else ElectricViolet.copy(alpha = 0.85f)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(startX + i * (barWidth + spacing), baseY - barHeight / 2),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            }

            // Central Video Title / Badge Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (format == VideoFormat.SHORTS) "▶ YOUTUBE SHORTS" else "▶ YOUTUBE 16:9",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title.ifBlank { "YouTube AI Factory Video" },
                    color = TextWhite,
                    fontSize = if (format == VideoFormat.SHORTS) 15.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            // Central Play/Pause Overlay indicator when paused
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xAA000000), CircleShape)
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Dynamic Subtitle Overlay at bottom of video
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                when (subtitleStyle) {
                    SubtitleStyle.BOLD_YELLOW -> {
                        Box(
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(8.dp))
                                .background(Color(0xEE000000), RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFFFFD600), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentSubtitleText,
                                color = Color(0xFFFFD600),
                                fontSize = if (format == VideoFormat.SHORTS) 15.sp else 17.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    SubtitleStyle.CYAN_NEON -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0xCC05101E), RoundedCornerShape(8.dp))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentSubtitleText,
                                color = NeonCyan,
                                fontSize = if (format == VideoFormat.SHORTS) 14.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    SubtitleStyle.MINIMAL_WHITE -> {
                        Box(
                            modifier = Modifier
                                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = currentSubtitleText,
                                color = Color.White,
                                fontSize = if (format == VideoFormat.SHORTS) 13.sp else 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Playback Controls & Scrubber
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeonCyan
                )
            }

            IconButton(
                onClick = { currentSeconds = 0f },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Slider(
                value = currentSeconds,
                onValueChange = { currentSeconds = it },
                valueRange = 0f..durationSec.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0xFF1E283E)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${currentSeconds.toInt()}s / ${durationSec}s",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
