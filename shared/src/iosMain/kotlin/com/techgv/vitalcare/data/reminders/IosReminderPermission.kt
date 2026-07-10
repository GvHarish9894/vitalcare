package com.techgv.vitalcare.data.reminders

import com.techgv.vitalcare.domain.reminders.ReminderPermission
import com.techgv.vitalcare.domain.reminders.ReminderPermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

class IosReminderPermission : ReminderPermission {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun status(): ReminderPermissionStatus =
        suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                val granted = when (settings?.authorizationStatus) {
                    UNAuthorizationStatusAuthorized,
                    UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral,
                    -> true
                    else -> false // NotDetermined or Denied → request/banner flows
                }
                continuation.resume(
                    if (granted) ReminderPermissionStatus.GRANTED
                    else ReminderPermissionStatus.DENIED,
                )
            }
        }

    override suspend fun request(): Boolean = suspendCancellableCoroutine { continuation ->
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ ->
            continuation.resume(granted)
        }
    }

    override fun openSystemSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
