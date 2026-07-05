package com.techgv.vitalcare.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.ConfirmDialog
import com.techgv.vitalcare.core.designsystem.components.SectionHeader
import com.techgv.vitalcare.core.designsystem.components.VitalTextField
import com.techgv.vitalcare.core.util.AppLinks
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.ThemePreference
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.action_cancel
import vitalcare.shared.generated.resources.cadence_daily
import vitalcare.shared.generated.resources.cadence_monthly
import vitalcare.shared.generated.resources.cadence_off
import vitalcare.shared.generated.resources.cadence_weekly
import vitalcare.shared.generated.resources.disconnect_message
import vitalcare.shared.generated.resources.disconnect_title
import vitalcare.shared.generated.resources.export_empty
import vitalcare.shared.generated.resources.export_failed
import vitalcare.shared.generated.resources.export_scope_title
import vitalcare.shared.generated.resources.filter_all
import vitalcare.shared.generated.resources.filter_month
import vitalcare.shared.generated.resources.filter_today
import vitalcare.shared.generated.resources.filter_week
import vitalcare.shared.generated.resources.settings_about
import vitalcare.shared.generated.resources.settings_appearance
import vitalcare.shared.generated.resources.settings_backup_export
import vitalcare.shared.generated.resources.settings_drive_auto_backup
import vitalcare.shared.generated.resources.settings_drive_backing_up
import vitalcare.shared.generated.resources.settings_drive_backup_now
import vitalcare.shared.generated.resources.settings_drive_connect
import vitalcare.shared.generated.resources.settings_drive_connect_subtitle
import vitalcare.shared.generated.resources.settings_drive_disconnect
import vitalcare.shared.generated.resources.settings_drive_last_backup
import vitalcare.shared.generated.resources.settings_drive_never_backed_up
import vitalcare.shared.generated.resources.settings_drive_not_configured
import vitalcare.shared.generated.resources.settings_drive_restore
import vitalcare.shared.generated.resources.settings_drive_restore_note
import vitalcare.shared.generated.resources.settings_drive_restoring
import vitalcare.shared.generated.resources.snackbar_backup_done
import vitalcare.shared.generated.resources.snackbar_backup_failed
import vitalcare.shared.generated.resources.snackbar_drive_connect_failed
import vitalcare.shared.generated.resources.snackbar_restore_done
import vitalcare.shared.generated.resources.snackbar_restore_no_backup
import vitalcare.shared.generated.resources.snackbar_restore_unsupported
import vitalcare.shared.generated.resources.snackbar_restore_up_to_date
import vitalcare.shared.generated.resources.error_generic
import vitalcare.shared.generated.resources.settings_export_csv
import vitalcare.shared.generated.resources.settings_export_csv_subtitle
import vitalcare.shared.generated.resources.settings_google_drive
import vitalcare.shared.generated.resources.settings_privacy
import vitalcare.shared.generated.resources.settings_privacy_note
import vitalcare.shared.generated.resources.settings_privacy_toggle
import vitalcare.shared.generated.resources.settings_profile
import vitalcare.shared.generated.resources.settings_profile_name
import vitalcare.shared.generated.resources.settings_source
import vitalcare.shared.generated.resources.settings_theme
import vitalcare.shared.generated.resources.settings_title
import vitalcare.shared.generated.resources.settings_version
import vitalcare.shared.generated.resources.theme_dark
import vitalcare.shared.generated.resources.theme_light
import vitalcare.shared.generated.resources.theme_system

