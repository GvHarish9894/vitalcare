package com.techgv.vitalcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.techgv.vitalcare.core.navigation.PendingNavigation
import com.techgv.vitalcare.core.util.AppForegroundTracker
import com.techgv.vitalcare.data.backup.AndroidDriveAuthorizer
import com.techgv.vitalcare.data.reminders.AndroidReminderPermission
import com.techgv.vitalcare.data.reminders.ReminderNotifications
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val driveAuthorizer: AndroidDriveAuthorizer by inject()
    private val reminderPermission: AndroidReminderPermission by inject()
    private val pendingNavigation: PendingNavigation by inject()

    // Google consent UI for Drive authorization (D-021).
    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        driveAuthorizer.onAuthorizationResult(result.data)
    }

    // POST_NOTIFICATIONS dialog for reminders (D-032) — requested lazily,
    // only when the user enables the reminder toggle.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        reminderPermission.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        driveAuthorizer.resolutionLauncher = { pendingIntent ->
            authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }
        reminderPermission.requestLauncher = { permission ->
            notificationPermissionLauncher.launch(permission)
        }
        handleOpenTarget(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenTarget(intent)
    }

    override fun onResume() {
        super.onResume()
        AppForegroundTracker.isForeground = true
    }

    override fun onPause() {
        AppForegroundTracker.isForeground = false
        super.onPause()
    }

    override fun onDestroy() {
        driveAuthorizer.resolutionLauncher = null
        reminderPermission.requestLauncher = null
        super.onDestroy()
    }

    /** Reminder-notification tap → shared PendingNavigation → Record Vitals. */
    private fun handleOpenTarget(intent: Intent?) {
        if (intent?.getStringExtra(ReminderNotifications.EXTRA_OPEN) ==
            ReminderNotifications.TARGET_RECORD_VITALS
        ) {
            intent.removeExtra(ReminderNotifications.EXTRA_OPEN)
            pendingNavigation.requestRecordVitals()
        }
    }
}
