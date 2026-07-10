package com.techgv.vitalcare.data.repository

import com.techgv.vitalcare.core.telemetry.Telemetry
import com.techgv.vitalcare.data.settings.AppSettings
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.model.VolumeUnit
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
    override val driveConnected: StateFlow<Boolean> = appSettings.driveConnected
    override val lastBackupAt: StateFlow<Long> = appSettings.lastBackupAt
    override val autoBackupCadence: StateFlow<AutoBackupCadence> = appSettings.autoBackupCadence
    override val volumeUnit: StateFlow<VolumeUnit> = appSettings.volumeUnit
    override val dailyFluidGoalMl: StateFlow<Int> = appSettings.dailyFluidGoalMl

    override fun setTheme(value: ThemePreference) = appSettings.setTheme(value)

    override fun setProfileName(value: String) = appSettings.setProfileName(value)

    override fun setTelemetryEnabled(value: Boolean) {
        appSettings.setTelemetryEnabled(value)
        telemetry.setEnabled(value)
    }

    override fun setDriveConnected(value: Boolean) = appSettings.setDriveConnected(value)

    override fun setLastBackupAt(value: Long) = appSettings.setLastBackupAt(value)

    override fun setAutoBackupCadence(value: AutoBackupCadence) =
        appSettings.setAutoBackupCadence(value)

    override fun setVolumeUnit(value: VolumeUnit) = appSettings.setVolumeUnit(value)

    override fun setDailyFluidGoalMl(value: Int) = appSettings.setDailyFluidGoalMl(value)
}
