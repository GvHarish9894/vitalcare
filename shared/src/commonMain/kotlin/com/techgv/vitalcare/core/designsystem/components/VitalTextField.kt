package com.techgv.vitalcare.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType

/**
 * Filled, rounded text field with unit suffix, range hint, and error slot
 * (03 §4.5). Passing [onClick] renders a read-only field that opens a picker
 * (used for the time field).
 */
@Composable
fun VitalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
            supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            readOnly = onClick != null,
            trailingIcon = trailingIcon,
            shape = MaterialTheme.shapes.extraSmall,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.matchParentSizeIfNeeded(onClick != null),
        )
        if (onClick != null) {
            // TextField consumes taps even when read-only; a transparent overlay
            // forwards them to the picker instead.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
            )
        }
    }
}

private fun Modifier.matchParentSizeIfNeeded(needed: Boolean): Modifier = this
