package com.techgv.vitalcare.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-extension colors that have no Material 3 slot: the bento tile tints,
 * the limited warm accent (never a warning, D-030), and success/warning.
 * Text on every tint must be near-black ink — use `MaterialTheme.colorScheme.onSurface`.
 */
@Immutable
data class VitalCareColors(
    val tintSage: Color,
    val tintBlue: Color,
    val tintLavender: Color,
    val tintPeach: Color,
    val tintCream: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
)

internal val LightVitalCareColors = VitalCareColors(
    tintSage = TintSageLight,
    tintBlue = TintBlueLight,
    tintLavender = TintLavenderLight,
    tintPeach = TintPeachLight,
    tintCream = TintCreamLight,
    accent = AccentLight,
    onAccent = Color(0xFFFFFFFF),
    success = SuccessLight,
    warning = WarningLight,
)

internal val DarkVitalCareColors = VitalCareColors(
    tintSage = TintSageDark,
    tintBlue = TintBlueDark,
    tintLavender = TintLavenderDark,
    tintPeach = TintPeachDark,
    tintCream = TintCreamDark,
    accent = AccentDark,
    onAccent = Color(0xFF3A1404),
    success = SuccessDark,
    warning = WarningDark,
)

internal val LocalVitalCareColors = staticCompositionLocalOf { LightVitalCareColors }
