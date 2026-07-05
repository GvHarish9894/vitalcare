package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeVitalsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.validation.VitalField
import com.techgv.vitalcare.domain.validation.VitalsError
import com.techgv.vitalcare.domain.validation.VitalsValidator
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveVitalRecordTest {

    private val repository = FakeVitalsRepository()
    private val saveVitalRecord = SaveVitalRecord(
        repository = repository,
        validator = VitalsValidator(Fixtures.clock, Fixtures.timeZone),
        clock = Fixtures.clock,
        timeZone = Fixtures.timeZone,
    )

    @Test
    fun createStampsIdTodayAndTimestamps() = runTest {
        val result = saveVitalRecord(Fixtures.input(spo2 = "95"))

        assertIs<SaveVitalRecord.Result.Saved>(result)
        val saved = repository.current().single()
        assertTrue(saved.id.isNotBlank())
        assertEquals(Fixtures.today, saved.date)
        assertEquals(95, saved.spo2)
        assertEquals(Fixtures.nowEpochMillis, saved.createdAt)
        assertEquals(Fixtures.nowEpochMillis, saved.updatedAt)
    }

    @Test
    fun invalidInputIsNeverPersisted() = runTest {
        val result = saveVitalRecord(Fixtures.input(spo2 = "101"))

        val invalid = assertIs<SaveVitalRecord.Result.Invalid>(result)
        assertEquals(VitalsError.SPO2_OUT_OF_RANGE, invalid.errors[VitalField.SPO2])
        assertTrue(repository.current().isEmpty())
    }

    @Test
    fun editPreservesIdentityAndBumpsUpdatedAt() = runTest {
        val existing = Fixtures.record(
            id = "existing-1",
            createdAt = 500L,
            updatedAt = 500L,
            spo2 = 98,
        )
        repository.seed(existing)

        val result = saveVitalRecord(
            Fixtures.input(time = LocalTime(9, 15), spo2 = "91"),
            existing = existing,
        )

        assertIs<SaveVitalRecord.Result.Saved>(result)
        val saved = repository.current().single()
        assertEquals("existing-1", saved.id)
        assertEquals(existing.date, saved.date)
        assertEquals(500L, saved.createdAt)
        assertEquals(Fixtures.nowEpochMillis, saved.updatedAt)
        assertEquals(91, saved.spo2)
        assertEquals(LocalTime(9, 15), saved.time)
    }

    @Test
    fun repositoryFailureSurfacesAsFailed() = runTest {
        repository.failWrites = true
        val result = saveVitalRecord(Fixtures.input())
        assertIs<SaveVitalRecord.Result.Failed>(result)
    }
}
