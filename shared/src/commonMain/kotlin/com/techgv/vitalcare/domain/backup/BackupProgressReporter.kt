package com.techgv.vitalcare.domain.backup

/**
 * Surfaces manual-backup progress outside the app UI: Android posts a
 * low-importance progress notification so the user sees "Backing up…" →
 * "Backed up ✓" even if they navigate away; iOS relies on the in-app spinner
 * (no ongoing-progress notification idiom) and no-ops.
 *
 * Only invoked for interactive "Back up now"; silent auto-backups stay quiet
 * (03 §6 — backup is passive, never a notification storm). The reporter owns
 * its own display strings so the domain layer stays resource-free.
 */
interface BackupProgressReporter {
    fun running()
    fun finished(success: Boolean)
}

/** iOS / any platform without progress notifications. */
class NoOpBackupProgressReporter : BackupProgressReporter {
    override fun running() = Unit
    override fun finished(success: Boolean) = Unit
}