@Composable
fun SettingsScreen(showSnackbar: (String) -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportEmpty = stringResource(Res.string.export_empty)
    val exportFailed = stringResource(Res.string.export_failed)
    val connectFailed = stringResource(Res.string.snackbar_drive_connect_failed)
    val backupDone = stringResource(Res.string.snackbar_backup_done)
    val backupFailed = stringResource(Res.string.snackbar_backup_failed)
    val restoreUpToDate = stringResource(Res.string.snackbar_restore_up_to_date)
    val restoreNoBackup = stringResource(Res.string.snackbar_restore_no_backup)
    val restoreUnsupported = stringResource(Res.string.snackbar_restore_unsupported)
    val restoreDoneTemplate = stringResource(Res.string.snackbar_restore_done, "%COUNT%")
    val genericError = stringResource(Res.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.ExportEmpty -> showSnackbar(exportEmpty)
                SettingsEffect.ExportFailed -> showSnackbar(exportFailed)
                SettingsEffect.DriveConnectFailed -> showSnackbar(connectFailed)
                SettingsEffect.BackupDone -> showSnackbar(backupDone)
                SettingsEffect.BackupFailed -> showSnackbar(backupFailed)
                is SettingsEffect.RestoreDone ->
                    showSnackbar(restoreDoneTemplate.replace("%COUNT%", effect.count.toString()))
                SettingsEffect.RestoreUpToDate -> showSnackbar(restoreUpToDate)
                SettingsEffect.RestoreNoBackup -> showSnackbar(restoreNoBackup)
                SettingsEffect.RestoreUnsupported -> showSnackbar(restoreUnsupported)
                SettingsEffect.RestoreFailed -> showSnackbar(genericError)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 10.dp),
        )

        SectionHeader(stringResource(Res.string.settings_profile))
        VitalTextField(
            value = uiState.profileName,
            onValueChange = { viewModel.onEvent(SettingsEvent.ProfileNameChanged(it)) },
            label = stringResource(Res.string.settings_profile_name),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader(stringResource(Res.string.settings_appearance))
        ThemeSelector(
            selected = uiState.theme,
            onSelect = { viewModel.onEvent(SettingsEvent.ThemeSelected(it)) },
        )

        SectionHeader(stringResource(Res.string.settings_backup_export))
        SettingsCard {
            SettingsRow(
                icon = Icons.Rounded.Download,
                title = stringResource(Res.string.settings_export_csv),
                subtitle = stringResource(Res.string.settings_export_csv_subtitle),
                enabled = !uiState.isExporting,
                onClick = { viewModel.onEvent(SettingsEvent.ExportCsvClicked) },
            )
            DriveSection(uiState = uiState, onEvent = viewModel::onEvent)
        }

        SectionHeader(stringResource(Res.string.settings_privacy))
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_privacy_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(Res.string.settings_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = uiState.telemetryEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.TelemetryToggled(it)) },
                )
            }
        }

        SectionHeader(stringResource(Res.string.settings_about))
        val uriHandler = LocalUriHandler.current
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_version),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = uiState.versionName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                onClick = { uriHandler.openUri(AppLinks.REPOSITORY) },
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_source),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = AppLinks.REPOSITORY.removePrefix("https://"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (uiState.showExportScopeChooser) {
        ExportScopeDialog(
            onSelect = { viewModel.onEvent(SettingsEvent.ExportScopeSelected(it)) },
            onDismiss = { viewModel.onEvent(SettingsEvent.ExportChooserDismissed) },
        )
    }
    if (uiState.showDisconnectConfirm) {
        ConfirmDialog(
            title = stringResource(Res.string.disconnect_title),
            message = stringResource(Res.string.disconnect_message),
            confirmLabel = stringResource(Res.string.settings_drive_disconnect),
            dismissLabel = stringResource(Res.string.action_cancel),
            onConfirm = { viewModel.onEvent(SettingsEvent.DisconnectConfirmed) },
            onDismiss = { viewModel.onEvent(SettingsEvent.DisconnectDismissed) },
            destructive = true,
        )
    }
}

/**
 * Drive subsection states (03 §3.10): not configured in this build → disabled
 * row (D-027); configured but not connected → connect row; connected → backup
 * now / restore / auto-backup cadence / disconnect.
 */
