package com.techgv.vitalcare.domain.repository

import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.ReminderPreferences
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.model.VolumeUnit
import kotlinx.coroutines.flow.StateFlow

/**
 * Local app settings (F8): optional profile name (D-019), theme (FR-SE2),
 * the telemetry opt-out (FR-SE5/D-029), and backup state (06 §6 — kept in
 * settings, never per-record). Values persist via multiplatform-settings;
 * setters are synchronous key-value writes.
 */
interface SettingsRepository {
    val theme: StateFlow<ThemePreference>
    val profileName: StateFlow<String>
    val telemetryEnabled: StateFlow<Boolean>

    val driveConnected: StateFlow<Boolean>
    /** Epoch millis of the last successful backup; 0 = never. */
    val lastBackupAt: StateFlow<Long>
    val autoBackupCadence: StateFlow<AutoBackupCadence>

    /** Fluid display unit and daily intake goal in canonical mL (FR-SE6, D-033). */
    val volumeUnit: StateFlow<VolumeUnit>
    val dailyFluidGoalMl: StateFlow<Int>
    /** Vitals reminder intent (D-032) — scheduling is additionally permission-gated. */
    val reminderPreferences: StateFlow<ReminderPreferences>

    fun setTheme(value: ThemePreference)
    fun setProfileName(value: String)
    fun setTelemetryEnabled(value: Boolean)

    fun setDriveConnected(value: Boolean)
    fun setLastBackupAt(value: Long)
    fun setAutoBackupCadence(value: AutoBackupCadence)

    fun setVolumeUnit(value: VolumeUnit)
    fun setDailyFluidGoalMl(value: Int)
    fun setReminderPreferences(value: ReminderPreferences)
}
