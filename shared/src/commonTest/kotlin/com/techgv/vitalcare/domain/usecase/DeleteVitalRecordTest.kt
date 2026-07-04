package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeVitalsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeleteVitalRecordTest {

    private val repository = FakeVitalsRepository()
    private val deleteVitalRecord = DeleteVitalRecord(
        repository = repository,
        clock = Fixtures.clock,
        timeZone = Fixtures.timeZone,
    )

    @Test
    fun deletesTodaysRecordPermanently() = runTest {
        repository.seed(Fixtures.record(id = "today-1", date = Fixtures.today))

        val result = deleteVitalRecord("today-1")

        assertIs<AppResult.Success<Unit>>(result)
        assertTrue(repository.current().isEmpty())
    }

    @Test
    fun refusesToDeletePastRecord() = runTest {
        repository.seed(Fixtures.record(id = "old-1", date = Fixtures.yesterday))

        val result = deleteVitalRecord("old-1")

        val failure = assertIs<AppResult.Failure>(result)
        assertEquals(AppError.NotAllowed, failure.error)
        assertEquals(1, repository.current().size)
    }

    @Test
    fun missingRecordIsNotFound() = runTest {
        val result = deleteVitalRecord("nope")
        val failure = assertIs<AppResult.Failure>(result)
        assertEquals(AppError.NotFound, failure.error)
    }
}
