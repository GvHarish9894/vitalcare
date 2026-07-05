package com.techgv.vitalcare.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * A single vitals reading (06 §1). Null vitals mean "not measured" (never 0).
 * `date` is fixed at creation (BR-4); `updatedAt` drives restore-merge LWW
 * (D-024) and is epoch millis UTC (D-016).
 */
data class VitalRecord(
    val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val spo2: Int?,
    val heartRate: Int?,
    val systolic: Int?,
    val diastolic: Int?,
    val remarks: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
