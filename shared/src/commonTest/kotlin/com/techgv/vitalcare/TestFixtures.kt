package com.techgv.vitalcare

import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.validation.VitalsInput
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** Deterministic clock — tests never read real time (08-testing). */
class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

object Fixtures {
    val timeZone: TimeZone = TimeZone.UTC

    /** "Now" for all tests: 2026-07-04 10:00 UTC. */
    val nowInstant: Instant = Instant.parse("2026-07-04T10:00:00Z")
    val clock: Clock = FixedClock(nowInstant)
    val today: LocalDate = LocalDate(2026, 7, 4)
    val yesterday: LocalDate = LocalDate(2026, 7, 3)
    val nowEpochMillis: Long = nowInstant.toEpochMilliseconds()

    fun record(
        id: String = "record-1",
        date: LocalDate = today,
        time: LocalTime = LocalTime(8, 30),
        spo2: Int? = 98,
        heartRate: Int? = 72,
        systolic: Int? = 120,
        diastolic: Int? = 80,
        remarks: String? = null,
        createdAt: Long = 1_000L,
        updatedAt: Long = 1_000L,
    ): VitalRecord = VitalRecord(
        id = id,
        date = date,
        time = time,
        spo2 = spo2,
        heartRate = heartRate,
        systolic = systolic,
        diastolic = diastolic,
        remarks = remarks,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun input(
        date: LocalDate = today,
        time: LocalTime = LocalTime(8, 30),
        spo2: String = "98",
        heartRate: String = "72",
        systolic: String = "120",
        diastolic: String = "80",
        remarks: String = "",
    ): VitalsInput = VitalsInput(
        date = date,
        time = time,
        spo2 = spo2,
        heartRate = heartRate,
        systolic = systolic,
        diastolic = diastolic,
        remarks = remarks,
    )
}
