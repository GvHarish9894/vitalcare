package com.techgv.vitalcare

import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.ReminderPreferences
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Reusable in-memory [SettingsRepository] for tests. */
class FakeSettingsRepository : SettingsRepository {
    override val theme = MutableStateFlow(ThemePreference.SYSTEM)
    override val profileName = MutableStateFlow("")
    override val telemetryEnabled = MutableStateFlow(true)
    override val driveConnected = MutableStateFlow(false)
    override val lastBackupAt = MutableStateFlow(0L)
    override val autoBackupCadence = MutableStateFlow(AutoBackupCadence.OFF)
    override val volumeUnit = MutableStateFlow(VolumeUnit.ML)
    override val dailyFluidGoalMl = MutableStateFlow(2000)
    override val reminderPreferences = MutableStateFlow(ReminderPreferences())

    override fun setTheme(value: ThemePreference) { theme.value = value }
    override fun setProfileName(value: String) { profileName.value = value }
    override fun setTelemetryEnabled(value: Boolean) { telemetryEnabled.value = value }
    override fun setDriveConnected(value: Boolean) { driveConnected.value = value }
    override fun setLastBackupAt(value: Long) { lastBackupAt.value = value }
    override fun setAutoBackupCadence(value: AutoBackupCadence) { autoBackupCadence.value = value }
    override fun setVolumeUnit(value: VolumeUnit) { volumeUnit.value = value }
    override fun setDailyFluidGoalMl(value: Int) { dailyFluidGoalMl.value = value }
    override fun setReminderPreferences(value: ReminderPreferences) {
        reminderPreferences.value = value
    }
}
