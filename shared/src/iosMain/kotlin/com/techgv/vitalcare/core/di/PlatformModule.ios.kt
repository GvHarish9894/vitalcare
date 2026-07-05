package com.techgv.vitalcare.core.di

import androidx.room.RoomDatabase
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.techgv.vitalcare.core.util.AppInfo
import com.techgv.vitalcare.data.backup.FileExporter
import com.techgv.vitalcare.data.backup.IosBackupScheduler
import com.techgv.vitalcare.data.backup.IosFileExporter
import com.techgv.vitalcare.data.local.VitalCareDatabase
import com.techgv.vitalcare.data.local.databaseBuilder
import com.techgv.vitalcare.domain.backup.BackupScheduler
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.backup.UnavailableDriveAuthorizer
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<VitalCareDatabase>> { databaseBuilder() }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<FileExporter> { IosFileExporter() }
    // Drive needs the GoogleSignIn SDK added in Xcode plus a client ID
    // (contributor-supplied, D-027) — until then the feature reads unavailable.
    single<DriveAuthorizer> { UnavailableDriveAuthorizer() }
    single { IosBackupScheduler() }
    single<BackupScheduler> { get<IosBackupScheduler>() }
    single {
        val versionName = NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        AppInfo(versionName = versionName ?: "1.0")
    }
}
