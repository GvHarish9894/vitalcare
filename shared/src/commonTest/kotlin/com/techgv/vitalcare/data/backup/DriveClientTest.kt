package com.techgv.vitalcare.data.backup

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DriveClientTest {

    private fun client(engine: () -> MockEngine): DriveClient =
        DriveClient(HttpClient(engine()))

    private val jsonHeaders = headersOf("Content-Type", "application/json")

    @Test
    fun uploadCreatesFileWithMultipartWhenNoneExists() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        var createBody = ""
        val driveClient = client {
            MockEngine { request ->
                requests += request.method to request.url.toString()
                when {
                    request.url.encodedPath.endsWith("/drive/v3/files") &&
                        request.method == HttpMethod.Get ->
                        respond("""{"files":[]}""", HttpStatusCode.OK, jsonHeaders)
                    request.method == HttpMethod.Post -> {
                        createBody = request.body.toByteArray().decodeToString()
                        respond("""{"id":"new-id"}""", HttpStatusCode.OK, jsonHeaders)
                    }
                    else -> respond("unexpected", HttpStatusCode.BadRequest)
                }
            }
        }

        val result = driveClient.upload("token-1", """{"schemaVersion":1}""")

        assertIs<AppResult.Success<Unit>>(result)
        assertTrue(requests.any { it.first == HttpMethod.Post && it.second.contains("uploadType=multipart") })
        assertTrue(createBody.contains(""""parents":["appDataFolder"]"""))
        assertTrue(createBody.contains(""""schemaVersion":1"""))
    }

    @Test
    fun uploadOverwritesExistingFileWithMediaPatch() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val driveClient = client {
            MockEngine { request ->
                requests += request.method to request.url.toString()
                when (request.method) {
                    HttpMethod.Get ->
                        respond(
                            """{"files":[{"id":"existing-9","name":"vitalcare-backup.json"}]}""",
                            HttpStatusCode.OK,
                            jsonHeaders,
                        )
                    HttpMethod.Patch -> respond("""{"id":"existing-9"}""", HttpStatusCode.OK, jsonHeaders)
                    else -> respond("unexpected", HttpStatusCode.BadRequest)
                }
            }
        }

        val result = driveClient.upload("token-1", "{}")

        assertIs<AppResult.Success<Unit>>(result)
        assertTrue(
            requests.any {
                it.first == HttpMethod.Patch && it.second.contains("files/existing-9") &&
                    it.second.contains("uploadType=media")
            },
        )
    }

    @Test
    fun downloadReturnsNullWhenNoBackupExists() = runTest {
        val driveClient = client {
            MockEngine { respond("""{"files":[]}""", HttpStatusCode.OK, jsonHeaders) }
        }

        val result = assertIs<AppResult.Success<String?>>(driveClient.download("token-1"))
        assertNull(result.value)
    }

    @Test
    fun downloadFetchesMediaContent() = runTest {
        val driveClient = client {
            MockEngine { request ->
                if (request.url.parameters["alt"] == "media") {
                    respond("""{"schemaVersion":1,"records":[]}""", HttpStatusCode.OK, jsonHeaders)
                } else {
                    respond("""{"files":[{"id":"f1"}]}""", HttpStatusCode.OK, jsonHeaders)
                }
            }
        }

        val result = assertIs<AppResult.Success<String?>>(driveClient.download("token-1"))
        assertEquals("""{"schemaVersion":1,"records":[]}""", result.value)
    }

    @Test
    fun unauthorizedMapsToDriveAuthError() = runTest {
        val driveClient = client {
            MockEngine { respond("denied", HttpStatusCode.Unauthorized) }
        }

        val failure = assertIs<AppResult.Failure>(driveClient.upload("stale-token", "{}"))
        assertEquals(AppError.DriveAuth, failure.error)
    }

    @Test
    fun serverErrorMapsToNetworkError() = runTest {
        val driveClient = client {
            MockEngine { respond("boom", HttpStatusCode.InternalServerError) }
        }

        val failure = assertIs<AppResult.Failure>(driveClient.download("token-1"))
        assertEquals(AppError.Network, failure.error)
    }
}
