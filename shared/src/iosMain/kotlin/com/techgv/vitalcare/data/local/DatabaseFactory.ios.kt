package com.techgv.vitalcare.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun databaseBuilder(): RoomDatabase.Builder<VitalCareDatabase> {
    val documentsDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("Unable to resolve the documents directory")
    return Room.databaseBuilder<VitalCareDatabase>(name = "$documentsDir/$DATABASE_NAME")
}
