package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.VideoStatus

@Composable
fun StatusBadge(
    statusStr: String,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val status = VideoStatus.fromString(statusStr)
    val color = when (status) {
        VideoStatus.DRAFT -> Color(0xFF64748B)
        VideoStatus.IN_PROGRESS -> Color(0xFF00E5FF)
        VideoStatus.REVIEW_PENDING -> Color(0xFFF59E0B)
        VideoStatus.COMPLETED -> Color(0xFF10B981)
        VideoStatus.PUBLISHED -> Color(0xFF9D4EDD)
    }

    val label = AppStrings.get(status.labelKey, lang)

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("status_badge_${status.name.lowercase()}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = " $label",
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp
            )
        }
    }
}
