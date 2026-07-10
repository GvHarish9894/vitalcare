package com.techgv.vitalcare.data.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.techgv.vitalcare.shared.R

/**
 * Posts the vitals reminder notification (D-032). Content is always PHI-free
 * (§07/6) — never values, remarks, or the profile name. Tapping routes to
 * Record Vitals via a launcher intent extra that MainActivity forwards into
 * the shared PendingNavigation.
 */
class ReminderNotifications(private val context: Context) {

    fun show(title: String, text: String) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return // permission revoked mid-flight

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName) ?: return
        launchIntent
            .putExtra(EXTRA_OPEN, TARGET_RECORD_VITALS)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission raced away between the check and notify — drop quietly.
        }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val EXTRA_OPEN = "com.techgv.vitalcare.OPEN"
        const val TARGET_RECORD_VITALS = "record_vitals"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE = 2001
    }
}
