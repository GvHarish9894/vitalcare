package com.techgv.vitalcare.domain.repository

import com.techgv.vitalcare.domain.model.ThemePreference
import kotlinx.coroutines.flow.StateFlow

/**
 * Local app settings (F8): optional profile name (D-019), theme (FR-SE2), and
 * the telemetry opt-out (FR-SE5/D-029). Values persist via
 * multiplatform-settings; setters are synchronous key-value writes.
 */
interface SettingsRepository {
    val theme: StateFlow<ThemePreference>
    val profileName: StateFlow<String>
    val telemetryEnabled: StateFlow<Boolean>

    fun setTheme(value: ThemePreference)
    fun setProfileName(value: String)
    fun setTelemetryEnabled(value: Boolean)
}
