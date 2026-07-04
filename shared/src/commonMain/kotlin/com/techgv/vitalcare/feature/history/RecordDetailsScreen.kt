package com.techgv.vitalcare.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.BentoTile
import com.techgv.vitalcare.core.designsystem.components.CircleIconButton
import com.techgv.vitalcare.core.designsystem.components.ConfirmDialog
import com.techgv.vitalcare.core.designsystem.components.VitalValueDisplay
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme
import com.techgv.vitalcare.core.util.FullDateFormat
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.model.isBloodPressureOutOfRange
import com.techgv.vitalcare.domain.model.isHeartRateOutOfRange
import com.techgv.vitalcare.domain.model.isSpo2OutOfRange
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.action_back
import vitalcare.shared.generated.resources.action_cancel
import vitalcare.shared.generated.resources.action_delete
import vitalcare.shared.generated.resources.action_edit
import vitalcare.shared.generated.resources.details_created
import vitalcare.shared.generated.resources.details_delete_message
import vitalcare.shared.generated.resources.details_delete_title
import vitalcare.shared.generated.resources.details_read_only
import vitalcare.shared.generated.resources.details_updated
import vitalcare.shared.generated.resources.error_generic
import vitalcare.shared.generated.resources.field_remarks
import vitalcare.shared.generated.resources.snackbar_reading_deleted
import vitalcare.shared.generated.resources.unit_bpm
import vitalcare.shared.generated.resources.unit_mmhg
import vitalcare.shared.generated.resources.unit_percent
import vitalcare.shared.generated.resources.vital_blood_pressure
import vitalcare.shared.generated.resources.vital_heart_rate
import vitalcare.shared.generated.resources.vital_spo2

@Composable
fun RecordDetailsScreen(
    recordId: String,
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit,
    showSnackbar: (String) -> Unit,
) {
    val viewModel = koinViewModel<RecordDetailsViewModel> { parametersOf(recordId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val deletedMessage = stringResource(Res.string.snackbar_reading_deleted)
    val genericError = stringResource(Res.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecordDetailsEffect.Deleted -> {
                    showSnackbar(deletedMessage)
                    onNavigateBack()
                }
                RecordDetailsEffect.NotFound -> onNavigateBack()
                RecordDetailsEffect.DeleteFailed -> showSnackbar(genericError)
            }
        }
    }

    val record = uiState.record ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        DetailsTopBar(
            record = record,
            isEditable = uiState.isEditable,
            onBack = onNavigateBack,
            onEdit = { onEdit(record.id) },
            onDelete = { viewModel.onEvent(RecordDetailsEvent.DeleteClicked) },
        )
        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BentoTile(tint = VitalCareTheme.colors.tintSage, icon = Icons.Rounded.WaterDrop) {
                VitalValueDisplay(
                    value = record.spo2?.toString() ?: "—",
                    unit = stringResource(Res.string.unit_percent),
                    label = stringResource(Res.string.vital_spo2),
                    outOfRange = record.isSpo2OutOfRange(),
                    hero = true,
                )
            }
            BentoTile(tint = VitalCareTheme.colors.tintBlue, icon = Icons.Rounded.Favorite) {
                VitalValueDisplay(
                    value = record.heartRate?.toString() ?: "—",
                    unit = stringResource(Res.string.unit_bpm),
                    label = stringResource(Res.string.vital_heart_rate),
                    outOfRange = record.isHeartRateOutOfRange(),
                    hero = true,
                )
            }
            BentoTile(tint = VitalCareTheme.colors.tintLavender, icon = Icons.Rounded.MonitorHeart) {
                VitalValueDisplay(
                    value = if (record.systolic != null && record.diastolic != null) {
                        "${record.systolic}/${record.diastolic}"
                    } else {
                        "—"
                    },
                    unit = stringResource(Res.string.unit_mmhg),
                    label = stringResource(Res.string.vital_blood_pressure),
                    outOfRange = record.isBloodPressureOutOfRange(),
                    hero = true,
                )
            }
            record.remarks?.let { remarks ->
                BentoTile(tint = VitalCareTheme.colors.tintCream) {
                    Text(
                        text = stringResource(Res.string.field_remarks),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text = remarks, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(Res.string.details_created, uiState.createdText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (record.updatedAt != record.createdAt) {
            Text(
                text = stringResource(Res.string.details_updated, uiState.updatedText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!uiState.isEditable) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.details_read_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.details_delete_title),
            message = stringResource(Res.string.details_delete_message),
            confirmLabel = stringResource(Res.string.action_delete),
            dismissLabel = stringResource(Res.string.action_cancel),
            onConfirm = { viewModel.onEvent(RecordDetailsEvent.DeleteConfirmed) },
            onDismiss = { viewModel.onEvent(RecordDetailsEvent.DeleteDismissed) },
            destructive = true,
        )
    }
}

@Composable
private fun DetailsTopBar(
    record: VitalRecord,
    isEditable: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(Res.string.action_back),
            onClick = onBack,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = FullDateFormat.format(record.date),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = TimeFormat.format(record.time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isEditable) {
            CircleIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = stringResource(Res.string.action_edit),
                onClick = onEdit,
            )
            Spacer(Modifier.width(8.dp))
            CircleIconButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(Res.string.action_delete),
                onClick = onDelete,
            )
        }
    }
}
