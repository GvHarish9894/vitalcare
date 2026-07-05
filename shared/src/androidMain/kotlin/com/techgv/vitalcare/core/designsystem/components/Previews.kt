package com.techgv.vitalcare.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme

@Preview
@Composable
private fun DesignSystemPreview() {
    VitalCareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PrimaryButton(text = "Record Vitals", onClick = {}, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "History", onClick = {})
                    CircleIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Search",
                        onClick = {},
                    )
                }
                BentoTile(
                    tint = VitalCareTheme.colors.tintSage,
                    hero = true,
                    icon = Icons.Rounded.WaterDrop,
                ) {
                    VitalValueDisplay(value = "98", unit = "%", label = "SpO₂", hero = true)
                }
                BentoTile(tint = VitalCareTheme.colors.tintBlue, icon = Icons.Rounded.Favorite) {
                    VitalValueDisplay(value = "182", unit = "bpm", label = "Heart Rate", outOfRange = false)
                }
                VitalTextField(
                    value = "98",
                    onValueChange = {},
                    label = "SpO₂",
                    suffix = "%",
                    supportingText = "70–100",
                )
                VitalTrendChart(
                    series = listOf(
                        ChartSeries(
                            points = listOf(
                                ChartPoint(0f, 96f),
                                ChartPoint(1f, 98f),
                                ChartPoint(2f, 97f),
                            ),
                            color = Color(0xFF4849A1),
                        ),
                    ),
                    startLabel = "Mon",
                    endLabel = "Wed",
                )
                BottomNavBar(
                    items = listOf(
                        BottomNavItem(Icons.Rounded.Home, "Home", selected = true, onClick = {}),
                        BottomNavItem(Icons.Rounded.History, "History", selected = false, onClick = {}),
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemDarkPreview() {
    VitalCareTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PrimaryButton(text = "Record Vitals", onClick = {}, modifier = Modifier.fillMaxWidth())
                BentoTile(
                    tint = VitalCareTheme.colors.tintLavender,
                    icon = Icons.Rounded.Favorite,
                ) {
                    VitalValueDisplay(value = "72", unit = "bpm", label = "Heart Rate", hero = true)
                }
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "No readings yet",
                    message = "Start by recording your first reading",
                )
            }
        }
    }
}
