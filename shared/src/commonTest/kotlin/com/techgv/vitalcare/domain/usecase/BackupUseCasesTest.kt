package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeVitalsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppInfo
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.data.backup.BackupFile
import com.techgv.vitalcare.data.backup.BackupSerializer
import com.techgv.vitalcare.data.backup.toBackupDto
import com.techgv.vitalcare.domain.backup.BackupRemote
import com.techgv.vitalcare.domain.backup.BackupScheduler
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.model.AutoBackupCadence
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeSettingsRepository : SettingsRepository {
    override val theme = MutableStateFlow(ThemePreference.SYSTEM)
    override val profileName = MutableStateFlow("")
    override val telemetryEnabled = MutableStateFlow(true)
    override val driveConnected = MutableStateFlow(false)
    override val lastBackupAt = MutableStateFlow(0L)
    override val autoBackupCadence = MutableStateFlow(AutoBackupCadence.OFF)

    override fun setTheme(value: ThemePreference) { theme.value = value }
    override fun setProfileName(value: String) { profileName.value = value }
    override fun setTelemetryEnabled(value: Boolean) { telemetryEnabled.value = value }
    override fun setDriveConnected(value: Boolean) { driveConnected.value = value }
    override fun setLastBackupAt(value: Long) { lastBackupAt.value = value }
    override fun setAutoBackupCadence(value: AutoBackupCadence) { autoBackupCadence.value = value }
}

private class FakeAuthorizer(
    var token: AppResult<String> = AppResult.Success("token-1"),
) : DriveAuthorizer {
    override val isAvailable = true
    var revoked = false
    override suspend fun authorize(interactive: Boolean): AppResult<String> = token
    override suspend fun revoke(): AppResult<Unit> { revoked = true; return AppResult.Success(Unit) }
}

private class FakeRemote : BackupRemote {
    var stored: String? = null
    var failUpload = false
    override suspend fun upload(accessToken: String, content: String): AppResult<Unit> {
        if (failUpload) return AppResult.Failure(AppError.Network)
        stored = content
        return AppResult.Success(Unit)
    }
    override suspend fun download(accessToken: String): AppResult<String?> =
        AppResult.Success(stored)
}

private class FakeScheduler : BackupScheduler {
    var scheduled: AutoBackupCadence? = null
    var cancelled = false
    override fun schedule(cadence: AutoBackupCadence) { scheduled = cadence }
    override fun cancel() { cancelled = true }
}

class BackupUseCasesTest {

    private val vitals = FakeVitalsRepository()
    private val settings = FakeSettingsRepository()
    private val authorizer = FakeAuthorizer()
    private val remote = FakeRemote()
    private val scheduler = FakeScheduler()
    private val serializer = BackupSerializer()

    private fun backupNow() = BackupNow(
        vitals, settings, serializer, remote, authorizer, AppInfo("1.0"), Fixtures.clock,
    )

    private fun restore() = RestoreFromDrive(
        vitals, serializer, remote, authorizer, MergeBackupRecords(),
    )

    @Test
    fun backupThenRestoreRoundTripsLosslessly() = runTest {
        vitals.seed(
            Fixtures.record(id = "a", remarks = "Morning, resting"),
            Fixtures.record(id = "b", spo2 = null, heartRate = 68, systolic = null, diastolic = null),
        )
        assertIs<AppResult.Success<Unit>>(backupNow()(interactive = true))
        val before = vitals.current().toSet()

        // Wipe local and restore — every record must come back identical.
        vitals.seed()
        val restored = assertIs<AppResult.Success<Int>>(restore()())

        assertEquals(2, restored.value)
        assertEquals(before, vitals.current().toSet())
    }

    @Test
    fun backupRecordsLastBackupTime() = runTest {
        vitals.seed(Fixtures.record(id = "a"))
        assertIs<AppResult.Success<Unit>>(backupNow()(interactive = true))
        assertEquals(Fixtures.nowEpochMillis, settings.lastBackupAt.value)
    }

    @Test
    fun failedUploadLeavesLastBackupUntouched() = runTest {
        remote.failUpload = true
        vitals.seed(Fixtures.record(id = "a"))

        val failure = assertIs<AppResult.Failure>(backupNow()(interactive = true))

        assertEquals(AppError.Network, failure.error)
        assertEquals(0L, settings.lastBackupAt.value)
    }

    @Test
    fun restoreIsIdempotentAndNonDestructive() = runTest {
        vitals.seed(Fixtures.record(id = "backup-1", updatedAt = 100L))
        assertIs<AppResult.Success<Unit>>(backupNow()(interactive = true))

        // Local gains a newer edit + a local-only record after the backup.
        vitals.seed(
            Fixtures.record(id = "backup-1", spo2 = 91, updatedAt = 500L),
            Fixtures.record(id = "local-only"),
        )

        val first = assertIs<AppResult.Success<Int>>(restore()())
        val second = assertIs<AppResult.Success<Int>>(restore()())

        assertEquals(0, first.value) // older snapshot never overwrites newer local
        assertEquals(0, second.value) // idempotent
        assertEquals(setOf("backup-1", "local-only"), vitals.current().map { it.id }.toSet())
        assertEquals(91, vitals.current().first { it.id == "backup-1" }.spo2)
    }

    @Test
    fun restoreWithNoBackupFailsWithNotFound() = runTest {
        val failure = assertIs<AppResult.Failure>(restore()())
        assertEquals(AppError.NotFound, failure.error)
    }

    @Test
    fun restoreRejectsNewerSchemaVersion() = runTest {
        remote.stored = serializer.encode(
            BackupFile(
                schemaVersion = 1,
                exportedAt = 1L,
                appVersion = "9.9",
                records = listOf(Fixtures.record(id = "x").toBackupDto()),
            ),
        ).replace("\"schemaVersion\":1", "\"schemaVersion\":42")

        val failure = assertIs<AppResult.Failure>(restore()())
        assertEquals(AppError.UnsupportedBackup(42), failure.error)
    }

    @Test
    fun connectStoresConnectedFlagAndDisconnectClearsEverything() = runTest {
        assertIs<AppResult.Success<Unit>>(ConnectDrive(authorizer, settings)())
        assertTrue(settings.driveConnected.value)

        settings.setAutoBackupCadence(AutoBackupCadence.DAILY)
        assertIs<AppResult.Success<Unit>>(DisconnectDrive(authorizer, settings, scheduler)())

        assertTrue(authorizer.revoked)
        assertEquals(false, settings.driveConnected.value)
        assertEquals(AutoBackupCadence.OFF, settings.autoBackupCadence.value)
        assertTrue(scheduler.cancelled)
    }

    @Test
    fun setAutoBackupSchedulesAndCancels() {
        val setAutoBackup = SetAutoBackup(settings, scheduler)

        setAutoBackup(AutoBackupCadence.WEEKLY)
        assertEquals(AutoBackupCadence.WEEKLY, scheduler.scheduled)

        setAutoBackup(AutoBackupCadence.OFF)
        assertTrue(scheduler.cancelled)
    }

    @Test
    fun connectFailsCleanlyWhenAuthorizationDenied() = runTest {
        authorizer.token = AppResult.Failure(AppError.DriveAuth)

        val failure = assertIs<AppResult.Failure>(ConnectDrive(authorizer, settings)())

        assertEquals(AppError.DriveAuth, failure.error)
        assertEquals(false, settings.driveConnected.value)
    }
}
