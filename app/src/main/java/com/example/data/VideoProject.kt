package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val topic: String,
    val language: String = "fa", // "fa" or "en"
    val format: String = "SHORTS", // "SHORTS" or "LONG"
    val targetDurationSec: Int = 60,
    val tone: String = "EDUCATIONAL",
    val scriptHook: String = "",
    val scriptIntro: String = "",
    val scriptBody: String = "",
    val scriptCta: String = "",
    val scriptOutro: String = "",
    val fullScript: String = "",
    val description: String = "",
    val tags: String = "",
    val status: String = "DRAFT", // DRAFT, IN_PROGRESS, REVIEW_PENDING, COMPLETED, PUBLISHED
    val voiceStyle: String = "MALE_DEEP",
    val speechRate: Float = 1.0f,
    val subtitleStyle: String = "BOLD_YELLOW",
    val visualTheme: String = "CYBERPUNK",
    val selectedImageUris: String = "", // Comma-separated or newline-separated URIs
    val selectedAudioUri: String? = null,
    val renderedVideoPath: String? = null,
    val renderedVideoUri: String? = null,
    val renderProgress: Int = 0, // 0 to 100
    val renderingError: String? = null,
    val youtubeVideoId: String? = null,
    val isUploaded: Boolean = false,
    val scheduledPublishTime: Long? = null,
    val viewsCount: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
