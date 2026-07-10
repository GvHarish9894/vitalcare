package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppInfo
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.nowEpochMillis
import com.techgv.vitalcare.data.backup.BackupFile
import com.techgv.vitalcare.data.backup.BackupSerializer
import com.techgv.vitalcare.data.backup.toBackupDto
import com.techgv.vitalcare.data.backup.toDomain
import com.techgv.vitalcare.domain.backup.BackupProgressReporter
import com.techgv.vitalcare.domain.backup.BackupRemote
import com.techgv.vitalcare.domain.backup.BackupScheduler
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.repository.FluidRepository
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

/** Connect Google Drive (FR-B2): interactive consent for `drive.file` only (D-021). */
class ConnectDrive(
    private val authorizer: DriveAuthorizer,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val token = when (val result = authorizer.authorize(interactive = true)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        if (token.isBlank()) return AppResult.Failure(AppError.DriveAuth)
        settings.setDriveConnected(true)
        return AppResult.Success(Unit)
    }
}

/** Disconnect Drive (FR-B6): revoke, forget state, cancel auto-backup. */
class DisconnectDrive(
    private val authorizer: DriveAuthorizer,
    private val settings: SettingsRepository,
    private val scheduler: BackupScheduler,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        authorizer.revoke() // best effort — local state is cleared regardless
        settings.setDriveConnected(false)
        settings.setAutoBackupCadence(AutoBackupCadence.OFF)
        scheduler.cancel()
        return AppResult.Success(Unit)
    }
}

/** Upload a full snapshot to Drive's appDataFolder (FR-B3, 05 §5). */
class BackupNow(
    private val vitalsRepository: VitalsRepository,
    private val fluidsRepository: FluidRepository,
    private val settings: SettingsRepository,
    private val serializer: BackupSerializer,
    private val remote: BackupRemote,
    private val authorizer: DriveAuthorizer,
    private val appInfo: AppInfo,
    private val clock: Clock,
    private val progress: BackupProgressReporter,
) {
    suspend operator fun invoke(interactive: Boolean = true): AppResult<Unit> {
        val records = when (val snapshot = vitalsRepository.getAll()) {
            is AppResult.Success -> snapshot.value
            is AppResult.Failure -> return snapshot
        }
        val fluids = when (val snapshot = fluidsRepository.getAll()) {
            is AppResult.Success -> snapshot.value
            is AppResult.Failure -> return snapshot
        }
        val document = serializer.encode(
            BackupFile(
                schemaVersion = BackupSerializer.SCHEMA_VERSION,
                exportedAt = clock.nowEpochMillis(),
                appVersion = appInfo.versionName,
                profileName = settings.profileName.value.ifBlank { null },
                records = records.map { it.toBackupDto() },
                fluids = fluids.map { it.toBackupDto() },
            ),
        )
        val token = when (val auth = authorizer.authorize(interactive)) {
            is AppResult.Success -> auth.value
            is AppResult.Failure -> return auth // pre-upload; no progress notification shown
        }
        // Only manual backups get a visible progress notification (03 §6).
        if (interactive) progress.running()
        return when (val uploaded = remote.upload(token, document)) {
            is AppResult.Success -> {
                settings.setLastBackupAt(clock.nowEpochMillis())
                if (interactive) progress.finished(success = true)
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> {
                if (interactive) progress.finished(success = false)
                uploaded
            }
        }
    }
}

/**
 * Download and merge the Drive backup (FR-B5, D-024): union by id, newer
 * `updatedAt` wins, never deletes local rows. Returns how many records were
 * added or updated (idempotent — restoring twice returns 0 the second time).
 */
class RestoreFromDrive(
    private val vitalsRepository: VitalsRepository,
    private val fluidsRepository: FluidRepository,
    private val serializer: BackupSerializer,
    private val remote: BackupRemote,
    private val authorizer: DriveAuthorizer,
    private val merge: MergeBackupRecords,
    private val mergeFluids: MergeFluidEntries,
) {
    suspend operator fun invoke(): AppResult<Int> {
        val token = when (val auth = authorizer.authorize(interactive = true)) {
            is AppResult.Success -> auth.value
            is AppResult.Failure -> return auth
        }
        val raw = when (val download = remote.download(token)) {
            is AppResult.Success -> download.value ?: return AppResult.Failure(AppError.NotFound)
            is AppResult.Failure -> return download
        }
        val backup = when (val decoded = serializer.decode(raw)) {
            is AppResult.Success -> decoded.value
            is AppResult.Failure -> return decoded
        }

        val localRecords = when (val snapshot = vitalsRepository.getAll()) {
            is AppResult.Success -> snapshot.value
            is AppResult.Failure -> return snapshot
        }
        val recordsToWrite = merge(local = localRecords, incoming = backup.records.map { it.toDomain() })
        if (recordsToWrite.isNotEmpty()) {
            when (val written = vitalsRepository.upsertAll(recordsToWrite)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return written
            }
        }

        val localFluids = when (val snapshot = fluidsRepository.getAll()) {
            is AppResult.Success -> snapshot.value
            is AppResult.Failure -> return snapshot
        }
        val fluidsToWrite = mergeFluids(local = localFluids, incoming = backup.fluids.map { it.toDomain() })
        if (fluidsToWrite.isNotEmpty()) {
            when (val written = fluidsRepository.upsertAll(fluidsToWrite)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> return written
            }
        }

        return AppResult.Success(recordsToWrite.size + fluidsToWrite.size)
    }
}

/** Pick the auto-backup cadence (FR-B4, D-022) and (re)schedule accordingly. */
class SetAutoBackup(
    private val settings: SettingsRepository,
    private val scheduler: BackupScheduler,
) {
    operator fun invoke(cadence: AutoBackupCadence) {
        settings.setAutoBackupCadence(cadence)
        if (cadence == AutoBackupCadence.OFF) scheduler.cancel() else scheduler.schedule(cadence)
    }
}

data class BackupStatus(
    val connected: Boolean,
    /** Epoch millis; 0 = never backed up. */
    val lastBackupAt: Long,
    /** Records changed since the last backup (06 §6). */
    val unbackedCount: Int,
)

/** Reactive backup status for the Settings section and Dashboard hint (FR-D3). */
class ObserveBackupStatus(
    private val settings: SettingsRepository,
    private val vitalsRepository: VitalsRepository,
    private val fluidsRepository: FluidRepository,
) {
    operator fun invoke(): Flow<BackupStatus> = combine(
        settings.driveConnected,
        settings.lastBackupAt,
        vitalsRepository.observeAll(),
        fluidsRepository.observeAll(),
    ) { connected, lastBackupAt, records, fluids ->
        BackupStatus(
            connected = connected,
            lastBackupAt = lastBackupAt,
            unbackedCount = records.count { it.updatedAt > lastBackupAt } +
                fluids.count { it.updatedAt > lastBackupAt },
        )
    }
}
