package com.techgv.vitalcare.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.cd_out_of_range

/**
 * Hero numeral + unit + label. Out-of-range values get red + a warning icon —
 * never color alone (03 §5).
 */
@Composable
fun VitalValueDisplay(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    outOfRange: Boolean = false,
    hero: Boolean = false,
) {
    val valueColor =
        if (outOfRange) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val valueStyle =
        if (hero) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = valueStyle, color = valueColor)
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (outOfRange) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = stringResource(Res.string.cd_out_of_range),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp).size(18.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
