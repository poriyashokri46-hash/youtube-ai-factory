package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
                        lang = appLanguage
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
