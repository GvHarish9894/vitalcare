package com.techgv.vitalcare.data.reminders

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.techgv.vitalcare.domain.reminders.ReminderPermission
import com.techgv.vitalcare.domain.reminders.ReminderPermissionStatus
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * POST_NOTIFICATIONS bridge (API 33+). The system dialog needs an Activity,
 * so MainActivity registers a launcher and wires [requestLauncher] /
 * [onPermissionResult] — same pattern as AndroidDriveAuthorizer. `status()`
 * also covers the app-level notifications toggle and a silenced channel, so
 * a revoke from device settings is detected on the next resume refresh.
 */
class AndroidReminderPermission(private val context: Context) : ReminderPermission {

    /** Set by MainActivity while alive; launches the system permission dialog. */
    var requestLauncher: ((String) -> Unit)? = null

    private var pendingRequest: CancellableContinuation<Boolean>? = null

    override suspend fun status(): ReminderPermissionStatus {
        val notificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                !channelSilenced()
        return when {
            !notificationsEnabled -> ReminderPermissionStatus.DENIED
            Build.VERSION.SDK_INT >= 33 -> ReminderPermissionStatus.GRANTED
            else -> ReminderPermissionStatus.NOT_REQUIRED
        }
    }

    override suspend fun request(): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            // No runtime permission to ask for — the app-level toggle can only
            // be flipped in system settings (the blocked-state banner handles it).
            return status() != ReminderPermissionStatus.DENIED
        }
        val launcher = requestLauncher ?: return false
        return suspendCancellableCoroutine { continuation ->
            pendingRequest = continuation
            continuation.invokeOnCancellation { pendingRequest = null }
            launcher(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Called by MainActivity with the dialog result. */
    fun onPermissionResult(granted: Boolean) {
        pendingRequest?.resume(granted)
        pendingRequest = null
    }

    override fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun channelSilenced(): Boolean {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(ReminderNotifications.CHANNEL_ID)
            ?: return false // channel is created lazily; absent = not silenced
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }
}
