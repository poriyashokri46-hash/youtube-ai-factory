package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AutomationTask
import com.example.data.PreferencesManager
import com.example.data.SampleData
import com.example.data.VideoProject
import com.example.localization.AppLanguage
import com.example.model.SubtitleStyle
import com.example.model.ToneType
import com.example.model.VideoFormat
import com.example.model.VideoStatus
import com.example.model.VisualTheme
import com.example.model.VoiceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    CONTENT_STUDIO,
    PRODUCTION,
    PUBLISHING,
    ANALYTICS,
    AUTOMATION,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.projectDao()
    private val automationDao = db.automationDao()
    val preferencesManager = PreferencesManager(application)

    val appLanguage: StateFlow<AppLanguage> = preferencesManager.appLanguage
    val isYouTubeConnected: StateFlow<Boolean> = preferencesManager.isYouTubeConnected
    val channelName: StateFlow<String> = preferencesManager.channelName
    val channelHandle: StateFlow<String> = preferencesManager.channelHandle
    val automationEnabled: StateFlow<Boolean> = preferencesManager.automationEnabled
    val automationTime: StateFlow<String> = preferencesManager.automationTime

    val projects: StateFlow<List<VideoProject>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<AutomationTask>> = automationDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _navigationStack = mutableListOf<AppScreen>()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Content Studio state
    private val _editingProjectId = MutableStateFlow<Long?>(null)
    val editingProjectId: StateFlow<Long?> = _editingProjectId.asStateFlow()

    val topicInput = MutableStateFlow("")
    // Bring-your-own Gemini key is kept in memory only; it is never written to preferences or the database.
    val geminiApiKey = MutableStateFlow("")
    private val _isGeneratingScript = MutableStateFlow(false)
    val isGeneratingScript: StateFlow<Boolean> = _isGeneratingScript.asStateFlow()
    private val geminiHttpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    val contentLang = MutableStateFlow("fa")
    val formatSelection = MutableStateFlow(VideoFormat.SHORTS)
    val durationSec = MutableStateFlow(60)
    val toneSelection = MutableStateFlow(ToneType.EDUCATIONAL)

    val hookInput = MutableStateFlow("")
    val introInput = MutableStateFlow("")
    val bodyInput = MutableStateFlow("")
    val ctaInput = MutableStateFlow("")
    val outroInput = MutableStateFlow("")

    val titleInput = MutableStateFlow("")
    val descriptionInput = MutableStateFlow("")
    val tagsInput = MutableStateFlow("")

    // Video Production state
    private val _selectedProductionProjectId = MutableStateFlow<Long?>(null)
    val selectedProductionProjectId: StateFlow<Long?> = _selectedProductionProjectId.asStateFlow()

    val prodVoiceStyle = MutableStateFlow(VoiceStyle.MALE_DEEP)
    val prodSpeechRate = MutableStateFlow(1.0f)
    val prodVisualTheme = MutableStateFlow(VisualTheme.CYBERPUNK)
    val prodSubtitleStyle = MutableStateFlow(SubtitleStyle.BOLD_YELLOW)

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    private val _renderProgress = MutableStateFlow(0)
    val renderProgress: StateFlow<Int> = _renderProgress.asStateFlow()

    private val _renderStepMessage = MutableStateFlow("")
    val renderStepMessage: StateFlow<String> = _renderStepMessage.asStateFlow()

    val selectedImageUris = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val selectedAudioUri = MutableStateFlow<android.net.Uri?>(null)
    val selectedAudioName = MutableStateFlow<String?>(null)
    val renderedVideoFile = MutableStateFlow<java.io.File?>(null)
    val renderedVideoUri = MutableStateFlow<android.net.Uri?>(null)
    val renderingError = MutableStateFlow<String?>(null)

    // Publishing state
    private val _selectedPublishProjectId = MutableStateFlow<Long?>(null)
    val selectedPublishProjectId: StateFlow<Long?> = _selectedPublishProjectId.asStateFlow()

    val publishVisibility = MutableStateFlow("Public")
    val publishCategory = MutableStateFlow("Science & Technology")
    val publishScheduleDate = MutableStateFlow("همین حالا (Immediately)")

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    private var renderJob: Job? = null
    private var uploadJob: Job? = null

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        if (_navigationStack.isNotEmpty()) {
            val previous = _navigationStack.removeAt(_navigationStack.lastIndex)
            _currentScreen.value = previous
            return true
        } else if (_currentScreen.value != AppScreen.DASHBOARD) {
            _currentScreen.value = AppScreen.DASHBOARD
            return true
        }
        return false
    }

    fun switchLanguage(language: AppLanguage) {
        preferencesManager.setLanguage(language)
    }

    fun toggleLanguage() {
        preferencesManager.toggleLanguage()
    }

    // Studio functions
    fun startNewProject() {
        _editingProjectId.value = null
        topicInput.value = ""
        contentLang.value = if (appLanguage.value == AppLanguage.PERSIAN) "fa" else "en"
        formatSelection.value = VideoFormat.SHORTS
        durationSec.value = 60
        toneSelection.value = ToneType.EDUCATIONAL
        hookInput.value = ""
        introInput.value = ""
        bodyInput.value = ""
        ctaInput.value = ""
        outroInput.value = ""
        titleInput.value = ""
        descriptionInput.value = ""
        tagsInput.value = ""
        navigateTo(AppScreen.CONTENT_STUDIO)
    }

    fun editProject(project: VideoProject) {
        _editingProjectId.value = project.id
        topicInput.value = project.topic
        contentLang.value = project.language
        formatSelection.value = VideoFormat.fromString(project.format)
        durationSec.value = project.targetDurationSec
        toneSelection.value = ToneType.fromString(project.tone)
        hookInput.value = project.scriptHook
        introInput.value = project.scriptIntro
        bodyInput.value = project.scriptBody
        ctaInput.value = project.scriptCta
        outroInput.value = project.scriptOutro
        titleInput.value = project.title
        descriptionInput.value = project.description
        tagsInput.value = project.tags
        navigateTo(AppScreen.CONTENT_STUDIO)
    }

    fun generateAiScript() {
        val topic = topicInput.value.trim()
        val apiKey = geminiApiKey.value.trim()
        if (topic.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit(if (appLanguage.value == AppLanguage.PERSIAN) "ابتدا موضوع ویدیو را وارد کنید." else "Enter a video topic first.")
            }
            return
        }
        if (apiKey.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit(if (appLanguage.value == AppLanguage.PERSIAN) "برای تولید واقعی، کلید API جمینای را در همین صفحه وارد کنید. کلید فقط تا بسته‌شدن برنامه در حافظه می‌ماند." else "For real generation, enter your Gemini API key on this screen. It stays in memory only until the app closes.")
            }
            return
        }
        if (_isGeneratingScript.value) return
        _isGeneratingScript.value = true
        viewModelScope.launch {
            try {
                val generated = withContext(Dispatchers.IO) {
                    requestGeminiScript(apiKey, topic)
                }
                titleInput.value = generated.optString("title").trim().ifBlank { error("AI response did not include a title.") }
                hookInput.value = generated.optString("hook").trim()
                introInput.value = generated.optString("intro").trim()
                bodyInput.value = generated.optString("body").trim()
                ctaInput.value = generated.optString("cta").trim()
                outroInput.value = generated.optString("outro").trim()
                descriptionInput.value = generated.optString("description").trim()
                tagsInput.value = generated.optString("tags").trim()
                _snackbarMessage.emit(if (appLanguage.value == AppLanguage.PERSIAN) "سناریو با پاسخ واقعی Gemini تولید شد؛ قبل از ذخیره بازبینی کنید." else "Script generated by Gemini. Review it before saving.")
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("HTTP 400") == true -> "درخواست Gemini پذیرفته نشد؛ کلید، مدل یا تنظیمات API را بررسی کنید."
                    e.message?.contains("HTTP 401") == true || e.message?.contains("HTTP 403") == true -> "دسترسی Gemini رد شد؛ اعتبار کلید و دسترسی API را بررسی کنید."
                    e.message?.contains("HTTP 429") == true -> "سهمیه یا محدودیت درخواست Gemini پر شده است؛ بعداً دوباره تلاش کنید."
                    else -> e.message?.take(220) ?: "خطای نامشخص در ارتباط با Gemini."
                }
                _snackbarMessage.emit(if (appLanguage.value == AppLanguage.PERSIAN) "تولید هوش مصنوعی انجام نشد: $message" else "AI generation failed: $message")
            } finally {
                _isGeneratingScript.value = false
            }
        }
    }

    private fun requestGeminiScript(apiKey: String, topic: String): JSONObject {
        val language = if (contentLang.value == "fa") "Persian (Farsi)" else "English"
        val format = if (formatSelection.value == VideoFormat.SHORTS) "YouTube Short" else "long-form YouTube video"
        val tone = toneSelection.value.name
        val prompt = """
            Create a useful, specific, non-clickbait $format script about: $topic.
            Output language: $language. Tone: $tone. Target duration: ${durationSec.value} seconds.
            Return ONLY a valid JSON object with exactly these string fields:
            title, hook, intro, body, cta, outro, description, tags.
            Make the body substantive and relevant to the topic. Do not invent factual statistics or sources.
            tags should be comma-separated. Do not wrap JSON in markdown fences.
        """.trimIndent()
        val requestJson = JSONObject()
            .put("contents", org.json.JSONArray().put(
                JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body)
            .build()
        geminiHttpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${responseText.take(300)}")
            val root = JSONObject(responseText)
            val text = root.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                ?: throw IllegalStateException("Gemini returned no text.")
            val jsonText = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            return JSONObject(jsonText)
        }
    }

    fun saveProject() {
        val topic = topicInput.value.trim()
        val title = titleInput.value.trim()
        if (topic.isBlank() || title.isBlank()) {
            viewModelScope.launch {
                _snackbarMessage.emit(
                    if (appLanguage.value == AppLanguage.PERSIAN) "لطفاً موضوع و عنوان ویدیو را وارد کنید" else "Please enter both topic and title"
                )
            }
            return
        }

        val fullScript = listOf(
            hookInput.value,
            introInput.value,
            bodyInput.value,
            ctaInput.value,
            outroInput.value
        ).filter { it.isNotBlank() }.joinToString("\n\n")

        viewModelScope.launch {
            val existingId = _editingProjectId.value
            val existing = if (existingId != null) projectDao.getProjectById(existingId) else null
            val contentChanged = existing != null && (
                existing.title != title ||
                    existing.topic != topic ||
                    existing.language != contentLang.value ||
                    existing.format != formatSelection.value.name ||
                    existing.targetDurationSec != durationSec.value ||
                    existing.tone != toneSelection.value.name ||
                    existing.scriptHook != hookInput.value ||
                    existing.scriptIntro != introInput.value ||
                    existing.scriptBody != bodyInput.value ||
                    existing.scriptCta != ctaInput.value ||
                    existing.scriptOutro != outroInput.value ||
                    existing.description != descriptionInput.value ||
                    existing.tags != tagsInput.value
                )

            val project = VideoProject(
                id = existingId ?: 0,
                title = title,
                topic = topic,
                language = contentLang.value,
                format = formatSelection.value.name,
                targetDurationSec = durationSec.value,
                tone = toneSelection.value.name,
                scriptHook = hookInput.value,
                scriptIntro = introInput.value,
                scriptBody = bodyInput.value,
                scriptCta = ctaInput.value,
                scriptOutro = outroInput.value,
                fullScript = fullScript,
                description = descriptionInput.value,
                tags = tagsInput.value,
                status = if (contentChanged) VideoStatus.DRAFT.name else (existing?.status ?: VideoStatus.DRAFT.name),
                voiceStyle = existing?.voiceStyle ?: VoiceStyle.MALE_DEEP.name,
                speechRate = existing?.speechRate ?: 1.0f,
                subtitleStyle = existing?.subtitleStyle ?: SubtitleStyle.BOLD_YELLOW.name,
                visualTheme = existing?.visualTheme ?: VisualTheme.CYBERPUNK.name,
                selectedImageUris = existing?.selectedImageUris ?: "",
                selectedAudioUri = existing?.selectedAudioUri,
                renderedVideoPath = if (contentChanged) null else existing?.renderedVideoPath,
                renderedVideoUri = if (contentChanged) null else existing?.renderedVideoUri,
                renderProgress = if (contentChanged) 0 else (existing?.renderProgress ?: 0),
                renderingError = if (contentChanged) null else existing?.renderingError,
                youtubeVideoId = existing?.youtubeVideoId,
                scheduledPublishTime = existing?.scheduledPublishTime,
                isUploaded = existing?.isUploaded ?: false,
                viewsCount = existing?.viewsCount ?: 0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val newId = projectDao.insertProject(project)
            val assignedId = if (existingId != null) existingId else newId
            _editingProjectId.value = assignedId
            _selectedProductionProjectId.value = assignedId

            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "پروژه در پایگاه داده محلی ذخیره شد" else "Project saved to local database"
            )
            navigateTo(AppScreen.DASHBOARD)
        }
    }

    fun deleteProject(project: VideoProject) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
            if (_selectedProductionProjectId.value == project.id) {
                _selectedProductionProjectId.value = null
            }
            if (_selectedPublishProjectId.value == project.id) {
                _selectedPublishProjectId.value = null
            }
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "پروژه با موفقیت حذف شد" else "Project deleted successfully"
            )
        }
    }

    // Production functions
    fun selectProjectForProduction(projectId: Long) {
        _selectedProductionProjectId.value = projectId
        viewModelScope.launch {
            val project = projectDao.getProjectById(projectId) ?: return@launch
            prodVoiceStyle.value = VoiceStyle.fromString(project.voiceStyle)
            prodSpeechRate.value = project.speechRate
            prodVisualTheme.value = VisualTheme.fromString(project.visualTheme)
            prodSubtitleStyle.value = SubtitleStyle.fromString(project.subtitleStyle)
            _renderProgress.value = project.renderProgress

            // Load saved media
            selectedImageUris.value = if (!project.selectedImageUris.isNullOrBlank()) {
                project.selectedImageUris.split(",").mapNotNull {
                    runCatching { android.net.Uri.parse(it.trim()) }.getOrNull()
                }
            } else emptyList()

            selectedAudioUri.value = project.selectedAudioUri?.let {
                runCatching { android.net.Uri.parse(it) }.getOrNull()
            }
            selectedAudioName.value = selectedAudioUri.value?.lastPathSegment

            renderedVideoUri.value = project.renderedVideoUri?.let {
                runCatching { android.net.Uri.parse(it) }.getOrNull()
            }
            renderedVideoFile.value = project.renderedVideoPath?.let {
                java.io.File(it).takeIf { f -> f.exists() }
            }
            renderingError.value = project.renderingError
        }
        navigateTo(AppScreen.PRODUCTION)
    }

    fun onImagesSelected(uris: List<android.net.Uri>) {
        val current = selectedImageUris.value.toMutableList()
        for (u in uris) {
            if (!current.contains(u)) current.add(u)
        }
        selectedImageUris.value = current
    }

    fun removeSelectedImage(uri: android.net.Uri) {
        selectedImageUris.value = selectedImageUris.value.filter { it != uri }
    }

    fun clearSelectedImages() {
        selectedImageUris.value = emptyList()
    }

    fun onAudioSelected(uri: android.net.Uri?, name: String? = null) {
        selectedAudioUri.value = uri
        selectedAudioName.value = name ?: uri?.lastPathSegment
    }

    fun startRenderPipeline() {
        val projectId = _selectedProductionProjectId.value ?: return
        if (_isRendering.value) return

        _isRendering.value = true
        _renderProgress.value = 0
        renderingError.value = null

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            try {
                val project = projectDao.getProjectById(projectId)
                if (project == null) {
                    _isRendering.value = false
                    return@launch
                }

                _renderStepMessage.value = if (appLanguage.value == AppLanguage.PERSIAN)
                    "آماده‌سازی لایه‌ها و آغاز رندر واقعی MP4..."
                else
                    "Initializing real MP4 rendering pipeline..."

                val renderer = com.example.video.VideoRenderer(getApplication())
                val result = renderer.renderVideo(
                    project = project.copy(
                        voiceStyle = prodVoiceStyle.value.name,
                        speechRate = prodSpeechRate.value,
                        visualTheme = prodVisualTheme.value.name,
                        subtitleStyle = prodSubtitleStyle.value.name
                    ),
                    imageUris = selectedImageUris.value,
                    audioUri = selectedAudioUri.value,
                    onProgress = { percent, msg ->
                        _renderProgress.value = percent
                        _renderStepMessage.value = msg
                    }
                )

                renderedVideoFile.value = result.file
                renderedVideoUri.value = result.uri

                // Update project in DB with real rendered MP4 path and URI
                val updated = project.copy(
                    status = VideoStatus.COMPLETED.name,
                    renderProgress = 100,
                    voiceStyle = prodVoiceStyle.value.name,
                    speechRate = prodSpeechRate.value,
                    visualTheme = prodVisualTheme.value.name,
                    subtitleStyle = prodSubtitleStyle.value.name,
                    selectedImageUris = selectedImageUris.value.joinToString(","),
                    selectedAudioUri = selectedAudioUri.value?.toString(),
                    renderedVideoPath = result.file.absolutePath,
                    renderedVideoUri = result.uri.toString(),
                    renderingError = null,
                    updatedAt = System.currentTimeMillis()
                )
                projectDao.updateProject(updated)

                _isRendering.value = false
                _snackbarMessage.emit(
                    if (appLanguage.value == AppLanguage.PERSIAN)
                        "ویدیوی واقعی MP4 با موفقیت تولید شد (${result.file.name})!"
                    else
                        "Real MP4 video rendered successfully (${result.file.name})!"
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Render error", e)
                renderingError.value = e.localizedMessage ?: e.toString()
                _isRendering.value = false
                _snackbarMessage.emit(
                    if (appLanguage.value == AppLanguage.PERSIAN)
                        "خطا در رندر ویدیو: ${e.localizedMessage}"
                    else
                        "Video rendering error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun exportVideoToDevice() {
        val file = renderedVideoFile.value
        viewModelScope.launch {
            if (file != null && file.exists()) {
                _snackbarMessage.emit(
                    if (appLanguage.value == AppLanguage.PERSIAN)
                        "فایل MP4 واقعی در مسیر ذخیره شده است: ${file.absolutePath}"
                    else
                        "Real MP4 video file stored at: ${file.absolutePath}"
                )
            } else {
                _snackbarMessage.emit(
                    if (appLanguage.value == AppLanguage.PERSIAN)
                        "ابتدا ویدیو را رندر کنید تا فایل MP4 ایجاد شود"
                    else
                        "Please render the video first to create the MP4 file"
                )
            }
        }
    }

    // Publishing functions
    fun selectProjectForPublishing(projectId: Long) {
        _selectedPublishProjectId.value = projectId
        navigateTo(AppScreen.PUBLISHING)
    }

    fun connectYouTubeOAuth(channelTitle: String = "AI Factory Studio", handle: String = "@AIFactoryOfficial") {
        // Never mark a channel as connected without completing Google's real OAuth flow.
        preferencesManager.setYouTubeConnected(false)
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN)
                    "اتصال واقعی یوتیوب هنوز پیکربندی نشده است؛ هیچ حسابی متصل نشد."
                else
                    "Real YouTube OAuth is not configured yet; no account was connected."
            )
        }
    }

    fun disconnectYouTube() {
        preferencesManager.setYouTubeConnected(false)
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "اتصال حساب یوتیوب قطع شد" else "YouTube account disconnected"
            )
        }
    }

    fun startYouTubeUpload() {
        // Upload is deliberately blocked until real OAuth + YouTube Data API upload are configured.
        _isUploading.value = false
        _uploadProgress.value = 0
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN)
                    "هیچ ویدیویی آپلود نشد. انتشار واقعی به OAuth گوگل و YouTube Data API نیاز دارد که هنوز پیکربندی نشده‌اند."
                else
                    "Nothing was uploaded. Real publishing requires configured Google OAuth and the YouTube Data API."
            )
        }
    }

    // Automation functions
    fun toggleAutomation(enabled: Boolean) {
        preferencesManager.setAutomationEnabled(enabled)
    }

    fun updateAutomationTime(time: String) {
        preferencesManager.setAutomationTime(time)
    }

    fun runAutomationTaskNow() {
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN)
                    "اجرای خودکار هنوز پیاده‌سازی نشده است؛ هیچ وظیفه یا ویدیویی به‌صورت ساختگی ایجاد نشد."
                else
                    "Background automation is not implemented yet; no fake task or video was created."
            )
        }
    }

    override fun onCleared() {
        geminiApiKey.value = ""
        geminiHttpClient.dispatcher.cancelAll()
        geminiHttpClient.connectionPool.evictAll()
        super.onCleared()
    }

    // Reset Data
    fun resetToSampleData() {
        viewModelScope.launch {
            projectDao.deleteAll()
            automationDao.deleteAll()
            projectDao.insertAll(SampleData.getInitialProjects())
            _selectedProductionProjectId.value = null
            _selectedPublishProjectId.value = null
            _editingProjectId.value = null
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "پروژه‌های نمونهٔ پیش‌نویس بازیابی شدند؛ وظایف ساختگی خودکار حذف شدند." else "Sample draft projects restored; simulated automation tasks were removed."
            )
            navigateTo(AppScreen.DASHBOARD)
        }
    }
}
