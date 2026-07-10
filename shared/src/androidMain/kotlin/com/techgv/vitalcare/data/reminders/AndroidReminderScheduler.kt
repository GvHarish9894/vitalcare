package com.techgv.vitalcare.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techgv.vitalcare.core.util.AppForegroundTracker
import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.data.local.VitalRecordDao
import com.techgv.vitalcare.data.settings.AppSettings
import com.techgv.vitalcare.domain.model.ReminderPreferences
import com.techgv.vitalcare.domain.reminders.ReminderScheduler
import com.techgv.vitalcare.domain.reminders.ReminderSlots
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.app_name
import vitalcare.shared.generated.resources.notification_reminder_text
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Self-rechaining one-shot WorkManager chain (D-032): each run fires (or
 * skips, per the shared `shouldNotify` gate) and enqueues the next run at the
 * following clock-aligned slot. Chosen over PeriodicWork (can't clock-align
 * or honor quiet hours) and AlarmManager (exact-alarm permission friction);
 * WorkManager persists across reboots, and app-start sync re-chains after a
 * force-stop. Timing is intentionally inexact (± minutes under Doze).
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler, KoinComponent {

    override fun apply(preferences: ReminderPreferences) {
        if (!preferences.enabled) return cancelAll()
        enqueueNext(context, preferences, get(), get())
    }

    override fun cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Fire-time DB check already makes skip-if-recorded exact on Android. */
    override fun onVitalsRecorded() = Unit

    companion object {
        const val WORK_NAME = "vitalcare-reminder"

        internal fun enqueueNext(
            context: Context,
            preferences: ReminderPreferences,
            clock: Clock,
            timeZone: TimeZone,
        ) {
            val now = clock.nowLocal(timeZone)
            val next = ReminderSlots.nextSlotAfter(now, preferences)
            val delayMillis =
                (next.toInstant(timeZone) - now.toInstant(timeZone)).inWholeMilliseconds
            val request = OneTimeWorkRequestBuilder<VitalsReminderWorker>()
                .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

class VitalsReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    override suspend fun doWork(): Result {
        val preferences = get<AppSettings>().reminderPreferences.value
        if (!preferences.enabled) return Result.success() // disabled since scheduling — stop chain

        val clock = get<Clock>()
        val timeZone = get<TimeZone>()
        val now = clock.nowLocal(timeZone)

        if (ReminderSlots.shouldNotify(
                now = now,
                preferences = preferences,
                lastRecordedAt = newestRecordedAt(),
                isAppForeground = AppForegroundTracker.isForeground,
            )
        ) {
            get<ReminderNotifications>().show(
                title = getString(Res.string.app_name),
                text = getString(Res.string.notification_reminder_text),
            )
        }

        AndroidReminderScheduler.enqueueNext(applicationContext, preferences, clock, timeZone)
        return Result.success()
    }

    private suspend fun newestRecordedAt(): LocalDateTime? =
        get<VitalRecordDao>().getNewest()?.let { entity ->
            LocalDateTime(LocalDate.parse(entity.date), LocalTime.parse(entity.time))
        }
}
