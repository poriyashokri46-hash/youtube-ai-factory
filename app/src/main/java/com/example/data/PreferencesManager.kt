package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("yt_ai_factory_prefs", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(
        if (prefs.getString("app_lang", "fa") == "en") AppLanguage.ENGLISH else AppLanguage.PERSIAN
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isYouTubeConnected = MutableStateFlow(loadYouTubeConnectionFlag())
    val isYouTubeConnected: StateFlow<Boolean> = _isYouTubeConnected.asStateFlow()

    private val _channelName = MutableStateFlow(prefs.getString("channel_name", "AI Factory Studio") ?: "AI Factory Studio")
    val channelName: StateFlow<String> = _channelName.asStateFlow()

    private val _channelHandle = MutableStateFlow(prefs.getString("channel_handle", "@AIFactoryOfficial") ?: "@AIFactoryOfficial")
    val channelHandle: StateFlow<String> = _channelHandle.asStateFlow()

    // Automation is not implemented yet; a fresh install must never imply it is running.
    private val _automationEnabled = MutableStateFlow(false)
    val automationEnabled: StateFlow<Boolean> = _automationEnabled.asStateFlow()

    private val _automationTime = MutableStateFlow(prefs.getString("auto_time", "18:30") ?: "18:30")
    val automationTime: StateFlow<String> = _automationTime.asStateFlow()

    private fun loadYouTubeConnectionFlag(): Boolean {
        // Earlier builds set this flag without completing OAuth. Clear that legacy fake state once.
        if (!prefs.getBoolean("oauth_truth_migration_v1", false)) {
            prefs.edit()
                .putBoolean("yt_connected", false)
                .putBoolean("oauth_truth_migration_v1", true)
                .apply()
            return false
        }
        return prefs.getBoolean("yt_connected", false)
    }

    fun hasLegacyTruthCleanupRun(): Boolean =
        prefs.getBoolean("legacy_truth_cleanup_v1", false)

    fun markLegacyTruthCleanupRun() {
        prefs.edit().putBoolean("legacy_truth_cleanup_v1", true).apply()
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("app_lang", language.code).apply()
        _appLanguage.value = language
    }

    fun toggleLanguage() {
        val next = if (_appLanguage.value == AppLanguage.PERSIAN) AppLanguage.ENGLISH else AppLanguage.PERSIAN
        setLanguage(next)
    }

    fun setYouTubeConnected(connected: Boolean, channel: String = "AI Factory Studio", handle: String = "@AIFactoryOfficial") {
        prefs.edit()
            .putBoolean("yt_connected", connected)
            .putString("channel_name", channel)
            .putString("channel_handle", handle)
            .apply()
        _isYouTubeConnected.value = connected
        _channelName.value = channel
        _channelHandle.value = handle
    }

    fun setAutomationEnabled(enabled: Boolean) {
        // Do not persist a misleading enabled state until a real scheduler is installed.
        prefs.edit().putBoolean("auto_enabled", false).apply()
        _automationEnabled.value = false
    }

    fun setAutomationTime(time: String) {
        prefs.edit().putString("auto_time", time).apply()
        _automationTime.value = time
    }
}
