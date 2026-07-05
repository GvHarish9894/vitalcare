package com.techgv.vitalcare.domain.backup

import com.techgv.vitalcare.core.util.AppResult

/**
 * Remote store for the single backup document (05 §5): one file in the
 * user's Drive appDataFolder, overwritten on each backup (D-023).
 */
interface BackupRemote {

    /** Creates or overwrites the backup document. */
    suspend fun upload(accessToken: String, content: String): AppResult<Unit>

    /** Downloads the backup document; Success(null) means no backup exists yet. */
    suspend fun download(accessToken: String): AppResult<String?>
}
