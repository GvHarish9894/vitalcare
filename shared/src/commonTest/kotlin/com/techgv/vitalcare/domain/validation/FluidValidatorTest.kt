package com.techgv.vitalcare.domain.validation

import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.VolumeUnit
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FluidValidatorTest {

    private val validator = FluidValidator(Fixtures.clock, Fixtures.timeZone)

    private fun validate(
        amount: String = "250",
        unit: VolumeUnit = VolumeUnit.ML,
        type: FluidType = FluidType.INTAKE,
        time: LocalTime = LocalTime(9, 0),
        note: String = "",
    ) = validator.validate(Fixtures.fluidInput(time = time, type = type, amount = amount, unit = unit, note = note))

    @Test
    fun validMlEntryParses() {
        val result = assertIs<FluidValidationResult.Valid>(validate(amount = "250"))
        assertEquals(250, result.fluid.amountMl)
        assertEquals(FluidType.INTAKE, result.fluid.type)
    }

    @Test
    fun ozEntryConvertsToCanonicalMl() {
        val result = assertIs<FluidValidationResult.Valid>(validate(amount = "8", unit = VolumeUnit.OZ))
        assertEquals(237, result.fluid.amountMl) // 8 fl oz ≈ 236.6 mL
    }

    @Test
    fun blankAmountIsRequired() {
        val result = assertIs<FluidValidationResult.Invalid>(validate(amount = ""))
        assertEquals(FluidError.AMOUNT_REQUIRED, result.errors[FluidField.AMOUNT])
    }

    @Test
    fun nonNumericAmountFlagged() {
        val result = assertIs<FluidValidationResult.Invalid>(validate(amount = "abc"))
        assertEquals(FluidError.NOT_A_NUMBER, result.errors[FluidField.AMOUNT])
    }

    @Test
    fun zeroAndOverMaxOutOfRange() {
        assertEquals(
            FluidError.AMOUNT_OUT_OF_RANGE,
            assertIs<FluidValidationResult.Invalid>(validate(amount = "0")).errors[FluidField.AMOUNT],
        )
        assertEquals(
            FluidError.AMOUNT_OUT_OF_RANGE,
            assertIs<FluidValidationResult.Invalid>(validate(amount = "5001")).errors[FluidField.AMOUNT],
        )
    }

    @Test
    fun rangeBoundariesAreInclusive() {
        assertIs<FluidValidationResult.Valid>(validate(amount = "1"))
        assertIs<FluidValidationResult.Valid>(validate(amount = "5000"))
    }

    @Test
    fun futureTimeRejectedForToday() {
        // Fixed "now" is 10:00; 11:00 today is in the future (BR-6).
        val result = assertIs<FluidValidationResult.Invalid>(validate(time = LocalTime(11, 0)))
        assertEquals(FluidError.TIME_IN_FUTURE, result.errors[FluidField.TIME])
    }

    @Test
    fun noteTooLongFlagged() {
        val result = assertIs<FluidValidationResult.Invalid>(validate(note = "x".repeat(501)))
        assertEquals(FluidError.NOTE_TOO_LONG, result.errors[FluidField.NOTE])
    }

    @Test
    fun blankNoteBecomesNull() {
        val result = assertIs<FluidValidationResult.Valid>(validate(note = "   "))
        assertEquals(null, result.fluid.note)
    }
}
