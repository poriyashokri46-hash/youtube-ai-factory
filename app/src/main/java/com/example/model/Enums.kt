package com.example.model

enum class VideoFormat(val labelKey: String, val aspectRatio: Float, val isShort: Boolean) {
    SHORTS("studio_format_shorts", 9f / 16f, true),
    LONG("studio_format_long", 16f / 9f, false);

    companion object {
        fun fromString(value: String): VideoFormat = try {
            valueOf(value)
        } catch (e: Exception) {
            SHORTS
        }
    }
}

enum class VideoStatus(val labelKey: String, val colorHex: Long) {
    DRAFT("dash_filter_all", 0xFF64748B),
    IN_PROGRESS("dash_in_progress", 0xFF00E5FF),
    REVIEW_PENDING("dash_pending_review", 0xFFF59E0B),
    COMPLETED("dash_completed", 0xFF10B981),
    PUBLISHED("dash_published", 0xFF9D4EDD);

    companion object {
        fun fromString(value: String): VideoStatus = try {
            valueOf(value)
        } catch (e: Exception) {
            DRAFT
        }
    }
}

enum class ToneType(val labelKey: String) {
    EDUCATIONAL("studio_tone_educational"),
    ENTERTAINING("studio_tone_entertaining"),
    STORYTELLING("studio_tone_storytelling"),
    NEWS("studio_tone_news"),
    DRAMATIC("studio_tone_dramatic");

    companion object {
        fun fromString(value: String): ToneType = try {
            valueOf(value)
        } catch (e: Exception) {
            EDUCATIONAL
        }
    }
}

enum class SubtitleStyle(val labelKey: String) {
    BOLD_YELLOW("prod_sub_bold_yellow"),
    CYAN_NEON("prod_sub_cyan_neon"),
    MINIMAL_WHITE("prod_sub_minimal_white");

    companion object {
        fun fromString(value: String): SubtitleStyle = try {
            valueOf(value)
        } catch (e: Exception) {
            BOLD_YELLOW
        }
    }
}

enum class VoiceStyle(val labelKey: String) {
    MALE_DEEP("prod_voice_male"),
    FEMALE_CLEAR("prod_voice_female"),
    ENERGETIC_YOUTH("prod_voice_energetic"),
    NARRATOR_DOC("prod_voice_narrator");

    companion object {
        fun fromString(value: String): VoiceStyle = try {
            valueOf(value)
        } catch (e: Exception) {
            MALE_DEEP
        }
    }
}

enum class VisualTheme(val labelKey: String) {
    CYBERPUNK("prod_style_cyberpunk"),
    MINIMAL_CLEAN("prod_style_minimal"),
    DARK_STUDIO("prod_style_gradient");

    companion object {
        fun fromString(value: String): VisualTheme = try {
            valueOf(value)
        } catch (e: Exception) {
            CYBERPUNK
        }
    }
}
