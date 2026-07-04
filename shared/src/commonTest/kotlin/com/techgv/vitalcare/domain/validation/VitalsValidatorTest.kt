package com.techgv.vitalcare.domain.validation

import com.techgv.vitalcare.Fixtures
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class VitalsValidatorTest {

    private val validator = VitalsValidator(Fixtures.clock, Fixtures.timeZone)

    private fun assertFieldError(
        result: ValidationResult,
        field: VitalField,
        error: VitalsError,
    ) {
        val invalid = assertIs<ValidationResult.Invalid>(result)
        assertEquals(error, invalid.errors[field])
    }

    // --- SpO₂ boundaries (70–100) ---

    @Test
    fun spo2AtBoundariesIsValid() {
        assertIs<ValidationResult.Valid>(validator.validate(Fixtures.input(spo2 = "70")))
        assertIs<ValidationResult.Valid>(validator.validate(Fixtures.input(spo2 = "100")))
    }

    @Test
    fun spo2OutsideBoundariesIsInvalid() {
        assertFieldError(
            validator.validate(Fixtures.input(spo2 = "69")),
            VitalField.SPO2, VitalsError.SPO2_OUT_OF_RANGE,
        )
        assertFieldError(
            validator.validate(Fixtures.input(spo2 = "101")),
            VitalField.SPO2, VitalsError.SPO2_OUT_OF_RANGE,
        )
    }

    // --- Heart rate boundaries (20–250) ---

    @Test
    fun heartRateAtBoundariesIsValid() {
        assertIs<ValidationResult.Valid>(validator.validate(Fixtures.input(heartRate = "20")))
        assertIs<ValidationResult.Valid>(validator.validate(Fixtures.input(heartRate = "250")))
    }

    @Test
    fun heartRateOutsideBoundariesIsInvalid() {
        assertFieldError(
            validator.validate(Fixtures.input(heartRate = "19")),
            VitalField.HEART_RATE, VitalsError.HEART_RATE_OUT_OF_RANGE,
        )
        assertFieldError(
            validator.validate(Fixtures.input(heartRate = "251")),
            VitalField.HEART_RATE, VitalsError.HEART_RATE_OUT_OF_RANGE,
        )
    }

    // --- Systolic boundaries (50–250) ---

    @Test
    fun systolicAtBoundariesIsValid() {
        assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(systolic = "50", diastolic = "30")),
        )
        assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(systolic = "250", diastolic = "80")),
        )
    }

    @Test
    fun systolicOutsideBoundariesIsInvalid() {
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "49", diastolic = "30")),
            VitalField.SYSTOLIC, VitalsError.SYSTOLIC_OUT_OF_RANGE,
        )
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "251", diastolic = "80")),
            VitalField.SYSTOLIC, VitalsError.SYSTOLIC_OUT_OF_RANGE,
        )
    }

    // --- Diastolic boundaries (30–180, < systolic) ---

    @Test
    fun diastolicAtBoundariesIsValid() {
        assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(systolic = "200", diastolic = "30")),
        )
        assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(systolic = "250", diastolic = "180")),
        )
    }

    @Test
    fun diastolicOutsideBoundariesIsInvalid() {
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "120", diastolic = "29")),
            VitalField.DIASTOLIC, VitalsError.DIASTOLIC_OUT_OF_RANGE,
        )
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "250", diastolic = "181")),
            VitalField.DIASTOLIC, VitalsError.DIASTOLIC_OUT_OF_RANGE,
        )
    }

    @Test
    fun diastolicMustBeStrictlyLessThanSystolic() {
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "120", diastolic = "120")),
            VitalField.DIASTOLIC, VitalsError.DIASTOLIC_NOT_LESS_THAN_SYSTOLIC,
        )
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "110", diastolic = "120")),
            VitalField.DIASTOLIC, VitalsError.DIASTOLIC_NOT_LESS_THAN_SYSTOLIC,
        )
    }

    // --- BP pair rule ---

    @Test
    fun systolicWithoutDiastolicFlagsMissingHalf() {
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "120", diastolic = "")),
            VitalField.DIASTOLIC, VitalsError.BP_PAIR_REQUIRED,
        )
    }

    @Test
    fun diastolicWithoutSystolicFlagsMissingHalf() {
        assertFieldError(
            validator.validate(Fixtures.input(systolic = "", diastolic = "80")),
            VitalField.SYSTOLIC, VitalsError.BP_PAIR_REQUIRED,
        )
    }

    // --- At least one vital ---

    @Test
    fun allVitalsEmptyIsInvalid() {
        assertFieldError(
            validator.validate(
                Fixtures.input(spo2 = "", heartRate = "", systolic = "", diastolic = ""),
            ),
            VitalField.FORM, VitalsError.NO_VITALS,
        )
    }

    @Test
    fun singleVitalAloneIsValidAndOthersAreNull() {
        val result = validator.validate(
            Fixtures.input(spo2 = "", heartRate = "72", systolic = "", diastolic = ""),
        )
        val valid = assertIs<ValidationResult.Valid>(result)
        assertEquals(72, valid.vitals.heartRate)
        assertNull(valid.vitals.spo2)
        assertNull(valid.vitals.systolic)
        assertNull(valid.vitals.diastolic)
    }

    // --- Remarks ---

    @Test
    fun remarksAt500IsValidAnd501IsInvalid() {
        assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(remarks = "a".repeat(500))),
        )
        assertFieldError(
            validator.validate(Fixtures.input(remarks = "a".repeat(501))),
            VitalField.REMARKS, VitalsError.REMARKS_TOO_LONG,
        )
    }

    @Test
    fun blankRemarksBecomeNull() {
        val valid = assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(remarks = "   ")),
        )
        assertNull(valid.vitals.remarks)
    }

    // --- Time rules (fixture "now" is 10:00) ---

    @Test
    fun futureTimeTodayIsInvalid() {
        assertFieldError(
            validator.validate(Fixtures.input(time = LocalTime(10, 1))),
            VitalField.TIME, VitalsError.TIME_IN_FUTURE,
        )
    }

    @Test
    fun currentTimeTodayIsValid() {
        assertIs<ValidationResult.Valid>(
            validator.validate(Fixtures.input(time = LocalTime(10, 0))),
        )
    }

    @Test
    fun eveningTimeOnPastDateIsValid() {
        assertIs<ValidationResult.Valid>(
            validator.validate(
                Fixtures.input(date = Fixtures.yesterday, time = LocalTime(23, 59)),
            ),
        )
    }

    // --- Parsing ---

    @Test
    fun nonNumericVitalIsInvalid() {
        assertFieldError(
            validator.validate(Fixtures.input(spo2 = "abc")),
            VitalField.SPO2, VitalsError.NOT_A_NUMBER,
        )
    }
}
