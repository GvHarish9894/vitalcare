package com.techgv.vitalcare.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.plus_jakarta_sans_bold
import vitalcare.shared.generated.resources.plus_jakarta_sans_extrabold
import vitalcare.shared.generated.resources.plus_jakarta_sans_medium
import vitalcare.shared.generated.resources.plus_jakarta_sans_regular
import vitalcare.shared.generated.resources.plus_jakarta_sans_semibold

/** Plus Jakarta Sans (D-030); single seam so a fallback is a one-line change. */
@Composable
internal fun vitalCareFontFamily(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(Res.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

/**
 * Bold geometric type scale (03 §4.3). Sizes are sp, so platform font scaling
 * applies everywhere including hero numerals (NFR-6).
 *
 * Role mapping: displayLarge = hero numerals · displayMedium/Small = screen
 * titles · titleLarge/Medium = card titles · body = content · label = field
 * labels, chips, units, tabs.
 */
@Composable
internal fun vitalCareTypography(): Typography {
    val family = vitalCareFontFamily()
    fun style(size: Int, weight: FontWeight, lineHeight: Int, letterSpacing: Double = 0.0) =
        TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            letterSpacing = letterSpacing.sp,
        )
    return Typography(
        displayLarge = style(48, FontWeight.ExtraBold, 52, letterSpacing = -1.0),
        displayMedium = style(34, FontWeight.ExtraBold, 40, letterSpacing = -0.5),
        displaySmall = style(30, FontWeight.ExtraBold, 36, letterSpacing = -0.5),
        headlineLarge = style(26, FontWeight.Bold, 32),
        headlineMedium = style(22, FontWeight.Bold, 28),
        headlineSmall = style(20, FontWeight.Bold, 26),
        titleLarge = style(18, FontWeight.Bold, 24),
        titleMedium = style(17, FontWeight.Bold, 22),
        titleSmall = style(15, FontWeight.SemiBold, 20),
        bodyLarge = style(16, FontWeight.Medium, 24),
        bodyMedium = style(15, FontWeight.Medium, 22),
        bodySmall = style(13, FontWeight.Medium, 18),
        labelLarge = style(14, FontWeight.SemiBold, 20),
        labelMedium = style(13, FontWeight.SemiBold, 18),
        labelSmall = style(12, FontWeight.SemiBold, 16),
    )
}
