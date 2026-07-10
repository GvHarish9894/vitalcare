package com.techgv.vitalcare.core.util

/**
 * Lightweight platform log for diagnosing opt-in network features (Drive
 * backup). Never logs PHI (§07/6) — only HTTP statuses, error bodies, and
 * exception types. Android → Logcat (warn); iOS → NSLog.
 */
expect fun debugLog(tag: String, message: String)
