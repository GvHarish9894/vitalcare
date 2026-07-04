package com.techgv.vitalcare.feature.vitals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.CircleIconButton
import com.techgv.vitalcare.core.designsystem.components.ConfirmDialog
import com.techgv.vitalcare.core.designsystem.components.PrimaryButton
import com.techgv.vitalcare.core.designsystem.components.TimePickerDialog
import com.techgv.vitalcare.core.designsystem.components.VitalTextField
import com.techgv.vitalcare.core.util.FullDateFormat
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.domain.validation.VitalField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.action_close
import vitalcare.shared.generated.resources.action_save
import vitalcare.shared.generated.resources.cd_pick_time
import vitalcare.shared.generated.resources.date_today_format
import vitalcare.shared.generated.resources.discard_confirm
import vitalcare.shared.generated.resources.discard_dismiss
import vitalcare.shared.generated.resources.discard_message
import vitalcare.shared.generated.resources.discard_title
import vitalcare.shared.generated.resources.error_generic
import vitalcare.shared.generated.resources.field_date
import vitalcare.shared.generated.resources.field_remarks
import vitalcare.shared.generated.resources.field_remarks_hint
import vitalcare.shared.generated.resources.field_time
import vitalcare.shared.generated.resources.hint_range_diastolic
import vitalcare.shared.generated.resources.hint_range_heart_rate
import vitalcare.shared.generated.resources.hint_range_spo2
import vitalcare.shared.generated.resources.hint_range_systolic
import vitalcare.shared.generated.resources.record_vitals_edit_title
import vitalcare.shared.generated.resources.record_vitals_title
import vitalcare.shared.generated.resources.snackbar_reading_saved
import vitalcare.shared.generated.resources.unit_bpm
import vitalcare.shared.generated.resources.unit_mmhg
import vitalcare.shared.generated.resources.unit_percent
import vitalcare.shared.generated.resources.vital_diastolic
import vitalcare.shared.generated.resources.vital_heart_rate
import vitalcare.shared.generated.resources.vital_spo2
import vitalcare.shared.generated.resources.vital_systolic

@Composable
fun RecordVitalsScreen(
    recordId: String?,
    onNavigateBack: () -> Unit,
    showSnackbar: (String) -> Unit,
) {
    val viewModel = koinViewModel<RecordVitalsViewModel> { parametersOf(recordId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val savedMessage = stringResource(Res.string.snackbar_reading_saved)
    val genericError = stringResource(Res.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecordVitalsEffect.Saved -> {
                    showSnackbar(savedMessage)
                    onNavigateBack()
                }
                RecordVitalsEffect.Close -> onNavigateBack()
                RecordVitalsEffect.SaveFailed -> showSnackbar(genericError)
                RecordVitalsEffect.LoadFailed -> {
                    showSnackbar(genericError)
                    onNavigateBack()
                }
            }
        }
    }

    RecordVitalsContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun RecordVitalsContent(
    uiState: RecordVitalsUiState,
    onEvent: (RecordVitalsEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        RecordVitalsTopBar(isEdit = uiState.isEdit, onClose = { onEvent(RecordVitalsEvent.CloseClicked) })

        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Date — auto, visually non-editable (BR-4). Create mode always
            // dates the record today; edits (today-only, BR-2) show the date plainly.
            val formattedDate = FullDateFormat.format(uiState.date)
            VitalTextField(
                value = if (uiState.isEdit) {
                    formattedDate
                } else {
                    stringResource(Res.string.date_today_format, formattedDate)
                },
                onValueChange = {},
                label = stringResource(Res.string.field_date),
                readOnly = true,
                enabled = false,
            )
            VitalTextField(
                value = TimeFormat.format(uiState.time),
                onValueChange = {},
                label = stringResource(Res.string.field_time),
                errorText = uiState.fieldErrors[VitalField.TIME]?.text(),
                onClick = { onEvent(RecordVitalsEvent.TimeFieldClicked) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = stringResource(Res.string.cd_pick_time),
                    )
                },
            )
            VitalTextField(
                value = uiState.spo2,
                onValueChange = { onEvent(RecordVitalsEvent.Spo2Changed(it)) },
                label = stringResource(Res.string.vital_spo2),
                suffix = stringResource(Res.string.unit_percent),
                supportingText = stringResource(Res.string.hint_range_spo2),
                errorText = uiState.fieldErrors[VitalField.SPO2]?.text(),
                keyboardType = KeyboardType.Number,
            )
            VitalTextField(
                value = uiState.heartRate,
                onValueChange = { onEvent(RecordVitalsEvent.HeartRateChanged(it)) },
                label = stringResource(Res.string.vital_heart_rate),
                suffix = stringResource(Res.string.unit_bpm),
                supportingText = stringResource(Res.string.hint_range_heart_rate),
                errorText = uiState.fieldErrors[VitalField.HEART_RATE]?.text(),
                keyboardType = KeyboardType.Number,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalTextField(
                    value = uiState.systolic,
                    onValueChange = { onEvent(RecordVitalsEvent.SystolicChanged(it)) },
                    label = stringResource(Res.string.vital_systolic),
                    suffix = stringResource(Res.string.unit_mmhg),
                    supportingText = stringResource(Res.string.hint_range_systolic),
                    errorText = uiState.fieldErrors[VitalField.SYSTOLIC]?.text(),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                VitalTextField(
                    value = uiState.diastolic,
                    onValueChange = { onEvent(RecordVitalsEvent.DiastolicChanged(it)) },
                    label = stringResource(Res.string.vital_diastolic),
                    suffix = stringResource(Res.string.unit_mmhg),
                    supportingText = stringResource(Res.string.hint_range_diastolic),
                    errorText = uiState.fieldErrors[VitalField.DIASTOLIC]?.text(),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            VitalTextField(
                value = uiState.remarks,
                onValueChange = { onEvent(RecordVitalsEvent.RemarksChanged(it)) },
                label = stringResource(Res.string.field_remarks),
                supportingText = uiState.fieldErrors[VitalField.REMARKS]?.text()
                    ?: "${uiState.remarks.length}/500",
                errorText = uiState.fieldErrors[VitalField.REMARKS]?.text(),
                singleLine = false,
                minLines = 3,
            )
            uiState.fieldErrors[VitalField.FORM]?.let { formError ->
                Text(
                    text = formError.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        PrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = { onEvent(RecordVitalsEvent.SaveClicked) },
            loading = uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }

    if (uiState.showTimePicker) {
        TimePickerDialog(
            initial = uiState.time,
            onConfirm = { onEvent(RecordVitalsEvent.TimeChanged(it)) },
            onDismiss = { onEvent(RecordVitalsEvent.TimePickerDismissed) },
        )
    }
    if (uiState.showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.discard_title),
            message = stringResource(Res.string.discard_message),
            confirmLabel = stringResource(Res.string.discard_confirm),
            dismissLabel = stringResource(Res.string.discard_dismiss),
            onConfirm = { onEvent(RecordVitalsEvent.DiscardConfirmed) },
            onDismiss = { onEvent(RecordVitalsEvent.DiscardDismissed) },
            destructive = true,
        )
    }
}

@Composable
private fun RecordVitalsTopBar(isEdit: Boolean, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = stringResource(Res.string.action_close),
            onClick = onClose,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(
                if (isEdit) Res.string.record_vitals_edit_title else Res.string.record_vitals_title,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
