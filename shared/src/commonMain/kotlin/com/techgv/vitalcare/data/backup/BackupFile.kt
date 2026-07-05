package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.domain.model.VitalRecord
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Versioned full-snapshot backup document (06 §4, D-023). One file, stored in
 * Drive's hidden appDataFolder and overwritten on each backup. `schemaVersion`
 * lets a future app read older backups; restore rejects newer versions.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersion: String,
    val profileName: String? = null,
    val records: List<BackupRecordDto>,
)

@Serializable
data class BackupRecordDto(
    val id: String,
    val date: String,
    val time: String,
    val spo2: Int? = null,
    val heartRate: Int? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

fun VitalRecord.toBackupDto(): BackupRecordDto = BackupRecordDto(
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

fun BackupRecordDto.toDomain(): VitalRecord = VitalRecord(
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
