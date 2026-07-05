package com.techgv.vitalcare.data.reminders

import com.techgv.vitalcare.core.navigation.PendingNavigation
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionNone
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * Routes reminder taps into the shared [PendingNavigation] (→ Record Vitals)
 * and suppresses banners while the app is foregrounded (03 §1 — the user is
 * already in the app). Held as a Koin single: UNUserNotificationCenter keeps
 * its delegate weak, so someone must own a strong reference.
 */
class ReminderNotificationDelegate(
    private val pendingNavigation: PendingNavigation,
) : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        if (didReceiveNotificationResponse.notification.request.identifier
                .startsWith(IosReminderScheduler.ID_PREFIX)
        ) {
            pendingNavigation.requestRecordVitals()
        }
        withCompletionHandler()
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(UNNotificationPresentationOptionNone)
    }
}
