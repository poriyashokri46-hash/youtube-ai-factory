package com.example

import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.example.localization.AppLanguage
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.TopHeader
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.ContentStudioScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PublishingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoProductionScreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }

    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching {
                authorizationClient.getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { authorizationResult ->
                viewModel.completeYouTubeAuthorization(authorizationResult.accessToken.orEmpty())
            }.onFailure { error ->
                viewModel.reportYouTubeAuthorizationError(error.localizedMessage ?: "Google authorization failed.")
            }
        } else {
            viewModel.reportYouTubeAuthorizationError("Google authorization was cancelled.")
        }
    }

    val startYouTubeAuthorization = {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/youtube.upload"),
                    Scope("https://www.googleapis.com/auth/youtube.readonly"),
                    Scope("https://www.googleapis.com/auth/yt-analytics.readonly")
                )
            )
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    if (pendingIntent != null) {
                        authorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } else {
                        viewModel.reportYouTubeAuthorizationError("Google requested authorization but did not provide a consent screen.")
                    }
                } else {
                    viewModel.completeYouTubeAuthorization(authorizationResult.accessToken.orEmpty())
                }
            }
            .addOnFailureListener { error ->
                viewModel.reportYouTubeAuthorizationError(error.localizedMessage ?: "Google authorization is unavailable.")
            }
    }

    // Listen for snackbar alerts
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Hardware and gesture Back button handler
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.handleBack()
    }

    // Dynamic RTL/LTR layout based on active language
    val layoutDirection = if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy),
            containerColor = DeepNavy,
            topBar = {
                TopHeader(
                    currentLang = appLanguage,
                    isYouTubeConnected = isYouTubeConnected,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onOpenAutomation = { viewModel.navigateTo(AppScreen.AUTOMATION) },
                    onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    onOpenPublishing = { viewModel.navigateTo(AppScreen.PUBLISHING) }
                )
            },
            bottomBar = {
                AppBottomNavigation(
                    currentScreen = currentScreen,
                    lang = appLanguage,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DeepNavy)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        projects = projects,
                        tasks = tasks,
                        lang = appLanguage,
                        onStartNewVideo = { viewModel.startNewProject() },
                        onEditProject = { viewModel.editProject(it) },
                        onProduceProject = { viewModel.selectProjectForProduction(it) },
                        onPublishProject = { viewModel.selectProjectForPublishing(it) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onOpenAutomation = { viewModel.navigateTo(AppScreen.AUTOMATION) }
                    )
                    AppScreen.CONTENT_STUDIO -> ContentStudioScreen(
                        viewModel = viewModel,
                        lang = appLanguage
                    )
                    AppScreen.PRODUCTION -> VideoProductionScreen(
                        viewModel = viewModel,
                        projects = projects,
                        lang = appLanguage
                    )
                    AppScreen.PUBLISHING -> PublishingScreen(
                        viewModel = viewModel,
                        projects = projects,
                        lang = appLanguage,
                        onConnectYouTube = { startYouTubeAuthorization() }
                    )
                    AppScreen.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel,
                        projects = projects,
                        lang = appLanguage
                    )
                    AppScreen.AUTOMATION -> AutomationScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        lang = appLanguage
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        lang = appLanguage
                    )
                }
            }
        }
    }
}
