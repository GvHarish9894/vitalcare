package com.techgv.vitalcare.core.di

import com.techgv.vitalcare.data.backup.IosBackupScheduler
import com.techgv.vitalcare.data.reminders.ReminderNotificationDelegate
import com.techgv.vitalcare.domain.backup.DriveConfig
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Swift-friendly entry point: `KoinIOSKt.doInitKoin()` from iOSApp.init.
 * Drive stays disabled until a contributor wires GoogleSignIn + a client ID
 * (D-027); flip [driveEnabled] alongside that work.
 */
fun doInitKoin(driveEnabled: Boolean = false) {
    initKoin {
        modules(module { single { DriveConfig(enabled = driveEnabled) } })
    }
    // BGTask handlers must be registered before app launch completes (D-022).
    val koin = KoinPlatform.getKoin()
    koin.get<IosBackupScheduler>().registerBackgroundTask()
    // Reminder taps + foreground suppression (D-032); the delegate is a Koin
    // single because the center only keeps a weak reference.
    UNUserNotificationCenter.currentNotificationCenter().delegate =
        koin.get<ReminderNotificationDelegate>()
}
