package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.debugLog
import com.techgv.vitalcare.domain.backup.BackupRemote
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Drive REST v3 client for the single backup file in `appDataFolder`
 * (05 §5, D-021/D-023). Only ever called from the explicit backup/restore
 * actions — the app never touches the network otherwise (03 §6).
 */
class DriveClient(
    private val httpClient: HttpClient,
    private val filesBaseUrl: String = "https://www.googleapis.com/drive/v3",
    private val uploadBaseUrl: String = "https://www.googleapis.com/upload/drive/v3",
) : BackupRemote {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun upload(accessToken: String, content: String): AppResult<Unit> = guard {
        val existingId = findBackupFileId(accessToken)
        val response = if (existingId == null) {
            createFile(accessToken, content)
        } else {
            updateFile(accessToken, existingId, content)
        }
        if (!response.status.isSuccess()) {
            debugLog(TAG, "upload ${response.status} — ${response.bodyAsText().take(600)}")
            return failureFor(response)
        }
        AppResult.Success(Unit)
    }

    override suspend fun download(accessToken: String): AppResult<String?> = guard {
        val fileId = findBackupFileId(accessToken)
            ?: return@guard AppResult.Success(null)
        val response = httpClient.get("$filesBaseUrl/files/$fileId") {
            parameter("alt", "media")
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        if (!response.status.isSuccess()) {
            debugLog(TAG, "download ${response.status} — ${response.bodyAsText().take(600)}")
            return failureFor(response)
        }
        AppResult.Success(response.bodyAsText())
    }

    private suspend fun findBackupFileId(accessToken: String): String? {
        val response = httpClient.get("$filesBaseUrl/files") {
            parameter("spaces", "appDataFolder")
            parameter("q", "name = '${BackupSerializer.FILE_NAME}'")
            parameter("fields", "files(id,name)")
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        if (!response.status.isSuccess()) {
            debugLog(TAG, "list ${response.status} — ${response.bodyAsText().take(600)}")
            throw DriveHttpException(response.status)
        }
        return json.decodeFromString(FileListDto.serializer(), response.bodyAsText())
            .files.firstOrNull()?.id
    }

    private suspend fun createFile(accessToken: String, content: String): HttpResponse {
        // multipart/related: metadata part (name + appDataFolder parent) + media part.
        val metadata = """{"name":"${BackupSerializer.FILE_NAME}","parents":["appDataFolder"]}"""
        val body = buildString {
            append("--$BOUNDARY\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$BOUNDARY\r\n")
            append("Content-Type: ${BackupSerializer.MIME_TYPE}\r\n\r\n")
            append(content)
            append("\r\n--$BOUNDARY--")
        }
        return httpClient.post("$uploadBaseUrl/files") {
            parameter("uploadType", "multipart")
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(
                TextContent(
                    text = body,
                    contentType = ContentType("multipart", "related")
                        .withParameter("boundary", BOUNDARY),
                ),
            )
        }
    }

    private suspend fun updateFile(
        accessToken: String,
        fileId: String,
        content: String,
    ): HttpResponse = httpClient.patch("$uploadBaseUrl/files/$fileId") {
        parameter("uploadType", "media")
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        setBody(TextContent(content, ContentType.Application.Json))
    }

    private fun <T> failureFor(response: HttpResponse): AppResult<T> =
        AppResult.Failure(errorFor(response.status))

    private fun errorFor(status: HttpStatusCode): AppError = when (status) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AppError.DriveAuth
        else -> AppError.Network
    }

    /** Network/IO problems become [AppError.Network]; auth problems [AppError.DriveAuth]. */
    private inline fun <T> guard(block: () -> AppResult<T>): AppResult<T> = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (drive: DriveHttpException) {
        AppResult.Failure(errorFor(drive.status))
    } catch (t: Throwable) {
        debugLog(TAG, "network exception — ${t::class.simpleName}: ${t.message}")
        AppResult.Failure(AppError.Network)
    }

    private class DriveHttpException(val status: HttpStatusCode) : Exception()

    @Serializable
    private data class FileListDto(val files: List<FileDto> = emptyList())

    @Serializable
    private data class FileDto(val id: String, val name: String? = null)

    private companion object {
        const val BOUNDARY = "vitalcare-backup-boundary"
        const val TAG = "VitalCareDrive"
    }
}
