package com.techgv.vitalcare.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.AppInfo
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.usecase.ExportCsv
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profileName: String = "",
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val telemetryEnabled: Boolean = true,
    val versionName: String = "",
    val isExporting: Boolean = false,
    val showExportScopeChooser: Boolean = false,
)

sealed interface SettingsEvent {
    data class ProfileNameChanged(val value: String) : SettingsEvent
    data class ThemeSelected(val value: ThemePreference) : SettingsEvent
    data class TelemetryToggled(val enabled: Boolean) : SettingsEvent
    data object ExportCsvClicked : SettingsEvent
    data class ExportScopeSelected(val scope: HistoryFilter) : SettingsEvent
    data object ExportChooserDismissed : SettingsEvent
}

sealed interface SettingsEffect {
    data object ExportEmpty : SettingsEffect
    data object ExportFailed : SettingsEffect
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val exportCsv: ExportCsv,
    appInfo: AppInfo,
) : ViewModel() {

    private data class ExportUi(
        val isExporting: Boolean = false,
        val showChooser: Boolean = false,
    )

    private val exportUi = MutableStateFlow(ExportUi())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.profileName,
        settingsRepository.theme,
        settingsRepository.telemetryEnabled,
        exportUi,
    ) { profileName, theme, telemetryEnabled, export ->
        SettingsUiState(
            profileName = profileName,
            theme = theme,
            telemetryEnabled = telemetryEnabled,
            versionName = appInfo.versionName,
            isExporting = export.isExporting,
            showExportScopeChooser = export.showChooser,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(versionName = appInfo.versionName),
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
        }
    }

    private fun export(scope: HistoryFilter) {
        if (exportUi.value.isExporting) return
        exportUi.update { it.copy(showChooser = false, isExporting = true) }
        viewModelScope.launch {
            when (val result = exportCsv(scope)) {
                is ExportCsv.Result.Exported -> Unit // share sheet is already up
                is ExportCsv.Result.Empty -> _effects.send(SettingsEffect.ExportEmpty)
                is ExportCsv.Result.Failed -> _effects.send(SettingsEffect.ExportFailed)
            }
            exportUi.update { it.copy(isExporting = false) }
        }
    }
}
