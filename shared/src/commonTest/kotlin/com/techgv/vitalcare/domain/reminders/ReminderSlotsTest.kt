package com.techgv.vitalcare.domain.reminders

import com.techgv.vitalcare.domain.model.ReminderPreferences
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderSlotsTest {

    private val date = LocalDate(2026, 7, 5)
    private fun at(hour: Int, minute: Int = 0) = LocalDateTime(date, LocalTime(hour, minute))

    private fun prefs(
        enabled: Boolean = true,
        intervalHours: Int = 2,
        from: LocalTime = LocalTime(8, 0),
        until: LocalTime = LocalTime(21, 0),
        skipIfRecorded: Boolean = true,
    ) = ReminderPreferences(enabled, intervalHours, from, until, skipIfRecorded)

    // --- slotsFor ---

    @Test
    fun everyTwoHoursWithinDayWindow() {
        assertEquals(
            listOf(8, 10, 12, 14, 16, 18, 20).map { LocalTime(it, 0) },
            ReminderSlots.slotsFor(prefs(intervalHours = 2)),
        )
    }

    @Test
    fun hourlySlotsIncludeWindowEnd() {
        val slots = ReminderSlots.slotsFor(prefs(intervalHours = 1))
        assertEquals(14, slots.size) // 8:00 .. 21:00 inclusive
        assertEquals(LocalTime(21, 0), slots.last())
    }

    @Test
    fun overnightWindowWrapsMidnight() {
        assertEquals(
            listOf(
                LocalTime(21, 0), LocalTime(23, 0), LocalTime(1, 0),
                LocalTime(3, 0), LocalTime(5, 0), LocalTime(7, 0),
            ),
            ReminderSlots.slotsFor(prefs(from = LocalTime(21, 0), until = LocalTime(8, 0))),
        )
    }

    @Test
    fun equalStartAndEndMeansAllDay() {
        val slots = ReminderSlots.slotsFor(
            prefs(intervalHours = 6, from = LocalTime(8, 0), until = LocalTime(8, 0)),
        )
        assertEquals(
            listOf(LocalTime(8, 0), LocalTime(14, 0), LocalTime(20, 0), LocalTime(2, 0)),
            slots,
        )
    }

    @Test
    fun windowShorterThanIntervalStillRemindsAtStart() {
        val slots = ReminderSlots.slotsFor(
            prefs(intervalHours = 12, from = LocalTime(8, 0), until = LocalTime(12, 0)),
        )
        assertEquals(listOf(LocalTime(8, 0)), slots)
    }

    @Test
    fun slotCountIsCappedForHourlyAllDay() {
        val slots = ReminderSlots.slotsFor(
            prefs(intervalHours = 1, from = LocalTime(0, 0), until = LocalTime(0, 0)),
        )
        assertEquals(24, slots.size) // well under MAX_SLOTS/iOS's 64 limit
        assertTrue(slots.size <= ReminderSlots.MAX_SLOTS)
    }

    // --- shouldNotify ---

    @Test
    fun firesInsideWindowWhenNothingRecorded() {
        assertTrue(ReminderSlots.shouldNotify(at(10), prefs(), null, isAppForeground = false))
    }

    @Test
    fun neverFiresWhenDisabled() {
        assertFalse(
            ReminderSlots.shouldNotify(at(10), prefs(enabled = false), null, isAppForeground = false),
        )
    }

    @Test
    fun neverFiresInForeground() {
        assertTrue(ReminderSlots.shouldNotify(at(10), prefs(), null, isAppForeground = false))
        assertFalse(ReminderSlots.shouldNotify(at(10), prefs(), null, isAppForeground = true))
    }

    @Test
    fun quietHoursSuppressOutsideWindow() {
        assertFalse(ReminderSlots.shouldNotify(at(3), prefs(), null, isAppForeground = false))
        assertFalse(ReminderSlots.shouldNotify(at(22), prefs(), null, isAppForeground = false))
    }

    @Test
    fun overnightWindowFiresAfterMidnight() {
        val overnight = prefs(from = LocalTime(21, 0), until = LocalTime(8, 0))
        assertTrue(ReminderSlots.shouldNotify(at(2), overnight, null, isAppForeground = false))
        assertFalse(ReminderSlots.shouldNotify(at(12), overnight, null, isAppForeground = false))
    }

    @Test
    fun recordInCurrentSlotSuppresses() {
        // now = 10:30 → current slot started 10:00; a 10:05 reading suppresses.
        assertFalse(
            ReminderSlots.shouldNotify(at(10, 30), prefs(), at(10, 5), isAppForeground = false),
        )
    }

    @Test
    fun recordInPreviousSlotDoesNotSuppress() {
        assertTrue(
            ReminderSlots.shouldNotify(at(10, 30), prefs(), at(9, 55), isAppForeground = false),
        )
    }

    @Test
    fun skipDisabledFiresEvenAfterRecentRecord() {
        assertTrue(
            ReminderSlots.shouldNotify(
                at(10, 30),
                prefs(skipIfRecorded = false),
                at(10, 5),
                isAppForeground = false,
            ),
        )
    }

    // --- nextSlotAfter ---

    @Test
    fun nextSlotWithinWindow() {
        assertEquals(at(10), ReminderSlots.nextSlotAfter(at(9), prefs()))
        assertEquals(at(10), ReminderSlots.nextSlotAfter(at(8, 1), prefs()))
    }

    @Test
    fun exactSlotTimeMovesToTheFollowingSlot() {
        assertEquals(at(12), ReminderSlots.nextSlotAfter(at(10), prefs()))
    }

    @Test
    fun afterLastSlotRollsToTomorrowsFirst() {
        val next = ReminderSlots.nextSlotAfter(at(20, 30), prefs())
        assertEquals(LocalDateTime(LocalDate(2026, 7, 6), LocalTime(8, 0)), next)
    }

    @Test
    fun beforeWindowStartsPicksTodaysFirstSlot() {
        assertEquals(at(8), ReminderSlots.nextSlotAfter(at(6), prefs()))
    }

    @Test
    fun overnightNextSlotCrossesMidnight() {
        val overnight = prefs(from = LocalTime(21, 0), until = LocalTime(8, 0))
        assertEquals(
            LocalDateTime(LocalDate(2026, 7, 6), LocalTime(1, 0)),
            ReminderSlots.nextSlotAfter(at(23, 30), overnight),
        )
    }

    @Test
    fun overnightMorningGapWaitsForEvening() {
        val overnight = prefs(from = LocalTime(21, 0), until = LocalTime(8, 0))
        // 07:30 → last morning slot was 07:00; next is tonight 21:00.
        assertEquals(at(21), ReminderSlots.nextSlotAfter(at(7, 30), overnight))
    }
}
