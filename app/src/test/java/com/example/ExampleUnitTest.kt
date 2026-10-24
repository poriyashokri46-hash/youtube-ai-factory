package com.example

import com.example.data.SampleData
import com.example.data.VideoProject
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.SubtitleStyle
import com.example.model.VideoFormat
import com.example.model.VideoStatus
import com.example.model.VisualTheme
import com.example.model.VoiceStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLocalizationPersianAndEnglish() {
        val appNameFa = AppStrings.get("app_name", AppLanguage.PERSIAN)
        val appNameEn = AppStrings.get("app_name", AppLanguage.ENGLISH)
        assertEquals("کارخانه هوش مصنوعی یوتیوب", appNameFa)
        assertEquals("YouTube AI Factory", appNameEn)

        val navDashboardFa = AppStrings.get("nav_dashboard", AppLanguage.PERSIAN)
        val navDashboardEn = AppStrings.get("nav_dashboard", AppLanguage.ENGLISH)
        assertEquals("داشبورد", navDashboardFa)
        assertEquals("Dashboard", navDashboardEn)

        val pickImagesFa = AppStrings.get("prod_pick_images", AppLanguage.PERSIAN)
        val pickImagesEn = AppStrings.get("prod_pick_images", AppLanguage.ENGLISH)
        assertTrue(pickImagesFa.isNotBlank())
        assertTrue(pickImagesEn.isNotBlank())
    }

    @Test
    fun testEnumsParsing() {
        val shorts = VideoFormat.fromString("SHORTS")
        val longFormat = VideoFormat.fromString("LONG")
        assertEquals(VideoFormat.SHORTS, shorts)
        assertEquals(VideoFormat.LONG, longFormat)

        val completedStatus = VideoStatus.fromString("COMPLETED")
        assertEquals(VideoStatus.COMPLETED, completedStatus)

        val subStyle = SubtitleStyle.fromString("BOLD_YELLOW")
        assertEquals(SubtitleStyle.BOLD_YELLOW, subStyle)

        val theme = VisualTheme.fromString("CYBERPUNK")
        assertEquals(VisualTheme.CYBERPUNK, theme)

        val voice = VoiceStyle.fromString("MALE_DEEP")
        assertEquals(VoiceStyle.MALE_DEEP, voice)
    }

    @Test
    fun testVideoProjectWithMediaFields() {
        val project = VideoProject(
            id = 10,
            title = "تست ویدیوی یوتیوب",
            topic = "هوش مصنوعی",
            selectedImageUris = "content://media/1,content://media/2",
            selectedAudioUri = "content://audio/sample.mp3",
            renderedVideoPath = "/movies/test.mp4",
            renderedVideoUri = "content://media/external/video/media/100"
        )
        assertEquals(2, project.selectedImageUris.split(",").size)
        assertNotNull(project.selectedAudioUri)
        assertNotNull(project.renderedVideoPath)
    }
    @Test
    fun sampleProjectsNeverClaimToBeRenderedOrPublished() {
        val projects = SampleData.getInitialProjects()
        assertTrue(projects.isNotEmpty())
        assertTrue(SampleData.getInitialTasks().isEmpty())
        projects.forEach { project ->
            assertEquals("DRAFT", project.status)
            assertEquals(false, project.isUploaded)
            assertEquals(null, project.youtubeVideoId)
            assertEquals(null, project.renderedVideoPath)
            assertEquals(0L, project.viewsCount)
            assertEquals(0, project.renderProgress)
        }
    }

}
