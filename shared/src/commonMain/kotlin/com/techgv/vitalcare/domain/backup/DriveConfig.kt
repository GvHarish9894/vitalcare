package com.techgv.vitalcare.domain.backup

/**
 * Whether this build ships a Drive OAuth client (D-027). Supplied by the app
 * entry points: Android reads `vitalcare.drive.enabled` from local.properties
 * via BuildConfig; iOS passes it from Swift. Defaults to false so a fresh
 * clone builds and runs with zero configuration.
 */
data class DriveConfig(val enabled: Boolean)
