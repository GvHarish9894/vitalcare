package com.techgv.vitalcare.core.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Firebase-backed [Telemetry] for Android (D-028). Analytics + Crashlytics only;
 * PHI-free by construction — callers pass screen/event names and primitive
 * counts, never vital values, remarks, or the profile name (§07/6).
 *
 * Firebase auto-initializes at process start via its ContentProvider using the
 * `google-services.json` merged by the Gradle plugin in `:androidApp`.
 */
class AndroidTelemetry(context: Context) : Telemetry {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logScreen(name: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, name) },
        )
    }

    override fun logEvent(name: String, params: Map<String, Any>) {
        analytics.logEvent(
            name,
            Bundle().apply {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putLong(key, value.toLong())
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Float -> putDouble(key, value.toDouble())
                        is Boolean -> putLong(key, if (value) 1L else 0L)
                        else -> putString(key, value.toString())
                    }
                }
            },
        )
    }

    override fun recordError(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    /** Honors the Settings opt-out (D-029): disables both collections when off. */
    override fun setEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }
}
