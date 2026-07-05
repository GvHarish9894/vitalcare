package com.techgv.vitalcare.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Called once from each platform entry point (MainActivity app / iOSApp). */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            platformModule,
            coreModule,
            databaseModule,
            repositoryModule,
            useCaseModule,
            backupModule,
            reminderModule,
            viewModelModule,
        )
    }
}
