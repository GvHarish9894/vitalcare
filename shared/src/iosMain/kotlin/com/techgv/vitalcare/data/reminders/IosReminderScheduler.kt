package com.techgv.vitalcare.data.reminders

import com.techgv.vitalcare.domain.model.ReminderPreferences
import com.techgv.vitalcare.domain.reminders.ReminderScheduler
import com.techgv.vitalcare.domain.reminders.ReminderSlots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.app_name
import vitalcare.shared.generated.resources.notification_reminder_text

/**
 * One repeating UNCalendarNotificationTrigger per daily slot (D-032):
 * clock-aligned, honors quiet hours by construction, survives reboot and app
 * kill, self-heals across DST/timezone changes. Slot count is capped at
 * [ReminderSlots.MAX_SLOTS] — under iOS's 64 pending-request limit.
 *
 * skipIfRecorded is best-effort on iOS: local notifications cannot run app
 * code at fire time, so the foreground suppression in the delegate is the
 * only reliable in-the-moment gate (documented platform limitation).
 */
class IosReminderScheduler : ReminderScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun apply(preferences: ReminderPreferences) {
        if (!preferences.enabled) return cancelAll()
        scope.launch {
            val title = getString(Res.string.app_name)
            val body = getString(Res.string.notification_reminder_text)
            val slots = ReminderSlots.slotsFor(preferences)
            removeReminderRequests { scheduleSlots(slots, title, body) }
        }
    }

    override fun cancelAll() {
        removeReminderRequests { }
    }

    /** See class KDoc — fire-time skipping is not possible for iOS local notifications. */
    override fun onVitalsRecorded() = Unit

    private fun scheduleSlots(slots: List<LocalTime>, title: String, body: String) {
        slots.forEach { slot ->
            val components = NSDateComponents().apply {
                hour = slot.hour.toLong()
                minute = slot.minute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = components,
                repeats = true,
            )
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body) // PHI-free by construction (§07/6)
                setSound(UNNotificationSound.defaultSound)
            }
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = identifierFor(slot),
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    /** Removes only our reminder requests (never other notification types). */
    private fun removeReminderRequests(then: () -> Unit) {
        center.getPendingNotificationRequestsWithCompletionHandler { pending ->
            val ids = pending.orEmpty()
                .filterIsInstance<UNNotificationRequest>()
                .map { it.identifier }
                .filter { it.startsWith(ID_PREFIX) }
            if (ids.isNotEmpty()) {
                center.removePendingNotificationRequestsWithIdentifiers(ids)
            }
            then()
        }
    }

    private fun identifierFor(slot: LocalTime): String =
        "$ID_PREFIX${slot.hour.toString().padStart(2, '0')}${slot.minute.toString().padStart(2, '0')}"

    companion object {
        const val ID_PREFIX = "vitalcare.reminder."
    }
}
