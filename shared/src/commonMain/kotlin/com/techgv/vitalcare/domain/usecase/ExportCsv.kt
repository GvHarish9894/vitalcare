package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.CsvEncoder
import com.techgv.vitalcare.core.util.csvExportFileName
import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.data.backup.FileExporter
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock

/**
 * CSV export (FR-B1/FR-H6): RFC 4180 file for the chosen scope, handed to the
 * platform save/share sheet. Needs no account and no network (05 §3).
 */
class ExportCsv(
    private val repository: VitalsRepository,
    private val csvEncoder: CsvEncoder,
    private val fileExporter: FileExporter,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    sealed interface Result {
        data object Exported : Result
        data object Empty : Result
        data class Failed(val error: AppError) : Result
    }

    suspend operator fun invoke(scope: HistoryFilter): Result {
        val today = clock.todayLocal(timeZone)
        val snapshot = when (scope) {
            HistoryFilter.ALL -> repository.getAll()
            HistoryFilter.TODAY -> repository.getByDateRange(today, today)
            HistoryFilter.WEEK ->
                repository.getByDateRange(today.minus(6, DateTimeUnit.DAY), today)
            HistoryFilter.MONTH ->
                repository.getByDateRange(today.minus(29, DateTimeUnit.DAY), today)
        }
        val records = when (snapshot) {
            is AppResult.Success -> snapshot.value
            is AppResult.Failure -> return Result.Failed(snapshot.error)
        }
        if (records.isEmpty()) return Result.Empty

        val csv = csvEncoder.encode(CSV_HEADER, records.map { it.toCsvRow() })
        val fileName = csvExportFileName(clock.nowLocal(timeZone))
        return when (val exported = fileExporter.export(fileName, "text/csv", csv)) {
            is AppResult.Success -> Result.Exported
            is AppResult.Failure -> Result.Failed(exported.error)
        }
    }

    private fun VitalRecord.toCsvRow(): List<String?> = listOf(
        date.toString(),
        time.toString(),
        spo2?.toString(),
        heartRate?.toString(),
        systolic?.toString(),
        diastolic?.toString(),
        remarks,
        createdAt.toString(),
        updatedAt.toString(),
    )

    companion object {
        /** Column order is a documented, stable format (06 §3). */
        val CSV_HEADER = listOf(
            "date", "time", "spo2", "heart_rate", "systolic", "diastolic",
            "remarks", "created_at", "updated_at",
        )
    }
}
