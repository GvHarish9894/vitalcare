package com.techgv.vitalcare.data.settings

import com.russhwolf.settings.Settings
import com.techgv.vitalcare.domain.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * multiplatform-settings wrapper. Each key is exposed as an in-memory
 * StateFlow seeded from disk with write-through persistence, so the UI can
 * observe changes without the observable-settings artifact.
 */
class AppSettings(private val settings: Settings) {

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemePreference> = _theme.asStateFlow()

    private val _profileName = MutableStateFlow(settings.getString(KEY_PROFILE_NAME, ""))
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _telemetryEnabled = MutableStateFlow(settings.getBoolean(KEY_TELEMETRY, true))
    val telemetryEnabled: StateFlow<Boolean> = _telemetryEnabled.asStateFlow()

    fun setTheme(value: ThemePreference) {
        settings.putString(KEY_THEME, value.name)
        _theme.value = value
    }

    fun setProfileName(value: String) {
        settings.putString(KEY_PROFILE_NAME, value)
        _profileName.value = value
    }

    fun setTelemetryEnabled(value: Boolean) {
        settings.putBoolean(KEY_TELEMETRY, value)
        _telemetryEnabled.value = value
    }

    private fun readTheme(): ThemePreference {
        val raw = settings.getString(KEY_THEME, ThemePreference.SYSTEM.name)
        return ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_PROFILE_NAME = "profile_name"
        const val KEY_TELEMETRY = "telemetry_enabled"
    }
}
