package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BackupSerializerTest {

    private val serializer = BackupSerializer()

    private fun backup(vararg records: BackupRecordDto) = BackupFile(
        schemaVersion = BackupSerializer.SCHEMA_VERSION,
        exportedAt = Fixtures.nowEpochMillis,
        appVersion = "1.0",
        profileName = "Amma",
        records = records.toList(),
    )

    @Test
    fun roundTripIsLossless() {
        val original = backup(
            Fixtures.record(id = "a", remarks = "Morning, resting").toBackupDto(),
            Fixtures.record(id = "b", spo2 = null, heartRate = 68, systolic = null, diastolic = null)
                .toBackupDto(),
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
            Fixtures.record(id = "a", spo2 = null, heartRate = 70, systolic = null, diastolic = null)
                .toBackupDto(),
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
            "\"schemaVersion\":1",
            "\"schemaVersion\":99",
        )

        val decoded = serializer.decode(futureDocument)

        val failure = assertIs<AppResult.Failure>(decoded)
        assertEquals(AppError.UnsupportedBackup(99), failure.error)
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
