package com.techgv.vitalcare.feature.fluids

import androidx.compose.runtime.Composable
import com.techgv.vitalcare.domain.model.VolumeUnit
import org.jetbrains.compose.resources.stringResource
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.fluid_amount_format
import vitalcare.shared.generated.resources.unit_ml
import vitalcare.shared.generated.resources.unit_oz

/** Localized short unit label ("mL" / "fl oz"). */
@Composable
fun VolumeUnit.label(): String = stringResource(
    when (this) {
        VolumeUnit.ML -> Res.string.unit_ml
        VolumeUnit.OZ -> Res.string.unit_oz
    },
)

/** A canonical mL amount rendered in the display unit with its suffix, e.g. "250 mL". */
@Composable
fun formatAmount(ml: Int, unit: VolumeUnit): String =
    stringResource(Res.string.fluid_amount_format, unit.format(ml), unit.label())

/** Signed net balance, e.g. "+300" / "−150" (number only, in the display unit). */
fun signedAmount(ml: Int, unit: VolumeUnit): String {
    val magnitude = unit.format(if (ml < 0) -ml else ml)
    return when {
        ml > 0 -> "+$magnitude"
        ml < 0 -> "−$magnitude"
        else -> magnitude
    }
}

/** Common quick-add presets, in canonical mL (D-032). */
val FluidPresetsMl: List<Int> = listOf(100, 250, 500)
