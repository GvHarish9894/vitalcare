package com.techgv.vitalcare.feature.fluids

import androidx.compose.runtime.Composable
import com.techgv.vitalcare.domain.validation.FluidError
import org.jetbrains.compose.resources.stringResource
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.error_amount_range
import vitalcare.shared.generated.resources.error_amount_required
import vitalcare.shared.generated.resources.error_not_a_number
import vitalcare.shared.generated.resources.error_note_too_long
import vitalcare.shared.generated.resources.error_time_future

/** Maps machine-readable fluid validation reasons to localized text (D-017). */
@Composable
fun FluidError.text(): String = stringResource(
    when (this) {
        FluidError.NOT_A_NUMBER -> Res.string.error_not_a_number
        FluidError.AMOUNT_REQUIRED -> Res.string.error_amount_required
        FluidError.AMOUNT_OUT_OF_RANGE -> Res.string.error_amount_range
        FluidError.TIME_IN_FUTURE -> Res.string.error_time_future
        FluidError.NOTE_TOO_LONG -> Res.string.error_note_too_long
    },
)
