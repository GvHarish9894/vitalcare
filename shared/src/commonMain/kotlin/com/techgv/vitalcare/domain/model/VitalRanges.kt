package com.techgv.vitalcare.domain.model

import com.techgv.vitalcare.domain.validation.VitalsValidator

// Out-of-range hints for display (03 §3.5) — same ranges as validation (01 §2).

fun VitalRecord.isSpo2OutOfRange(): Boolean =
    spo2 != null && spo2 !in VitalsValidator.SPO2_RANGE

fun VitalRecord.isHeartRateOutOfRange(): Boolean =
    heartRate != null && heartRate !in VitalsValidator.HEART_RATE_RANGE

fun VitalRecord.isBloodPressureOutOfRange(): Boolean =
    (systolic != null && systolic !in VitalsValidator.SYSTOLIC_RANGE) ||
        (diastolic != null && diastolic !in VitalsValidator.DIASTOLIC_RANGE) ||
        (systolic != null && diastolic != null && diastolic >= systolic)
