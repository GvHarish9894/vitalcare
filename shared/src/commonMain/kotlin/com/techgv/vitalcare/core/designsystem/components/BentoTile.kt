package com.techgv.vitalcare.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Soft-tint rounded tile — the core bento building block (03 §4.5). Content
 * on the tint must use near-black ink (`onSurface`), never a pastel (D-030).
 *
 * @param hero true = full-width hero tile with the larger 28 dp radius.
 */
@Composable
fun BentoTile(
    tint: Color,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    icon: ImageVector? = null,
    iconContentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = if (hero) MaterialTheme.shapes.large else MaterialTheme.shapes.medium
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(18.dp)) {
            if (icon != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = iconContentDescription,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = tint,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 1.dp,
        ) { body() }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = tint,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 1.dp,
        ) { body() }
    }
}
