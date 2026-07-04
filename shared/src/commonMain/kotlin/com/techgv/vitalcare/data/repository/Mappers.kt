package com.techgv.vitalcare.data.repository

import com.techgv.vitalcare.data.local.VitalRecordEntity
import com.techgv.vitalcare.domain.model.VitalRecord
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun VitalRecordEntity.toDomain(): VitalRecord = VitalRecord(
    id = id,
    date = LocalDate.parse(date),
    time = LocalTime.parse(time),
    spo2 = spo2,
    heartRate = heartRate,
    systolic = systolic,
    diastolic = diastolic,
    remarks = remarks,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun VitalRecord.toEntity(): VitalRecordEntity = VitalRecordEntity(
    id = id,
    date = date.toString(),
    time = time.toString(),
    spo2 = spo2,
    heartRate = heartRate,
    systolic = systolic,
    diastolic = diastolic,
    remarks = remarks,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
