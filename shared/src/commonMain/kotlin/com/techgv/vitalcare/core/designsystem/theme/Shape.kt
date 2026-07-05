package com.techgv.vitalcare.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Soft & rounded (03 §4.4): text fields 18 dp, cards 24 dp, hero tiles 28 dp.
 * Buttons and chips are full pills via component defaults (CircleShape).
 */
internal val VitalCareShapes = Shapes(
    extraSmall = RoundedCornerShape(18.dp), // filled text fields
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),     // cards
    large = RoundedCornerShape(28.dp),      // hero tiles
    extraLarge = RoundedCornerShape(32.dp),
)
