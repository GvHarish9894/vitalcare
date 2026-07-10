package com.techgv.vitalcare.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.BentoTile
import com.techgv.vitalcare.core.designsystem.components.PrimaryButton
import com.techgv.vitalcare.core.designsystem.components.SecondaryButton
import com.techgv.vitalcare.core.designsystem.components.VitalValueDisplay
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme
import com.techgv.vitalcare.core.util.DashboardDateFormat
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.domain.model.FluidDayBalance
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.model.isBloodPressureOutOfRange
import com.techgv.vitalcare.domain.model.isHeartRateOutOfRange
import com.techgv.vitalcare.domain.model.isSpo2OutOfRange
import com.techgv.vitalcare.feature.fluids.label
import com.techgv.vitalcare.feature.fluids.signedAmount
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.app_name
import vitalcare.shared.generated.resources.dashboard_backed_up_days_ago
import vitalcare.shared.generated.resources.dashboard_backed_up_today
import vitalcare.shared.generated.resources.dashboard_empty_title
import vitalcare.shared.generated.resources.dashboard_fluid_empty
import vitalcare.shared.generated.resources.dashboard_fluid_title
import vitalcare.shared.generated.resources.dashboard_unbacked_changes
import vitalcare.shared.generated.resources.fluid_intake
import vitalcare.shared.generated.resources.fluid_net
import vitalcare.shared.generated.resources.fluid_output
import vitalcare.shared.generated.resources.reminder_permission_blocked_action
import vitalcare.shared.generated.resources.reminder_permission_blocked_title
import vitalcare.shared.generated.resources.dashboard_latest_reading
import vitalcare.shared.generated.resources.dashboard_reading_at
import vitalcare.shared.generated.resources.dashboard_reading_count_one
import vitalcare.shared.generated.resources.dashboard_readings_count
import vitalcare.shared.generated.resources.dashboard_record_now
import vitalcare.shared.generated.resources.dashboard_record_vitals
import vitalcare.shared.generated.resources.dashboard_today
import vitalcare.shared.generated.resources.unit_bpm
import vitalcare.shared.generated.resources.unit_mmhg
import vitalcare.shared.generated.resources.unit_percent
import vitalcare.shared.generated.resources.vital_blood_pressure
import vitalcare.shared.generated.resources.vital_heart_rate
import vitalcare.shared.generated.resources.vital_spo2

@Composable
fun DashboardScreen(
    onRecordVitals: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFluids: () -> Unit,
) {
    val viewModel = koinViewModel<DashboardViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = DashboardDateFormat.format(uiState.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.reminderPermissionBlocked) {
            ReminderPermissionBanner(onClick = viewModel::openNotificationSettings)
        }

        LatestReadingTile(latest = uiState.latest, onRecordNow = onRecordVitals)
        TodayTile(count = uiState.count, times = uiState.times, onClick = onOpenHistory)
        FluidBalanceTile(
            balance = uiState.fluidBalance,
            unit = uiState.volumeUnit,
            onClick = onOpenFluids,
        )

        PrimaryButton(
            text = stringResource(Res.string.dashboard_record_vitals),
            onClick = onRecordVitals,
            modifier = Modifier.fillMaxWidth(),
        )
        BackupHintLine(hint = uiState.backupHint, onOpenSettings = onOpenSettings)
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Shown ONLY when reminders are on but notifications are blocked at the OS
 * level (D-032) — tapping opens the app's system notification settings.
 */
@Composable
private fun ReminderPermissionBanner(onClick: () -> Unit) {
    BentoTile(
        tint = VitalCareTheme.colors.tintPeach,
        icon = Icons.Rounded.NotificationsOff,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(Res.string.reminder_permission_blocked_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.reminder_permission_blocked_action),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Subtle, passive backup line — only when Drive is connected (FR-D3, 03 §6). */
@Composable
private fun BackupHintLine(hint: BackupHint?, onOpenSettings: () -> Unit) {
    when (hint) {
        null -> Unit
        is BackupHint.Pending -> Text(
            text = stringResource(Res.string.dashboard_unbacked_changes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onOpenSettings),
        )
        is BackupHint.BackedUp -> Text(
            text = if (hint.daysAgo == 0) {
                stringResource(Res.string.dashboard_backed_up_today)
            } else {
                stringResource(Res.string.dashboard_backed_up_days_ago, hint.daysAgo)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LatestReadingTile(latest: VitalRecord?, onRecordNow: () -> Unit) {
    BentoTile(
        tint = VitalCareTheme.colors.tintSage,
        hero = true,
        icon = Icons.Rounded.MonitorHeart,
    ) {
        if (latest == null) {
            Text(
                text = stringResource(Res.string.dashboard_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(Res.string.dashboard_record_now),
                onClick = onRecordNow,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.dashboard_latest_reading),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(
                        Res.string.dashboard_reading_at,
                        TimeFormat.format(latest.time),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            // Bento arrangement: two values side by side, wide BP below (03 §4.4).
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                VitalValueDisplay(
                    value = latest.spo2?.toString() ?: "—",
                    unit = stringResource(Res.string.unit_percent),
                    label = stringResource(Res.string.vital_spo2),
                    outOfRange = latest.isSpo2OutOfRange(),
                    modifier = Modifier.weight(1f),
                )
                VitalValueDisplay(
                    value = latest.heartRate?.toString() ?: "—",
                    unit = stringResource(Res.string.unit_bpm),
                    label = stringResource(Res.string.vital_heart_rate),
                    outOfRange = latest.isHeartRateOutOfRange(),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            VitalValueDisplay(
                value = if (latest.systolic != null && latest.diastolic != null) {
                    "${latest.systolic}/${latest.diastolic}"
                } else {
                    "—"
                },
                unit = stringResource(Res.string.unit_mmhg),
                label = stringResource(Res.string.vital_blood_pressure),
                outOfRange = latest.isBloodPressureOutOfRange(),
            )
        }
    }
}

@Composable
private fun FluidBalanceTile(
    balance: FluidDayBalance?,
    unit: VolumeUnit,
    onClick: () -> Unit,
) {
    BentoTile(
        tint = VitalCareTheme.colors.tintBlue,
        icon = Icons.Rounded.WaterDrop,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(Res.string.dashboard_fluid_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        if (balance == null || balance.isEmpty) {
            Text(
                text = stringResource(Res.string.dashboard_fluid_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VitalValueDisplay(
                    value = unit.format(balance.intakeMl),
                    unit = unit.label(),
                    label = stringResource(Res.string.fluid_intake),
                    modifier = Modifier.weight(1f),
                )
                VitalValueDisplay(
                    value = unit.format(balance.outputMl),
                    unit = unit.label(),
                    label = stringResource(Res.string.fluid_output),
                    modifier = Modifier.weight(1f),
                )
                VitalValueDisplay(
                    value = signedAmount(balance.netMl, unit),
                    unit = unit.label(),
                    label = stringResource(Res.string.fluid_net),
                    modifier = Modifier.weight(1f),
                )
            }
            if (balance.goalMl > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { balance.goalProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TodayTile(count: Int, times: List<LocalTime>, onClick: () -> Unit) {
    BentoTile(
        tint = VitalCareTheme.colors.tintCream,
        icon = Icons.Rounded.History,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(Res.string.dashboard_today),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (count == 1) {
                stringResource(Res.string.dashboard_reading_count_one)
            } else {
                stringResource(Res.string.dashboard_readings_count, count)
            },
            style = MaterialTheme.typography.displaySmall,
        )
        if (times.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = times.joinToString(" · ") { TimeFormat.format(it) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
