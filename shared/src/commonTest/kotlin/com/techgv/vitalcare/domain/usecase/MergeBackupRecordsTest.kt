package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeBackupRecordsTest {

    private val merge = MergeBackupRecords()

    @Test
    fun addsRecordsThatOnlyExistInBackup() {
        val local = listOf(Fixtures.record(id = "local-1"))
        val incoming = listOf(Fixtures.record(id = "backup-1"))

        val toWrite = merge(local, incoming)

        assertEquals(listOf("backup-1"), toWrite.map { it.id })
    }

    @Test
    fun newerIncomingWinsOverOlderLocal() {
        val local = listOf(Fixtures.record(id = "r", spo2 = 90, updatedAt = 100L))
        val incoming = listOf(Fixtures.record(id = "r", spo2 = 97, updatedAt = 200L))

        val toWrite = merge(local, incoming)

        assertEquals(97, toWrite.single().spo2)
    }

    @Test
    fun olderIncomingNeverOverwritesNewerLocal() {
        val local = listOf(Fixtures.record(id = "r", spo2 = 97, updatedAt = 200L))
        val incoming = listOf(Fixtures.record(id = "r", spo2 = 90, updatedAt = 100L))

        assertTrue(merge(local, incoming).isEmpty())
    }

    @Test
    fun equalTimestampsMeanNoWrite() {
        val record = Fixtures.record(id = "r", updatedAt = 100L)

        assertTrue(merge(listOf(record), listOf(record)).isEmpty())
    }

    @Test
    fun localOnlyRecordsAreNeverTouched() {
        val local = listOf(
            Fixtures.record(id = "keep-1"),
            Fixtures.record(id = "keep-2"),
        )

        val toWrite = merge(local, incoming = emptyList())

        assertTrue(toWrite.isEmpty()) // restore never deletes or rewrites local-only rows
    }

    @Test
    fun mergingTheSameBackupTwiceIsIdempotent() {
        val local = listOf(Fixtures.record(id = "r", updatedAt = 100L))
        val incoming = listOf(Fixtures.record(id = "r", spo2 = 96, updatedAt = 200L))

        val firstPass = merge(local, incoming)
        // Simulate applying the first pass, then restoring the same backup again.
        val afterFirst = firstPass + local.filter { l -> firstPass.none { it.id == l.id } }
        val secondPass = merge(afterFirst, incoming)

        assertEquals(1, firstPass.size)
        assertTrue(secondPass.isEmpty())
    }
}
