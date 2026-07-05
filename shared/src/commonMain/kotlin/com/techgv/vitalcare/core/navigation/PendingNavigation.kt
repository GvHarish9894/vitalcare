package com.techgv.vitalcare.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform notification-tap routing (D-032): both platforms funnel
 * "open Record Vitals" here instead of using per-platform deep-link APIs.
 * The StateFlow buffers the request across cold starts — Android sets it
 * from MainActivity intent extras, iOS from the notification-center
 * delegate; `App()` consumes it once the NavHost is up.
 */
class PendingNavigation {

    enum class Target { RECORD_VITALS }

    private val _target = MutableStateFlow<Target?>(null)
    val target: StateFlow<Target?> = _target.asStateFlow()

    fun requestRecordVitals() {
        _target.value = Target.RECORD_VITALS
    }

    fun consume() {
        _target.value = null
    }
}
