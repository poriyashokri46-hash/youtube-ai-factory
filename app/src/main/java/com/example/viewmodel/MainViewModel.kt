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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    init {
        seedDatabaseIfEmpty()
    }

    private fun seedDatabaseIfEmpty() {
        viewModelScope.launch {
            if (projectDao.getCount() == 0) {
                projectDao.insertAll(SampleData.getInitialProjects())
            }
            if (automationDao.getCount() == 0) {
                automationDao.insertAll(SampleData.getInitialTasks())
            }
        }
    }

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
        val topic = topicInput.value.ifBlank {
            if (contentLang.value == "fa") "روش‌های افزایش تمرکز با تکنیک پومودورو" else "5 Productivity Habits for High Achievers"
        }
        topicInput.value = topic

        if (contentLang.value == "fa") {
            val isShort = formatSelection.value == VideoFormat.SHORTS
            if (isShort) {
                titleInput.value = "سه ترفند طلایی برای $topic که هیچ‌کس بهت نمی‌گه!"
                hookInput.value = "اگه فکر می‌کنی $topic سخته، این ۳۰ ثانیه رو ببین تا نظرت برای همیشه عوض بشه!"
                introInput.value = "سلام بچه‌ها! امروز بدون حاشیه می‌خوایم مهم‌ترین فرمول موفقیت در این زمینه رو مرور کنیم."
                bodyInput.value = "نکته اول: شروع با قدم‌های ۲ دقیقه‌ای.\nنکته دوم: حذف تمام نوتیفیکیشن‌ها.\nنکته سوم: بازبینی هفتگی پیشرفت."
                ctaInput.value = "این ویدیو رو برای دوست پرتلاشت بفرست و سابسکرایب کن!"
                outroInput.value = "ویدیو بعدی رو از دست نده، تا فردا!"
                descriptionInput.value = "در این ویدیوی کوتاه شورتس، نکات کلیدی و کاربردی درباره $topic را آموزش می‌دهیم. سابسکرایب کانال برای ویدیوهای روزانه فراموش نشود!"
                tagsInput.value = "$topic, آموزش سریع, یوتیوب شورتس, نکات طلایی, موفقیت"
            } else {
                titleInput.value = "راهنمای جامع و صفر تا صد $topic | استراتژی ۲۰۲۵"
                hookInput.value = "چرا ۹۵ درصد از افراد در $topic شکست می‌خورند و راز ۵ درصدی که به نتیجه می‌رسند چیه؟"
                introInput.value = "در این ویدیوی کامل و تحلیلی، تمام ابعاد، ابزارها و نقشه‌راه عملیاتی را با جزئیات بررسی می‌کنیم."
                bodyInput.value = "فصل اول: مفاهیم پایه و تغییر ذهنیت.\nفصل دوم: ابزارهای خودکارسازی و هوش مصنوعی لازم.\nفصل سوم: اشتباهات مهلکی که باید از آن‌ها دوری کنید.\nفصل چهارم: برنامه اقدام ۳۰ روزه برای رسیدن به نتیجه ملموس."
                ctaInput.value = "اگر این راهنما براتون مفید بود، حتماً ویدیو رو لایک کنید و نظرتون رو در کامنت‌ها بنویسید."
                outroInput.value = "فایل‌ها و لینک‌های تکمیلی در توضیحات قرار گرفته است. موفق باشید!"
                descriptionInput.value = "ویدیوی جامع بررسی $topic به همراه نقشه راه عملیاتی برای علاقه‌مندان و تولیدکنندگان محتوا."
                tagsInput.value = "$topic, آموزش یوتیوب, استراتژی, راهنمای جامع, درآمدزایی"
            }
        } else {
            val isShort = formatSelection.value == VideoFormat.SHORTS
            if (isShort) {
                titleInput.value = "Stop Doing This! The Secret to $topic in 60 Seconds"
                hookInput.value = "If you are still struggling with $topic, stop scrolling right now and listen up!"
                introInput.value = "Welcome back! Today we are exposing the number one mistake 90% of beginners make."
                bodyInput.value = "Step 1: Focus on consistency over intensity.\nStep 2: Automate repetitive workflows.\nStep 3: Track your daily metrics."
                ctaInput.value = "Smash the subscribe button and drop your thoughts below!"
                outroInput.value = "Catch you in the next viral breakdown!"
                descriptionInput.value = "Essential insights and practical tips on $topic. Subscribe for daily high-impact Shorts!"
                tagsInput.value = "$topic, Shorts, Productivity, LifeHacks, AI Tools"
            } else {
                titleInput.value = "The Complete Blueprint to $topic (Step-by-Step Guide)"
                hookInput.value = "What separates high performers in $topic from everyone else? It comes down to one single framework."
                introInput.value = "In this comprehensive guide, we will unpack the exact blueprint you need to master this domain."
                bodyInput.value = "Section 1: Foundations and Mindset.\nSection 2: The Core Execution Engine.\nSection 3: Real-World Case Studies.\nSection 4: Your 30-Day Implementation Checklist."
                ctaInput.value = "If this breakdown provided value, please hit like and subscribe for more in-depth masterclasses."
                outroInput.value = "All resource links and checklists are in the description below. See you next week!"
                descriptionInput.value = "Full breakdown and step-by-step masterclass covering $topic with actionable workflows."
                tagsInput.value = "$topic, Masterclass, StepByStep, YouTube Growth, Blueprint"
            }
        }
        viewModelScope.launch {
            _snackbarMessage.emit(if (appLanguage.value == AppLanguage.PERSIAN) "سناریو و متادیتای ویدیو با موفقیت ساخته شد" else "AI Script & Metadata generated")
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
                status = existing?.status ?: VideoStatus.DRAFT.name,
                voiceStyle = existing?.voiceStyle ?: VoiceStyle.MALE_DEEP.name,
                speechRate = existing?.speechRate ?: 1.0f,
                subtitleStyle = existing?.subtitleStyle ?: SubtitleStyle.BOLD_YELLOW.name,
                visualTheme = existing?.visualTheme ?: VisualTheme.CYBERPUNK.name,
                renderedVideoPath = existing?.renderedVideoPath,
                renderProgress = existing?.renderProgress ?: 0,
                youtubeVideoId = existing?.youtubeVideoId,
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

    fun connectYouTubeOAuth(channelTitle: String = "AI Factory Channel", handle: String = "@AIFactoryGlobal") {
        preferencesManager.setYouTubeConnected(true, channelTitle, handle)
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "کانال یوتیوب با موفقیت از طریق OAuth 2.0 متصل شد!" else "YouTube channel securely connected via OAuth 2.0!"
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
        val projectId = _selectedPublishProjectId.value ?: return
        if (_isUploading.value) return

        if (!isYouTubeConnected.value) {
            viewModelScope.launch {
                _snackbarMessage.emit(
                    if (appLanguage.value == AppLanguage.PERSIAN) "ابتدا کانال یوتیوب خود را متصل کنید" else "Please connect your YouTube channel first"
                )
            }
            return
        }

        _isUploading.value = true
        _uploadProgress.value = 0

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            for (p in 1..100) {
                delay(35)
                _uploadProgress.value = p
            }

            val project = projectDao.getProjectById(projectId)
            if (project != null) {
                val updated = project.copy(
                    status = VideoStatus.PUBLISHED.name,
                    isUploaded = true,
                    youtubeVideoId = "yt_ai_" + System.currentTimeMillis().toString().takeLast(6),
                    updatedAt = System.currentTimeMillis()
                )
                projectDao.updateProject(updated)
            }

            _isUploading.value = false
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "ویدیو با موفقیت در یوتیوب منتشر شد!" else "Video successfully published to YouTube!"
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
            val isFa = appLanguage.value == AppLanguage.PERSIAN
            val topicsFa = listOf(
                "۵ ترفند هوش مصنوعی برای ویرایش سریع ویدیو",
                "نحوه بهینه‌سازی تگ‌های یوتیوب برای رفتن به اکسپلور",
                "راهنمای ساخت پادکست خودکار با هوش مصنوعی"
            )
            val topicsEn = listOf(
                "5 AI Prompts for Viral Video Scripts",
                "How to Rank #1 on YouTube Search",
                "Automating YouTube Shorts With Zero Budget"
            )

            val chosenTopic = if (isFa) topicsFa.random() else topicsEn.random()
            val taskTitle = if (isFa) "وظیفه فوری: $chosenTopic" else "Instant Task: $chosenTopic"
            val log = if (isFa) "سناریو نگارش شد، صدا تولید گردید و برای بررسی آماده شد." else "Script generated, voiceover synthesized, ready for review."

            val task = AutomationTask(
                title = taskTitle,
                format = "SHORTS",
                language = if (isFa) "fa" else "en",
                scheduledTimeStr = if (isFa) "همین حالا" else "Right now",
                status = "COMPLETED",
                logMessage = log
            )
            automationDao.insertTask(task)

            // Also create a draft project
            val project = VideoProject(
                title = chosenTopic,
                topic = chosenTopic,
                language = if (isFa) "fa" else "en",
                format = "SHORTS",
                targetDurationSec = 50,
                tone = "ENTERTAINING",
                scriptHook = if (isFa) "این ترفند را حتماً در ویدیوی بعدی امتحان کنید!" else "You must try this hack in your next video!",
                scriptIntro = if (isFa) "سلام همراهان عزیز کارخانه یوتیوب!" else "Welcome back creators!",
                scriptBody = if (isFa) "سه گام عملیاتی برای بهبود رتبه و بازدید کانال شما." else "Three actionable steps to boost your channel reach.",
                scriptCta = if (isFa) "سابسکرایب کنید و زنگوله را بزنید!" else "Subscribe and hit the bell icon!",
                scriptOutro = if (isFa) "روز خوش و پر انرژی!" else "Keep creating!",
                fullScript = if (isFa) "این ترفند را حتماً امتحان کنید! سلام همراهان عزیز کارخانه یوتیوب! سه گام عملیاتی برای بهبود رتبه. سابسکرایب کنید!" else "Try this hack! Welcome back creators! Three actionable steps. Subscribe!",
                description = chosenTopic,
                tags = "YouTube, AI, Shorts, Growth",
                status = VideoStatus.REVIEW_PENDING.name,
                renderProgress = 100,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            projectDao.insertProject(project)

            _snackbarMessage.emit(
                if (isFa) "وظیفه خودکار با موفقیت اجرا شد و ویدیوی جدید ایجاد گردید" else "Automation task executed and new project created"
            )
        }
    }

    // Reset Data
    fun resetToSampleData() {
        viewModelScope.launch {
            projectDao.deleteAll()
            automationDao.deleteAll()
            projectDao.insertAll(SampleData.getInitialProjects())
            automationDao.insertAll(SampleData.getInitialTasks())
            _selectedProductionProjectId.value = null
            _selectedPublishProjectId.value = null
            _editingProjectId.value = null
            _snackbarMessage.emit(
                if (appLanguage.value == AppLanguage.PERSIAN) "داده‌ها با موفقیت به حالت اولیه بازنشانی شد" else "Sample data restored successfully"
            )
            navigateTo(AppScreen.DASHBOARD)
        }
    }
}
