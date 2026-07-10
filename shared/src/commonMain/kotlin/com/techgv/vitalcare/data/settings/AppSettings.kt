package com.techgv.vitalcare.data.settings

import com.russhwolf.settings.Settings
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.model.VolumeUnit
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

    // Backup state (06 §6) — kept in settings, never per-record.
    private val _driveConnected = MutableStateFlow(settings.getBoolean(KEY_DRIVE_CONNECTED, false))
    val driveConnected: StateFlow<Boolean> = _driveConnected.asStateFlow()

    private val _lastBackupAt = MutableStateFlow(settings.getLong(KEY_LAST_BACKUP_AT, 0L))
    val lastBackupAt: StateFlow<Long> = _lastBackupAt.asStateFlow()

    private val _autoBackupCadence = MutableStateFlow(readCadence())
    val autoBackupCadence: StateFlow<AutoBackupCadence> = _autoBackupCadence.asStateFlow()

    // Fluid preferences (D-032) — display unit and daily intake goal (canonical mL).
    private val _volumeUnit = MutableStateFlow(readVolumeUnit())
    val volumeUnit: StateFlow<VolumeUnit> = _volumeUnit.asStateFlow()

    private val _dailyFluidGoalMl =
        MutableStateFlow(settings.getInt(KEY_FLUID_GOAL_ML, DEFAULT_FLUID_GOAL_ML))
    val dailyFluidGoalMl: StateFlow<Int> = _dailyFluidGoalMl.asStateFlow()

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

    fun setDriveConnected(value: Boolean) {
        settings.putBoolean(KEY_DRIVE_CONNECTED, value)
        _driveConnected.value = value
    }

    fun setLastBackupAt(value: Long) {
        settings.putLong(KEY_LAST_BACKUP_AT, value)
        _lastBackupAt.value = value
    }

    fun setAutoBackupCadence(value: AutoBackupCadence) {
        settings.putString(KEY_AUTO_BACKUP, value.name)
        _autoBackupCadence.value = value
    }

    fun setVolumeUnit(value: VolumeUnit) {
        settings.putString(KEY_VOLUME_UNIT, value.name)
        _volumeUnit.value = value
    }

    fun setDailyFluidGoalMl(value: Int) {
        settings.putInt(KEY_FLUID_GOAL_ML, value)
        _dailyFluidGoalMl.value = value
    }

    private fun readTheme(): ThemePreference {
        val raw = settings.getString(KEY_THEME, ThemePreference.SYSTEM.name)
        return ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM
    }

    private fun readCadence(): AutoBackupCadence {
        val raw = settings.getString(KEY_AUTO_BACKUP, AutoBackupCadence.OFF.name)
        return AutoBackupCadence.entries.firstOrNull { it.name == raw } ?: AutoBackupCadence.OFF
    }

    private fun readVolumeUnit(): VolumeUnit {
        val raw = settings.getString(KEY_VOLUME_UNIT, VolumeUnit.ML.name)
        return VolumeUnit.entries.firstOrNull { it.name == raw } ?: VolumeUnit.ML
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_PROFILE_NAME = "profile_name"
        const val KEY_TELEMETRY = "telemetry_enabled"
        const val KEY_DRIVE_CONNECTED = "drive_connected"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val KEY_AUTO_BACKUP = "auto_backup_cadence"
        const val KEY_VOLUME_UNIT = "volume_unit"
        const val KEY_FLUID_GOAL_ML = "daily_fluid_goal_ml"
        const val DEFAULT_FLUID_GOAL_ML = 2000
    }
}
