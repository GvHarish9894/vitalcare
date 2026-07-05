package com.techgv.vitalcare.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.backup.BackupScheduler
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.usecase.BackupNow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.concurrent.TimeUnit

/** WorkManager-backed auto-backup (D-022): periodic, network-constrained. */
class AndroidBackupScheduler(private val context: Context) : BackupScheduler {

    override fun schedule(cadence: AutoBackupCadence) {
        val days = when (cadence) {
            AutoBackupCadence.OFF -> return cancel()
            AutoBackupCadence.DAILY -> 1L
            AutoBackupCadence.WEEKLY -> 7L
            AutoBackupCadence.MONTHLY -> 30L
        }
        val request = PeriodicWorkRequestBuilder<BackupWorker>(days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    internal companion object {
        const val WORK_NAME = "vitalcare-auto-backup"
    }
}

/** Runs a silent backup; consent must already be granted (never shows UI). */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    override suspend fun doWork(): Result =
        when (val result = get<BackupNow>().invoke(interactive = false)) {
            is AppResult.Success -> Result.success()
            is AppResult.Failure -> when (result.error) {
                is AppError.Network -> Result.retry()
                else -> Result.failure()
            }
        }
}
