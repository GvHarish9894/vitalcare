package com.techgv.vitalcare.data.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.techgv.vitalcare.domain.backup.BackupProgressReporter
import com.techgv.vitalcare.shared.R

/**
 * Shows a low-importance progress notification for a manual "Back up now"
 * (D-032-style visibility). "Backing up…" is ongoing + indeterminate; success
 * flips to a self-dismissing "Backed up ✓", failure stays until dismissed.
 * Silently no-ops when notifications aren't permitted — the in-app spinner
 * still covers the case.
 */
class AndroidBackupProgressReporter(private val context: Context) : BackupProgressReporter {

    override fun running() {
        post(context.getString(R.string.backup_notification_running), ongoing = true, autoDismiss = false)
    }

    override fun finished(success: Boolean) {
        val message = context.getString(
            if (success) R.string.backup_notification_done else R.string.backup_notification_failed,
        )
        post(message, ongoing = false, autoDismiss = success)
    }

    private fun post(message: String, ongoing: Boolean, autoDismiss: Boolean) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(R.string.backup_notification_title))
            .setContentText(message)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (ongoing) builder.setProgress(0, 0, true)
        if (autoDismiss) builder.setTimeoutAfter(SUCCESS_TIMEOUT_MS)
        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission raced away between check and notify — drop quietly.
        }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.backup_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "backup_progress"
        const val NOTIFICATION_ID = 1002
        const val SUCCESS_TIMEOUT_MS = 6_000L
    }
}
