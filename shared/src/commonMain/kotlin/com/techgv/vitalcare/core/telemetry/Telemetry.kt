package com.techgv.vitalcare.core.telemetry

/**
 * Analytics + crash-reporting seam (D-028). Strictly PHI-free by construction:
 * callers pass screen names, event names, and primitive counts only — never
 * vital values, remarks, or the profile name (§07/6).
 *
 * The real Firebase-backed implementation lands with the telemetry phase; the
 * app ships with [NoOpTelemetry] until then so the seam (and the Settings
 * opt-out, D-029) is exercised from day one.
 */
interface Telemetry {
    fun logScreen(name: String)
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun recordError(throwable: Throwable)
    fun setEnabled(enabled: Boolean)
}

class NoOpTelemetry : Telemetry {
    override fun logScreen(name: String) = Unit
    override fun logEvent(name: String, params: Map<String, Any>) = Unit
    override fun recordError(throwable: Throwable) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}
