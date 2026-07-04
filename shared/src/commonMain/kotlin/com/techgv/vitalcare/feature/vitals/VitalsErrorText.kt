package com.techgv.vitalcare.feature.vitals

import androidx.compose.runtime.Composable
import com.techgv.vitalcare.domain.validation.VitalsError
import org.jetbrains.compose.resources.stringResource
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.error_bp_pair
import vitalcare.shared.generated.resources.error_diastolic_not_less
import vitalcare.shared.generated.resources.error_diastolic_range
import vitalcare.shared.generated.resources.error_heart_rate_range
import vitalcare.shared.generated.resources.error_no_vitals
import vitalcare.shared.generated.resources.error_not_a_number
import vitalcare.shared.generated.resources.error_remarks_too_long
import vitalcare.shared.generated.resources.error_spo2_range
import vitalcare.shared.generated.resources.error_systolic_range
import vitalcare.shared.generated.resources.error_time_future

/** Maps machine-readable validation reasons to localized text (D-017). */
@Composable
fun VitalsError.text(): String = stringResource(
    when (this) {
        VitalsError.NOT_A_NUMBER -> Res.string.error_not_a_number
        VitalsError.SPO2_OUT_OF_RANGE -> Res.string.error_spo2_range
        VitalsError.HEART_RATE_OUT_OF_RANGE -> Res.string.error_heart_rate_range
        VitalsError.SYSTOLIC_OUT_OF_RANGE -> Res.string.error_systolic_range
        VitalsError.DIASTOLIC_OUT_OF_RANGE -> Res.string.error_diastolic_range
        VitalsError.DIASTOLIC_NOT_LESS_THAN_SYSTOLIC -> Res.string.error_diastolic_not_less
        VitalsError.BP_PAIR_REQUIRED -> Res.string.error_bp_pair
        VitalsError.NO_VITALS -> Res.string.error_no_vitals
        VitalsError.REMARKS_TOO_LONG -> Res.string.error_remarks_too_long
        VitalsError.TIME_IN_FUTURE -> Res.string.error_time_future
    },
)
