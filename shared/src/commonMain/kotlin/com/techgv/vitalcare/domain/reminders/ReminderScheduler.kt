package com.techgv.vitalcare.domain.reminders

import com.techgv.vitalcare.domain.model.ReminderPreferences

/**
 * Platform scheduling seam for vitals reminders (D-032): a self-rechaining
 * WorkManager worker on Android, repeating UNCalendarNotificationTriggers on
 * iOS. Notification content is always PHI-free (§07/6).
 */
interface ReminderScheduler {

    /** (Re)schedules everything to match [preferences]. Idempotent. */
    fun apply(preferences: ReminderPreferences)

    fun cancelAll()

    /**
     * A reading was just saved — platforms may suppress the imminent
     * reminder. Exact on Android (fire-time DB check makes this a no-op);
     * best-effort on iOS (local notifications can't run code at fire time,
     * so the next occurrence is re-planned instead).
     */
    fun onVitalsRecorded()
}

enum class ReminderPermissionStatus {
    GRANTED,
    DENIED,
    /** Platform needs no runtime permission (Android < 13 with channel enabled). */
    NOT_REQUIRED,
}

/**
 * Notification-permission seam. [request] is called from exactly one place —
 * the moment the user enables reminders (lazy by design); it may show the
 * system dialog and suspends until the user answers.
 */
interface ReminderPermission {
    suspend fun status(): ReminderPermissionStatus

    /** Shows the system permission dialog when possible; returns granted. */
    suspend fun request(): Boolean

    /** Opens the app's notification settings in the OS Settings app. */
    fun openSystemSettings()
}
