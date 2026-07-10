package com.techgv.vitalcare.domain.model

import kotlinx.datetime.LocalTime

/**
 * Vitals reminder preferences (D-032). Default OFF — the app never asks for
 * notification permission until the user turns reminders on.
 *
 * Slots are clock-aligned: activeFrom, activeFrom+interval, … while inside
 * the active window. activeFrom == activeUntil means all day; activeUntil
 * before activeFrom is an overnight window that wraps midnight.
 */
data class ReminderPreferences(
    val enabled: Boolean = false,
    val intervalHours: Int = 2,
    val activeFrom: LocalTime = LocalTime(8, 0),
    val activeUntil: LocalTime = LocalTime(21, 0),
    /** Don't remind when a reading was already saved in the current slot. */
    val skipIfRecorded: Boolean = true,
) {
    companion object {
        /** Interval presets offered in Settings. */
        val INTERVAL_PRESETS = listOf(1, 2, 4, 6, 12)
    }
}
