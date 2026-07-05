package com.techgv.vitalcare.domain.model

/** Auto-backup schedule (FR-B4, D-022). OFF is the default — nothing runs in the background. */
enum class AutoBackupCadence {
    OFF,
    DAILY,
    WEEKLY,
    MONTHLY,
}
