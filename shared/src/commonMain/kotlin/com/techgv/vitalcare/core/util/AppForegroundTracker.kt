package com.techgv.vitalcare.core.util

/**
 * Whether the app UI is currently in front of the user. Set by the platform
 * entry points (MainActivity onResume/onPause; iOS suppresses foreground
 * banners in the notification delegate instead). Used so reminders never
 * fire while the user is already in the app (03 §1 — never nag).
 */
object AppForegroundTracker {
    @Volatile
    var isForeground: Boolean = false
}
