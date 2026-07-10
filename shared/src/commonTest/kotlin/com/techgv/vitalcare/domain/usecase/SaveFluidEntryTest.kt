package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeFluidRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.validation.FluidField
import com.techgv.vitalcare.domain.validation.FluidValidator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SaveFluidEntryTest {

    private val repository = FakeFluidRepository()
    private val validator = FluidValidator(Fixtures.clock, Fixtures.timeZone)
    private val save = SaveFluidEntry(repository, validator, Fixtures.clock, Fixtures.timeZone)

    @Test
    fun createStampsIdDateAndTimestamps() = runTest {
        val result = save(Fixtures.fluidInput(amount = "250", type = FluidType.INTAKE))

        assertIs<SaveFluidEntry.Result.Saved>(result)
        val stored = repository.current().single()
        assertNotNull(stored.id)
        assertEquals(Fixtures.today, stored.date)
        assertEquals(250, stored.amountMl)
        assertEquals(Fixtures.nowEpochMillis, stored.createdAt)
        assertEquals(Fixtures.nowEpochMillis, stored.updatedAt)
    }

    @Test
    fun editPreservesIdentityAndBumpsUpdatedAt() = runTest {
        val existing = Fixtures.fluid(id = "f1", amountMl = 250, createdAt = 111L, updatedAt = 111L)

        val result = save(Fixtures.fluidInput(amount = "400"), existing = existing)

        assertIs<SaveFluidEntry.Result.Saved>(result)
        val stored = repository.current().single()
        assertEquals("f1", stored.id)
        assertEquals(400, stored.amountMl)
        assertEquals(111L, stored.createdAt)         // preserved
        assertEquals(Fixtures.nowEpochMillis, stored.updatedAt) // bumped
    }

    @Test
    fun invalidInputIsNotPersisted() = runTest {
        val result = save(Fixtures.fluidInput(amount = ""))

        val invalid = assertIs<SaveFluidEntry.Result.Invalid>(result)
        assertEquals(FluidField.AMOUNT, invalid.errors.keys.single())
        assertEquals(0, repository.current().size)
    }

    @Test
    fun storageFailureSurfacesAsFailed() = runTest {
        repository.failWrites = true
        assertIs<SaveFluidEntry.Result.Failed>(save(Fixtures.fluidInput()))
    }
}
