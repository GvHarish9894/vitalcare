package com.techgv.vitalcare.domain.backup

import com.techgv.vitalcare.domain.model.AutoBackupCadence

/**
 * Platform scheduler seam for optional auto-backup (D-022): WorkManager on
 * Android, BGTaskScheduler on iOS. Only ever active when Drive is connected
 * and a cadence other than OFF is selected.
 */
interface BackupScheduler {
    fun schedule(cadence: AutoBackupCadence)
    fun cancel()
}
