package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import kotlinx.serialization.json.Json

/**
 * Encodes/decodes the backup JSON (06 §4). Decoding tolerates unknown fields
 * from newer minor revisions but rejects a `schemaVersion` above what this
 * app understands, with a clear error (D-023).
 */
class BackupSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    fun decode(raw: String): AppResult<BackupFile> {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), raw)
        } catch (t: Throwable) {
            return AppResult.Failure(AppError.CorruptBackup)
        }
        if (backup.schemaVersion > SCHEMA_VERSION) {
            return AppResult.Failure(AppError.UnsupportedBackup(backup.schemaVersion))
        }
        return AppResult.Success(backup)
    }

    companion object {
        // v2 adds the `fluids` array (D-032). Decoding still tolerates v1 backups.
        const val SCHEMA_VERSION = 2
        const val FILE_NAME = "vitalcare-backup.json"
        const val MIME_TYPE = "application/json"
    }
}
