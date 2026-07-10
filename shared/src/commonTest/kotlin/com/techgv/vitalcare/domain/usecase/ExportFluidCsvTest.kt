package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeFluidRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.CsvEncoder
import com.techgv.vitalcare.data.backup.FileExporter
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.HistoryFilter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeFileExporter : FileExporter {
    var fileName: String? = null
    var mimeType: String? = null
    var content: String? = null
    override suspend fun export(fileName: String, mimeType: String, content: String): AppResult<Unit> {
        this.fileName = fileName
        this.mimeType = mimeType
        this.content = content
        return AppResult.Success(Unit)
    }
}

class ExportFluidCsvTest {

    private val repository = FakeFluidRepository()
    private val exporter = FakeFileExporter()
    private val export = ExportFluidCsv(
        repository, CsvEncoder(), exporter, Fixtures.clock, Fixtures.timeZone,
    )

    @Test
    fun exportsCanonicalMlRowsWithStableHeader() = runTest {
        repository.seed(
            Fixtures.fluid(id = "f1", type = FluidType.INTAKE, amountMl = 250, note = "water"),
            Fixtures.fluid(id = "f2", type = FluidType.OUTPUT, amountMl = 300, note = null),
        )

        assertIs<ExportFluidCsv.Result.Exported>(export(HistoryFilter.ALL))

        val lines = exporter.content!!.trim().lines()
        assertEquals("date,time,type,amount_ml,note,created_at,updated_at", lines[0])
        assertEquals("2026-07-04,09:00,INTAKE,250,water,1000,1000", lines[1])
        assertEquals("2026-07-04,09:00,OUTPUT,300,,1000,1000", lines[2])
        assertTrue(exporter.fileName!!.startsWith("vitalcare-fluids-"))
    }

    @Test
    fun emptyScopeReportsEmptyAndDoesNotExport() = runTest {
        assertIs<ExportFluidCsv.Result.Empty>(export(HistoryFilter.ALL))
        assertEquals(null, exporter.content)
    }
}
