package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.CsvEncoder
import com.techgv.vitalcare.core.util.fluidCsvExportFileName
import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.data.backup.FileExporter
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.repository.FluidRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock

/**
 * CSV export for fluid entries (FR-FL6, D-033): a separate RFC 4180 file for the
 * chosen scope, handed to the platform save/share sheet. Amounts are the
 * canonical mL value. Mirrors [ExportCsv].
 */
class ExportFluidCsv(
    private val repository: FluidRepository,
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
        val entries = when (snapshot) {
            is AppResult.Success -> snapshot.value
            is AppResult.Failure -> return Result.Failed(snapshot.error)
        }
        if (entries.isEmpty()) return Result.Empty

        val csv = csvEncoder.encode(CSV_HEADER, entries.map { it.toCsvRow() })
        val fileName = fluidCsvExportFileName(clock.nowLocal(timeZone))
        return when (val exported = fileExporter.export(fileName, "text/csv", csv)) {
            is AppResult.Success -> Result.Exported
            is AppResult.Failure -> Result.Failed(exported.error)
        }
    }

    private fun FluidEntry.toCsvRow(): List<String?> = listOf(
        date.toString(),
        time.toString(),
        type.name,
        amountMl.toString(),
        note,
        createdAt.toString(),
        updatedAt.toString(),
    )

    companion object {
        /** Column order is a documented, stable format (06 §3). */
        val CSV_HEADER = listOf(
            "date", "time", "type", "amount_ml", "note", "created_at", "updated_at",
        )
    }
}
