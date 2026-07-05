package com.techgv.vitalcare.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppInfo
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.FullDateFormat
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.ReminderPreferences
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.reminders.ReminderPermission
import com.techgv.vitalcare.domain.reminders.ReminderPermissionMonitor
import com.techgv.vitalcare.domain.reminders.ReminderPermissionStatus
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.usecase.BackupNow
import com.techgv.vitalcare.domain.usecase.ConnectDrive
import com.techgv.vitalcare.domain.usecase.DisconnectDrive
import com.techgv.vitalcare.domain.usecase.ExportCsv
import com.techgv.vitalcare.domain.usecase.ObserveBackupStatus
import com.techgv.vitalcare.domain.usecase.RestoreFromDrive
import com.techgv.vitalcare.domain.usecase.SetAutoBackup
import com.techgv.vitalcare.domain.usecase.SetReminderPreferences
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class SettingsUiState(
    val profileName: String = "",
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val telemetryEnabled: Boolean = true,
    val versionName: String = "",
    val isExporting: Boolean = false,
    val showExportScopeChooser: Boolean = false,
    // Drive (F7) — available only when this build ships an OAuth client (D-027).
    val driveAvailable: Boolean = false,
    val driveConnected: Boolean = false,
    val lastBackupText: String? = null,
    val autoBackupCadence: AutoBackupCadence = AutoBackupCadence.OFF,
    val isConnectingDrive: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val showDisconnectConfirm: Boolean = false,
    // Reminders (D-032): toggle stores intent; blocked = on but no permission.
    val reminders: ReminderPreferences = ReminderPreferences(),
    val reminderPermissionBlocked: Boolean = false,
    val showReminderFromPicker: Boolean = false,
    val showReminderUntilPicker: Boolean = false,
)

sealed interface SettingsEvent {
    data class ProfileNameChanged(val value: String) : SettingsEvent
    data class ThemeSelected(val value: ThemePreference) : SettingsEvent
    data class TelemetryToggled(val enabled: Boolean) : SettingsEvent
    data object ExportCsvClicked : SettingsEvent
    data class ExportScopeSelected(val scope: HistoryFilter) : SettingsEvent
    data object ExportChooserDismissed : SettingsEvent
    data object ConnectDriveClicked : SettingsEvent
    data object BackupNowClicked : SettingsEvent
    data object RestoreClicked : SettingsEvent
    data class CadenceSelected(val cadence: AutoBackupCadence) : SettingsEvent
    data object DisconnectClicked : SettingsEvent
    data object DisconnectConfirmed : SettingsEvent
    data object DisconnectDismissed : SettingsEvent
    data class ReminderToggled(val enabled: Boolean) : SettingsEvent
    data class ReminderIntervalSelected(val hours: Int) : SettingsEvent
    data object ReminderFromClicked : SettingsEvent
    data object ReminderUntilClicked : SettingsEvent
    data class ReminderFromChanged(val time: LocalTime) : SettingsEvent
    data class ReminderUntilChanged(val time: LocalTime) : SettingsEvent
    data object ReminderPickerDismissed : SettingsEvent
    data class ReminderSkipToggled(val enabled: Boolean) : SettingsEvent
    data object OpenNotificationSettings : SettingsEvent
}

