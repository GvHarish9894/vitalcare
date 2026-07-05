package com.techgv.vitalcare.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = PrimaryLight,
    onSecondary = OnPrimaryLight,
    secondaryContainer = PrimaryContainerLight,
    onSecondaryContainer = OnPrimaryContainerLight,
    tertiary = AccentLight,
    onTertiary = OnPrimaryLight,
    tertiaryContainer = TintPeachLight,
    onTertiaryContainer = InkLight,
    background = BackgroundLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceContainerHighestLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceContainerHigh = SurfaceContainerHighestLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainerLowest = SurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = PrimaryDark,
    onSecondary = OnPrimaryDark,
    secondaryContainer = PrimaryContainerDark,
    onSecondaryContainer = OnPrimaryContainerDark,
    tertiary = AccentDark,
    onTertiary = OnPrimaryDark,
    tertiaryContainer = TintPeachDark,
    onTertiaryContainer = InkDark,
    background = BackgroundDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceContainerHighestDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceContainerHigh = SurfaceContainerHighestDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainerLowest = SurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/**
 * "Soft Clinical" Material 3 theme (D-030): indigo primary, near-black ink,
 * pastel bento tints (via [VitalCareTheme.colors]), Plus Jakarta Sans,
 * full-pill/rounded shapes.
 */
@Composable
fun VitalCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extended = if (darkTheme) DarkVitalCareColors else LightVitalCareColors
    CompositionLocalProvider(LocalVitalCareColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = vitalCareTypography(),
            shapes = VitalCareShapes,
            content = content,
        )
    }
}

/** Accessor for the theme-extension colors, mirroring the MaterialTheme pattern. */
object VitalCareTheme {
    val colors: VitalCareColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVitalCareColors.current
}
