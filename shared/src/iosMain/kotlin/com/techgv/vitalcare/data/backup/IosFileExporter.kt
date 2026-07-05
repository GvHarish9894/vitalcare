package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

/** Writes to the temp dir and presents the system share sheet (05 §3). */
class IosFileExporter : FileExporter {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun export(
        fileName: String,
        mimeType: String,
        content: String,
    ): AppResult<Unit> = withContext(Dispatchers.Main) {
        try {
            val path = NSTemporaryDirectory() + fileName
            val written = NSString.create(string = content).writeToFile(
                path = path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
            if (!written) return@withContext AppResult.Failure(AppError.Unknown())

            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: return@withContext AppResult.Failure(AppError.Unknown())
            val activityController = UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(path)),
                applicationActivities = null,
            )
            // iPad requires a popover anchor.
            activityController.popoverPresentationController?.sourceView = rootController.view
            rootController.presentViewController(activityController, animated = true, completion = null)
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            AppResult.Failure(AppError.Unknown(t))
        }
    }
}
