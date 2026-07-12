package com.techgv.vitalcare.core.di

import android.content.Context
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.techgv.vitalcare.core.telemetry.AndroidTelemetry
import com.techgv.vitalcare.core.telemetry.Telemetry
import com.techgv.vitalcare.core.util.AppInfo
import com.techgv.vitalcare.data.backup.AndroidBackupScheduler
import com.techgv.vitalcare.data.backup.AndroidDriveAuthorizer
import com.techgv.vitalcare.data.backup.AndroidFileExporter
import com.techgv.vitalcare.data.backup.FileExporter
import com.techgv.vitalcare.data.local.VitalCareDatabase
import com.techgv.vitalcare.data.local.databaseBuilder
import com.techgv.vitalcare.data.backup.AndroidBackupProgressReporter
import com.techgv.vitalcare.data.reminders.AndroidReminderPermission
import com.techgv.vitalcare.data.reminders.AndroidReminderScheduler
import com.techgv.vitalcare.data.reminders.ReminderNotifications
import com.techgv.vitalcare.domain.backup.BackupProgressReporter
import com.techgv.vitalcare.domain.backup.BackupScheduler
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.reminders.ReminderPermission
import com.techgv.vitalcare.domain.reminders.ReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<Telemetry> { AndroidTelemetry(androidContext()) }
    single<RoomDatabase.Builder<VitalCareDatabase>> { databaseBuilder(androidContext()) }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("vitalcare_settings", Context.MODE_PRIVATE),
        )
    }
    single<FileExporter> { AndroidFileExporter(androidContext(), get()) }
    single { AndroidDriveAuthorizer(androidContext(), get()) } bind DriveAuthorizer::class
    single<BackupScheduler> { AndroidBackupScheduler(androidContext()) }
    single<BackupProgressReporter> { AndroidBackupProgressReporter(androidContext()) }
    single { ReminderNotifications(androidContext()) }
    single { AndroidReminderPermission(androidContext()) } bind ReminderPermission::class
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single {
        val context = androidContext()
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
        AppInfo(versionName = versionName ?: "1.0")
    }
}
