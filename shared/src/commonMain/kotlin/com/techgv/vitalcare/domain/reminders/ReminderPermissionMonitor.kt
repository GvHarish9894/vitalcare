package com.techgv.vitalcare.domain.reminders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable notification-permission state. Refreshed on every app resume
 * (the user may have flipped the permission in device settings) and after
 * each enable/request flow — the "verify after user enabled from device
 * settings" callback of D-032.
 */
class ReminderPermissionMonitor(private val permission: ReminderPermission) {

    private val _status = MutableStateFlow(ReminderPermissionStatus.NOT_REQUIRED)
    val status: StateFlow<ReminderPermissionStatus> = _status.asStateFlow()

    /** Re-reads the platform status; returns the fresh value. */
    suspend fun refresh(): ReminderPermissionStatus {
        val fresh = permission.status()
        _status.value = fresh
        return fresh
    }
}
