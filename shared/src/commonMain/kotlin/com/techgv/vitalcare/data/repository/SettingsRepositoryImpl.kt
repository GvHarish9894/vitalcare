package com.techgv.vitalcare.data.repository

import com.techgv.vitalcare.core.telemetry.Telemetry
import com.techgv.vitalcare.data.settings.AppSettings
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsRepositoryImpl(
    private val appSettings: AppSettings,
    private val telemetry: Telemetry,
) : SettingsRepository {

    init {
        // Honor the stored opt-out from the first session frame (D-029).
        telemetry.setEnabled(appSettings.telemetryEnabled.value)
    }

    override val theme: StateFlow<ThemePreference> = appSettings.theme
    override val profileName: StateFlow<String> = appSettings.profileName
    override val telemetryEnabled: StateFlow<Boolean> = appSettings.telemetryEnabled

    override fun setTheme(value: ThemePreference) = appSettings.setTheme(value)

    override fun setProfileName(value: String) = appSettings.setProfileName(value)

    override fun setTelemetryEnabled(value: Boolean) {
        appSettings.setTelemetryEnabled(value)
        telemetry.setEnabled(value)
    }
}
