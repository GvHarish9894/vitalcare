package com.techgv.vitalcare.domain.reminders

import com.techgv.vitalcare.domain.model.ReminderPreferences
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Pure slot arithmetic for vitals reminders (D-032) — shared by the Android
 * worker and the iOS calendar-trigger scheduler, and fully unit-tested.
 *
 * All math is in civil (wall-clock) time on purpose: reminder slots are
 * "8:00, 10:00, …" facts, so DST shifts and timezone changes self-heal the
 * next time a delay is computed against the current zone.
 */
object ReminderSlots {

    /** Safety cap under iOS's 64 pending-notification limit. */
    const val MAX_SLOTS = 60

    private const val DAY_MINUTES = 24 * 60

    /**
     * Clock-aligned slot times for one window: activeFrom, +interval, …
     * while still inside the window. activeFrom == activeUntil = all day;
     * activeUntil < activeFrom wraps midnight. Always contains at least the
     * window-start slot (a window shorter than the interval still reminds once).
     */
    fun slotsFor(preferences: ReminderPreferences): List<LocalTime> =
        slotOffsetsMinutes(preferences).map { offset ->
            minutesOfDayToTime((preferences.activeFrom.toMinutes() + offset).mod(DAY_MINUTES))
        }

    /**
     * Whether a reminder should actually fire at [now]. The scheduler only
     * aims at slot times; this is the final gate (quiet hours after clock
     * changes, foreground suppression, skip-when-recorded).
     */
    fun shouldNotify(
        now: LocalDateTime,
        preferences: ReminderPreferences,
        lastRecordedAt: LocalDateTime?,
        isAppForeground: Boolean,
    ): Boolean {
        if (!preferences.enabled) return false
        if (isAppForeground) return false // user is already in the app — never nag (03 §1)
        if (!isWithinWindow(now.time, preferences)) return false
        if (preferences.skipIfRecorded && lastRecordedAt != null &&
            lastRecordedAt >= currentSlotStart(now, preferences) && lastRecordedAt <= now
        ) {
            return false
        }
        return true
    }

    /** The next slot strictly after [now] — possibly tomorrow's first slot. */
    fun nextSlotAfter(now: LocalDateTime, preferences: ReminderPreferences): LocalDateTime {
        val offsets = slotOffsetsMinutes(preferences)
        // Window instances can start yesterday (overnight windows), today, or
        // tomorrow; the earliest future slot across those covers every case.
        return (-1..1)
            .flatMap { dayOffset ->
                val windowStart = now.date
                    .plus(dayOffset, DateTimeUnit.DAY)
                    .atTime(preferences.activeFrom)
                offsets.map { windowStart.plusMinutesCivil(it) }
            }
            .filter { it > now }
            .min()
    }

    /** Start of the slot [now] falls in (assumes [now] is within the window). */
    internal fun currentSlotStart(
        now: LocalDateTime,
        preferences: ReminderPreferences,
    ): LocalDateTime {
        val windowStart = windowStartFor(now, preferences)
        val minutesIntoWindow = now.minutesSinceCivil(windowStart)
        val interval = preferences.intervalHours * 60
        val slotIndex = (minutesIntoWindow / interval).coerceAtLeast(0)
        return windowStart.plusMinutesCivil(slotIndex * interval)
    }

    internal fun isWithinWindow(time: LocalTime, preferences: ReminderPreferences): Boolean {
        val from = preferences.activeFrom
        val until = preferences.activeUntil
        return when {
            from == until -> true // all day
            from < until -> time in from..until
            else -> time >= from || time <= until // overnight wrap
        }
    }

    /** Minute offsets of every slot from the window start. */
    private fun slotOffsetsMinutes(preferences: ReminderPreferences): List<Int> {
        val interval = (preferences.intervalHours * 60).coerceAtLeast(1)
        val windowLength = windowLengthMinutes(preferences)
        val offsets = generateSequence(0) { it + interval }
            .takeWhile { it <= windowLength }
            .take(MAX_SLOTS)
            .toList()
        return offsets.ifEmpty { listOf(0) }
    }

    private fun windowLengthMinutes(preferences: ReminderPreferences): Int {
        val from = preferences.activeFrom.toMinutes()
        val until = preferences.activeUntil.toMinutes()
        return when {
            from == until -> DAY_MINUTES - 1 // all day: stop before wrapping onto the start
            from < until -> until - from
            else -> DAY_MINUTES - from + until // overnight
        }
    }

    /** Window-instance start containing [now] (may be yesterday for overnight windows). */
    private fun windowStartFor(
        now: LocalDateTime,
        preferences: ReminderPreferences,
    ): LocalDateTime {
        val startsYesterday = now.time < preferences.activeFrom
        val date = if (startsYesterday) now.date.plus(-1, DateTimeUnit.DAY) else now.date
        return date.atTime(preferences.activeFrom)
    }

    private fun LocalTime.toMinutes(): Int = hour * 60 + minute

    private fun minutesOfDayToTime(minutesOfDay: Int): LocalTime =
        LocalTime(hour = minutesOfDay / 60, minute = minutesOfDay % 60)

    /** Civil (DST-agnostic) minute addition with day carry. */
    internal fun LocalDateTime.plusMinutesCivil(minutes: Int): LocalDateTime {
        val total = time.toMinutes() + minutes
        val dayCarry = total.floorDiv(DAY_MINUTES)
        val minutesOfDay = total.mod(DAY_MINUTES)
        return date.plus(dayCarry, DateTimeUnit.DAY).atTime(minutesOfDayToTime(minutesOfDay))
    }

    private fun LocalDateTime.minutesSinceCivil(other: LocalDateTime): Int {
        val dayDiff = other.date.daysUntil(date)
        return dayDiff * DAY_MINUTES + (time.toMinutes() - other.time.toMinutes())
    }
}