sealed interface SettingsEffect {
    data object ExportEmpty : SettingsEffect
    data object ExportFailed : SettingsEffect
    data object DriveConnectFailed : SettingsEffect
    data object BackupDone : SettingsEffect
    data object BackupFailed : SettingsEffect
    data class RestoreDone(val count: Int) : SettingsEffect
    data object RestoreUpToDate : SettingsEffect
    data object RestoreNoBackup : SettingsEffect
    data object RestoreUnsupported : SettingsEffect
    data object RestoreFailed : SettingsEffect
    data object ReminderPermissionDenied : SettingsEffect
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val exportCsv: ExportCsv,
    appInfo: AppInfo,
    driveAuthorizer: DriveAuthorizer,
    observeBackupStatus: ObserveBackupStatus,
    private val connectDrive: ConnectDrive,
    private val disconnectDrive: DisconnectDrive,
    private val backupNow: BackupNow,
    private val restoreFromDrive: RestoreFromDrive,
    private val setAutoBackup: SetAutoBackup,
    private val setReminderPreferences: SetReminderPreferences,
    private val reminderPermissionMonitor: ReminderPermissionMonitor,
    private val reminderPermission: ReminderPermission,
    private val timeZone: TimeZone,
) : ViewModel() {

    private data class ExportUi(
        val isExporting: Boolean = false,
        val showChooser: Boolean = false,
    )

    private data class DriveUi(
        val isConnecting: Boolean = false,
        val isBackingUp: Boolean = false,
        val isRestoring: Boolean = false,
        val showDisconnectConfirm: Boolean = false,
    )

    private data class ReminderPickers(
        val showFrom: Boolean = false,
        val showUntil: Boolean = false,
    )

    private val exportUi = MutableStateFlow(ExportUi())
    private val driveUi = MutableStateFlow(DriveUi())
    private val reminderPickers = MutableStateFlow(ReminderPickers())

    private val backupState = combine(
        observeBackupStatus(),
        settingsRepository.autoBackupCadence,
        driveUi,
    ) { status, cadence, ui -> Triple(status, cadence, ui) }

    private val reminderState = combine(
        settingsRepository.reminderPreferences,
        reminderPermissionMonitor.status,
        reminderPickers,
    ) { preferences, permission, pickers -> Triple(preferences, permission, pickers) }

    private val basics = combine(
        settingsRepository.profileName,
        settingsRepository.theme,
        settingsRepository.telemetryEnabled,
    ) { profileName, theme, telemetryEnabled -> Triple(profileName, theme, telemetryEnabled) }

    val uiState: StateFlow<SettingsUiState> = combine(
        basics,
        exportUi,
        backupState,
        reminderState,
    ) { (profileName, theme, telemetryEnabled), export, (status, cadence, drive),
        (reminders, permission, pickers) ->
        SettingsUiState(
            profileName = profileName,
            theme = theme,
            telemetryEnabled = telemetryEnabled,
            versionName = appInfo.versionName,
            isExporting = export.isExporting,
            showExportScopeChooser = export.showChooser,
            driveAvailable = driveAuthorizer.isAvailable,
            driveConnected = status.connected,
            lastBackupText = status.lastBackupAt.takeIf { it > 0 }?.let(::formatTimestamp),
            autoBackupCadence = cadence,
            isConnectingDrive = drive.isConnecting,
            isBackingUp = drive.isBackingUp,
            isRestoring = drive.isRestoring,
            showDisconnectConfirm = drive.showDisconnectConfirm,
            reminders = reminders,
            reminderPermissionBlocked =
                reminders.enabled && permission == ReminderPermissionStatus.DENIED,
            showReminderFromPicker = pickers.showFrom,
            showReminderUntilPicker = pickers.showUntil,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            versionName = appInfo.versionName,
            driveAvailable = driveAuthorizer.isAvailable,
        ),
    )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ProfileNameChanged ->
                settingsRepository.setProfileName(event.value.take(50))
            is SettingsEvent.ThemeSelected -> settingsRepository.setTheme(event.value)
            is SettingsEvent.TelemetryToggled ->
                settingsRepository.setTelemetryEnabled(event.enabled)
            SettingsEvent.ExportCsvClicked ->
                exportUi.update { it.copy(showChooser = true) }
            SettingsEvent.ExportChooserDismissed ->
                exportUi.update { it.copy(showChooser = false) }
            is SettingsEvent.ExportScopeSelected -> export(event.scope)
            SettingsEvent.ConnectDriveClicked -> connect()
            SettingsEvent.BackupNowClicked -> backup()
            SettingsEvent.RestoreClicked -> restore()
            is SettingsEvent.CadenceSelected -> setAutoBackup(event.cadence)
            SettingsEvent.DisconnectClicked ->
                driveUi.update { it.copy(showDisconnectConfirm = true) }
            SettingsEvent.DisconnectDismissed ->
                driveUi.update { it.copy(showDisconnectConfirm = false) }
            SettingsEvent.DisconnectConfirmed -> disconnect()
            is SettingsEvent.ReminderToggled ->
                updateReminders { it.copy(enabled = event.enabled) }
            is SettingsEvent.ReminderIntervalSelected ->
                updateReminders { it.copy(intervalHours = event.hours) }
            SettingsEvent.ReminderFromClicked ->
                reminderPickers.update { it.copy(showFrom = true) }
            SettingsEvent.ReminderUntilClicked ->
                reminderPickers.update { it.copy(showUntil = true) }
            is SettingsEvent.ReminderFromChanged -> {
                reminderPickers.update { it.copy(showFrom = false) }
                updateReminders { it.copy(activeFrom = event.time) }
            }
            is SettingsEvent.ReminderUntilChanged -> {
                reminderPickers.update { it.copy(showUntil = false) }
                updateReminders { it.copy(activeUntil = event.time) }
            }
            SettingsEvent.ReminderPickerDismissed ->
                reminderPickers.update { ReminderPickers() }
            is SettingsEvent.ReminderSkipToggled ->
                updateReminders { it.copy(skipIfRecorded = event.enabled) }
            SettingsEvent.OpenNotificationSettings -> reminderPermission.openSystemSettings()
        }
    }

    private fun updateReminders(transform: (ReminderPreferences) -> ReminderPreferences) {
        val updated = transform(settingsRepository.reminderPreferences.value)
        viewModelScope.launch {
            val result = setReminderPreferences(updated)
            if (result is SetReminderPreferences.Result.PermissionDenied) {
                _effects.send(SettingsEffect.ReminderPermissionDenied)
            }
        }
    }

    private fun export(scope: HistoryFilter) {
        if (exportUi.value.isExporting) return
        exportUi.update { it.copy(showChooser = false, isExporting = true) }
        viewModelScope.launch {
            when (exportCsv(scope)) {
                is ExportCsv.Result.Exported -> Unit // share sheet is already up
                is ExportCsv.Result.Empty -> _effects.send(SettingsEffect.ExportEmpty)
                is ExportCsv.Result.Failed -> _effects.send(SettingsEffect.ExportFailed)
            }
            exportUi.update { it.copy(isExporting = false) }
        }
    }

    private fun connect() {
        if (driveUi.value.isConnecting) return
        driveUi.update { it.copy(isConnecting = true) }
        viewModelScope.launch {
            if (connectDrive() is AppResult.Failure) {
                _effects.send(SettingsEffect.DriveConnectFailed)
            }
            driveUi.update { it.copy(isConnecting = false) }
        }
    }

    private fun backup() {
        if (driveUi.value.isBackingUp) return
        driveUi.update { it.copy(isBackingUp = true) }
        viewModelScope.launch {
            when (backupNow(interactive = true)) {
                is AppResult.Success -> _effects.send(SettingsEffect.BackupDone)
                is AppResult.Failure -> _effects.send(SettingsEffect.BackupFailed)
            }
            driveUi.update { it.copy(isBackingUp = false) }
        }
    }

    private fun restore() {
        if (driveUi.value.isRestoring) return
        driveUi.update { it.copy(isRestoring = true) }
        viewModelScope.launch {
            when (val result = restoreFromDrive()) {
                is AppResult.Success ->
                    if (result.value > 0) {
                        _effects.send(SettingsEffect.RestoreDone(result.value))
                    } else {
                        _effects.send(SettingsEffect.RestoreUpToDate)
                    }
                is AppResult.Failure -> when (result.error) {
                    is AppError.NotFound -> _effects.send(SettingsEffect.RestoreNoBackup)
                    is AppError.UnsupportedBackup ->
                        _effects.send(SettingsEffect.RestoreUnsupported)
                    else -> _effects.send(SettingsEffect.RestoreFailed)
                }
            }
            driveUi.update { it.copy(isRestoring = false) }
        }
    }

    private fun disconnect() {
        driveUi.update { it.copy(showDisconnectConfirm = false) }
        viewModelScope.launch { disconnectDrive() }
    }

    private fun formatTimestamp(epochMillis: Long): String {
        val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        return "${FullDateFormat.format(dateTime.date)}, ${TimeFormat.format(dateTime.time)}"
    }
}