@Composable
private fun DriveSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    when {
        !uiState.driveAvailable -> SettingsRow(
            icon = Icons.Rounded.CloudUpload,
            title = stringResource(Res.string.settings_google_drive),
            subtitle = stringResource(Res.string.settings_drive_not_configured),
            enabled = false,
            onClick = null,
        )
        !uiState.driveConnected -> SettingsRow(
            icon = Icons.Rounded.CloudUpload,
            title = stringResource(Res.string.settings_drive_connect),
            subtitle = stringResource(Res.string.settings_drive_connect_subtitle),
            enabled = !uiState.isConnectingDrive,
            onClick = { onEvent(SettingsEvent.ConnectDriveClicked) },
        )
        else -> {
            SettingsRow(
                icon = Icons.Rounded.CloudUpload,
                title = stringResource(Res.string.settings_drive_backup_now),
                subtitle = if (uiState.isBackingUp) {
                    stringResource(Res.string.settings_drive_backing_up)
                } else {
                    uiState.lastBackupText
                        ?.let { stringResource(Res.string.settings_drive_last_backup, it) }
                        ?: stringResource(Res.string.settings_drive_never_backed_up)
                },
                enabled = !uiState.isBackingUp,
                onClick = { onEvent(SettingsEvent.BackupNowClicked) },
            )
            SettingsRow(
                icon = Icons.Rounded.Restore,
                title = stringResource(Res.string.settings_drive_restore),
                subtitle = if (uiState.isRestoring) {
                    stringResource(Res.string.settings_drive_restoring)
                } else {
                    stringResource(Res.string.settings_drive_restore_note)
                },
                enabled = !uiState.isRestoring,
                onClick = { onEvent(SettingsEvent.RestoreClicked) },
            )
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(Res.string.settings_drive_auto_backup),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AutoBackupCadence.entries.forEach { cadence ->
                        FilterChip(
                            selected = cadence == uiState.autoBackupCadence,
                            onClick = { onEvent(SettingsEvent.CadenceSelected(cadence)) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            label = {
                                Text(
                                    text = stringResource(
                                        when (cadence) {
                                            AutoBackupCadence.OFF -> Res.string.cadence_off
                                            AutoBackupCadence.DAILY -> Res.string.cadence_daily
                                            AutoBackupCadence.WEEKLY -> Res.string.cadence_weekly
                                            AutoBackupCadence.MONTHLY -> Res.string.cadence_monthly
                                        },
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }
            SettingsRow(
                icon = Icons.Rounded.LinkOff,
                title = stringResource(Res.string.settings_drive_disconnect),
                subtitle = stringResource(Res.string.settings_google_drive),
                enabled = true,
                onClick = { onEvent(SettingsEvent.DisconnectClicked) },
            )
        }
    }
}

@Composable
private fun ThemeSelector(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    Column {
        Text(
            text = stringResource(Res.string.settings_theme),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.entries.forEach { theme ->
                FilterChip(
                    selected = theme == selected,
                    onClick = { onSelect(theme) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    label = {
                        Text(
                            text = stringResource(
                                when (theme) {
                                    ThemePreference.SYSTEM -> Res.string.theme_system
                                    ThemePreference.LIGHT -> Res.string.theme_light
                                    ThemePreference.DARK -> Res.string.theme_dark
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: (() -> Unit)?,
) {
    val alpha = if (enabled) 1f else 0.45f
    Surface(
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
private fun ExportScopeDialog(
    onSelect: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(Res.string.export_scope_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                HistoryFilter.entries.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = false, onClick = { onSelect(filter) })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = false, onClick = { onSelect(filter) })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                when (filter) {
                                    HistoryFilter.ALL -> Res.string.filter_all
                                    HistoryFilter.TODAY -> Res.string.filter_today
                                    HistoryFilter.WEEK -> Res.string.filter_week
                                    HistoryFilter.MONTH -> Res.string.filter_month
                                },
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
