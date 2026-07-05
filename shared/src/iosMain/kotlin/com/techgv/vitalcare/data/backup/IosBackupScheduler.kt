package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.backup.BackupScheduler
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.usecase.BackupNow
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

/**
 * BGTaskScheduler-backed auto-backup (D-022). The task identifier must be
 * listed under BGTaskSchedulerPermittedIdentifiers in Info.plist, and
 * [registerBackgroundTask] must run before app launch finishes (doInitKoin).
 * iOS treats the schedule as a hint — runs are opportunistic.
 */
class IosBackupScheduler : BackupScheduler, KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(ExperimentalForeignApi::class)
    override fun schedule(cadence: AutoBackupCadence) {
        val seconds = when (cadence) {
            AutoBackupCadence.OFF -> return cancel()
            AutoBackupCadence.DAILY -> DAY_SECONDS
            AutoBackupCadence.WEEKLY -> 7 * DAY_SECONDS
            AutoBackupCadence.MONTHLY -> 30 * DAY_SECONDS
        }
        val request = BGAppRefreshTaskRequest(identifier = TASK_IDENTIFIER)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(seconds)
        // Fails on simulator / when the identifier isn't permitted — harmless.
        BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
    }

    override fun cancel() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
    }

    /** Must be called before didFinishLaunching returns (done in doInitKoin). */
    fun registerBackgroundTask() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = TASK_IDENTIFIER,
            usingQueue = null,
        ) { task ->
            val refreshTask = task as? BGAppRefreshTask
            scope.launch {
                val result = get<BackupNow>().invoke(interactive = false)
                refreshTask?.setTaskCompletedWithSuccess(result is AppResult.Success<*>)
            }
        }
    }

    companion object {
        const val TASK_IDENTIFIER = "com.techgv.vitalcare.backup"
        private const val DAY_SECONDS = 24.0 * 60 * 60
    }
}
