package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.domain.model.ReminderPreferences
import com.techgv.vitalcare.domain.reminders.ReminderPermission
import com.techgv.vitalcare.domain.reminders.ReminderPermissionMonitor
import com.techgv.vitalcare.domain.reminders.ReminderPermissionStatus
import com.techgv.vitalcare.domain.reminders.ReminderScheduler
import com.techgv.vitalcare.domain.repository.SettingsRepository

/**
 * Reconciles scheduled reminders with (preference intent × permission
 * status): the toggle stores intent, the feature only runs with permission
 * (D-032). Called on app start, on every resume-time permission refresh, and
 * after preference changes.
 */
class SyncReminders(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val monitor: ReminderPermissionMonitor,
) {
    suspend operator fun invoke() {
        val preferences = settings.reminderPreferences.value
        val status = monitor.refresh()
        val runnable = preferences.enabled && status != ReminderPermissionStatus.DENIED
        if (runnable) scheduler.apply(preferences) else scheduler.cancelAll()
    }
}

/**
 * Persists a preference change and re-syncs. Enabling is the ONE place the
 * notification permission is ever requested (lazy by design): if the user
 * denies, the preference stays ON but nothing schedules — the Dashboard
 * banner / Settings warning take over until permission is granted in the OS.
 */
class SetReminderPreferences(
    private val settings: SettingsRepository,
    private val permission: ReminderPermission,
    private val syncReminders: SyncReminders,
) {
    sealed interface Result {
        data object Applied : Result
        /** Preference saved, but notifications are blocked at the OS level. */
        data object PermissionDenied : Result
    }

    suspend operator fun invoke(preferences: ReminderPreferences): Result {
        val wasEnabled = settings.reminderPreferences.value.enabled
        settings.setReminderPreferences(preferences)

        var denied = false
        if (preferences.enabled && !wasEnabled &&
            permission.status() == ReminderPermissionStatus.DENIED
        ) {
            denied = !permission.request()
        }
        syncReminders()
        return if (preferences.enabled && denied) Result.PermissionDenied else Result.Applied
    }
}
