package com.techgv.vitalcare.core.di

import com.techgv.vitalcare.data.backup.IosBackupScheduler
import com.techgv.vitalcare.domain.backup.DriveConfig
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * Swift-friendly entry point: `KoinIOSKt.doInitKoin()` from iOSApp.init.
 * Drive stays disabled until a contributor wires GoogleSignIn + a client ID
 * (D-027); flip [driveEnabled] alongside that work.
 */
fun doInitKoin(driveEnabled: Boolean = false) {
    initKoin {
        modules(module { single { DriveConfig(enabled = driveEnabled) } })
    }
    // BGTask handlers must be registered before app launch completes (D-022).
    KoinPlatform.getKoin().get<IosBackupScheduler>().registerBackgroundTask()
}
