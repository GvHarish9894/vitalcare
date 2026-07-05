package com.techgv.vitalcare.data.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes the export into app cache and opens the system share sheet through a
 * FileProvider URI (05 §3). Works from the application context.
 */
class AndroidFileExporter(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : FileExporter {

    override suspend fun export(
        fileName: String,
        mimeType: String,
        content: String,
    ): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportsDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            AppResult.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            AppResult.Failure(AppError.Unknown(t))
        }
    }
}
