package com.techgv.vitalcare.feature.fluids

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.validation.FluidField
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
import vitalcare.shared.generated.resources.field_amount
import vitalcare.shared.generated.resources.field_date
import vitalcare.shared.generated.resources.field_note
import vitalcare.shared.generated.resources.field_time
import vitalcare.shared.generated.resources.fluid_intake
import vitalcare.shared.generated.resources.fluid_output
import vitalcare.shared.generated.resources.fluid_type
import vitalcare.shared.generated.resources.hint_range_amount
import vitalcare.shared.generated.resources.log_fluid_edit_title
import vitalcare.shared.generated.resources.log_fluid_title
import vitalcare.shared.generated.resources.snackbar_fluid_saved

@Composable
fun LogFluidScreen(
    entryId: String?,
    initialType: FluidType?,
    onNavigateBack: () -> Unit,
    showSnackbar: (String) -> Unit,
) {
    val viewModel = koinViewModel<LogFluidViewModel> { parametersOf(entryId, initialType) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val savedMessage = stringResource(Res.string.snackbar_fluid_saved)
    val genericError = stringResource(Res.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LogFluidEffect.Saved -> {
                    showSnackbar(savedMessage)
                    onNavigateBack()
                }
                LogFluidEffect.Close -> onNavigateBack()
                LogFluidEffect.SaveFailed -> showSnackbar(genericError)
                LogFluidEffect.LoadFailed -> {
                    showSnackbar(genericError)
                    onNavigateBack()
                }
            }
        }
    }

    LogFluidContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LogFluidContent(
    uiState: LogFluidUiState,
    onEvent: (LogFluidEvent) -> Unit,
) {
    BackHandler(enabled = uiState.isDirty) { onEvent(LogFluidEvent.CloseClicked) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        LogFluidTopBar(isEdit = uiState.isEdit, onClose = { onEvent(LogFluidEvent.CloseClicked) })

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

            FluidTypeSelector(
                selected = uiState.type,
                onSelect = { onEvent(LogFluidEvent.TypeChanged(it)) },
            )

            // Amount is the one field every entry needs — focus it with the
            // keypad open so a custom log is type-amount-save (03 §3.12).
            val amountFocus = remember { FocusRequester() }
            if (!uiState.isEdit) {
                LaunchedEffect(Unit) { amountFocus.requestFocus() }
            }
            VitalTextField(
                value = uiState.amount,
                onValueChange = { onEvent(LogFluidEvent.AmountChanged(it)) },
                label = stringResource(Res.string.field_amount),
                fieldModifier = Modifier.focusRequester(amountFocus),
                suffix = uiState.unit.label(),
                supportingText = stringResource(Res.string.hint_range_amount),
                errorText = uiState.fieldErrors[FluidField.AMOUNT]?.text(),
                keyboardType = if (uiState.unit == VolumeUnit.OZ) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Number
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluidPresetsMl.forEach { ml ->
                    FilterChip(
                        selected = false,
                        onClick = { onEvent(LogFluidEvent.PresetSelected(ml)) },
                        shape = CircleShape,
                        label = { Text(formatAmount(ml, uiState.unit)) },
                    )
                }
            }

            VitalTextField(
                value = TimeFormat.format(uiState.time),
                onValueChange = {},
                label = stringResource(Res.string.field_time),
                errorText = uiState.fieldErrors[FluidField.TIME]?.text(),
                onClick = { onEvent(LogFluidEvent.TimeFieldClicked) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = stringResource(Res.string.cd_pick_time),
                    )
                },
            )

            VitalTextField(
                value = uiState.note,
                onValueChange = { onEvent(LogFluidEvent.NoteChanged(it)) },
                label = stringResource(Res.string.field_note),
                supportingText = uiState.fieldErrors[FluidField.NOTE]?.text()
                    ?: "${uiState.note.length}/500",
                errorText = uiState.fieldErrors[FluidField.NOTE]?.text(),
                singleLine = false,
                minLines = 2,
            )
            Spacer(Modifier.height(4.dp))
        }

        PrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = { onEvent(LogFluidEvent.SaveClicked) },
            loading = uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }

    if (uiState.showTimePicker) {
        TimePickerDialog(
            initial = uiState.time,
            onConfirm = { onEvent(LogFluidEvent.TimeChanged(it)) },
            onDismiss = { onEvent(LogFluidEvent.TimePickerDismissed) },
        )
    }
    if (uiState.showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.discard_title),
            message = stringResource(Res.string.discard_message),
            confirmLabel = stringResource(Res.string.discard_confirm),
            dismissLabel = stringResource(Res.string.discard_dismiss),
            onConfirm = { onEvent(LogFluidEvent.DiscardConfirmed) },
            onDismiss = { onEvent(LogFluidEvent.DiscardDismissed) },
            destructive = true,
        )
    }
}

@Composable
private fun FluidTypeSelector(selected: FluidType, onSelect: (FluidType) -> Unit) {
    Column {
        Text(
            text = stringResource(Res.string.fluid_type),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluidType.entries.forEach { type ->
                FilterChip(
                    selected = type == selected,
                    onClick = { onSelect(type) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    label = {
                        Text(
                            text = stringResource(
                                when (type) {
                                    FluidType.INTAKE -> Res.string.fluid_intake
                                    FluidType.OUTPUT -> Res.string.fluid_output
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
private fun LogFluidTopBar(isEdit: Boolean, onClose: () -> Unit) {
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
                if (isEdit) Res.string.log_fluid_edit_title else Res.string.log_fluid_title,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
