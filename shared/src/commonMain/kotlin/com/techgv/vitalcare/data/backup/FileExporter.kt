package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.core.util.AppResult

/**
 * Platform seam for saving/sharing an exported file (05 §3): Android writes
 * to app cache and opens a share sheet via FileProvider; iOS presents a
 * UIActivityViewController. Bound per platform in the Koin platform module.
 */
interface FileExporter {
    suspend fun export(fileName: String, mimeType: String, content: String): AppResult<Unit>
}
