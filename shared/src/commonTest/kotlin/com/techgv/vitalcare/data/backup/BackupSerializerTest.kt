package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.FluidType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupSerializerTest {

    private val serializer = BackupSerializer()

    private fun backup(
        records: List<BackupRecordDto> = emptyList(),
        fluids: List<FluidEntryDto> = emptyList(),
    ) = BackupFile(
        schemaVersion = BackupSerializer.SCHEMA_VERSION,
        exportedAt = Fixtures.nowEpochMillis,
        appVersion = "1.0",
        profileName = "Amma",
        records = records,
        fluids = fluids,
    )

    @Test
    fun roundTripIsLossless() {
        val original = backup(
            records = listOf(
                Fixtures.record(id = "a", remarks = "Morning, resting").toBackupDto(),
                Fixtures.record(id = "b", spo2 = null, heartRate = 68, systolic = null, diastolic = null)
                    .toBackupDto(),
            ),
        )

        val decoded = serializer.decode(serializer.encode(original))

        val result = assertIs<AppResult.Success<BackupFile>>(decoded)
        assertEquals(original, result.value)
        // And the DTOs map back to identical domain records.
        assertEquals(
            Fixtures.record(id = "a", remarks = "Morning, resting"),
            result.value.records[0].toDomain(),
        )
    }

    @Test
    fun nullVitalsStayNullNotZero() {
        val original = backup(
            records = listOf(
                Fixtures.record(id = "a", spo2 = null, heartRate = 70, systolic = null, diastolic = null)
                    .toBackupDto(),
            ),
        )

        val decoded = assertIs<AppResult.Success<BackupFile>>(
            serializer.decode(serializer.encode(original)),
        )

        assertNull(decoded.value.records.single().spo2)
        assertNull(decoded.value.records.single().systolic)
    }

    @Test
    fun rejectsNewerSchemaVersionWithClearError() {
        val futureDocument = serializer.encode(backup()).replace(
            "\"schemaVersion\":${BackupSerializer.SCHEMA_VERSION}",
            "\"schemaVersion\":99",
        )

        val decoded = serializer.decode(futureDocument)

        val failure = assertIs<AppResult.Failure>(decoded)
        assertEquals(AppError.UnsupportedBackup(99), failure.error)
    }

    @Test
    fun fluidsRoundTripLosslessly() {
        val original = backup(
            fluids = listOf(
                Fixtures.fluid(id = "f1", amountMl = 250, note = "water").toBackupDto(),
                Fixtures.fluid(id = "f2", type = FluidType.OUTPUT, amountMl = 300).toBackupDto(),
            ),
        )

        val decoded = assertIs<AppResult.Success<BackupFile>>(
            serializer.decode(serializer.encode(original)),
        )

        assertEquals(original, decoded.value)
        assertEquals(
            Fixtures.fluid(id = "f1", amountMl = 250, note = "water"),
            decoded.value.fluids[0].toDomain(),
        )
    }

    @Test
    fun readsOlderV1BackupWithNoFluidsArray() {
        // A v1 document predates fluids (D-033) — it must still decode, with fluids empty.
        val v1 = """
            {"schemaVersion":1,"exportedAt":1,"appVersion":"0.9",
             "records":[{"id":"a","date":"2026-07-04","time":"08:30",
             "createdAt":1,"updatedAt":1}]}
        """.trimIndent()

        val decoded = assertIs<AppResult.Success<BackupFile>>(serializer.decode(v1))

        assertEquals(1, decoded.value.records.size)
        assertTrue(decoded.value.fluids.isEmpty())
    }

    @Test
    fun toleratesUnknownFieldsFromMinorRevisions() {
        val withExtra = serializer.encode(backup()).replace(
            "\"appVersion\":",
            "\"someFutureField\":\"x\",\"appVersion\":",
        )

        assertIs<AppResult.Success<BackupFile>>(serializer.decode(withExtra))
    }

    @Test
    fun garbageInputFailsAsCorrupt() {
        val failure = assertIs<AppResult.Failure>(serializer.decode("not json at all"))
        assertEquals(AppError.CorruptBackup, failure.error)
    }
}
