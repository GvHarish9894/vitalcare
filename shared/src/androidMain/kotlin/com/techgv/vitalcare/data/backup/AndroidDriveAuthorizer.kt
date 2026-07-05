package com.techgv.vitalcare.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.backup.DriveConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Drive authorization via Play Services' AuthorizationClient (D-021):
 * `drive.file` scope only, never an app login. Requires the host app's
 * package + signing SHA-1 to be registered as an OAuth client in the
 * contributor's Google Cloud project (D-027) — gated by [DriveConfig].
 *
 * When consent UI is needed, the pending intent is launched through
 * [resolutionLauncher], which MainActivity wires to an activity-result
 * launcher and forwards back via [onAuthorizationResult].
 */
class AndroidDriveAuthorizer(
    private val context: Context,
    private val config: DriveConfig,
    private val httpClient: HttpClient,
) : DriveAuthorizer {

    override val isAvailable: Boolean get() = config.enabled

    /** Set by MainActivity while it is alive; launches the consent UI. */
    var resolutionLauncher: ((PendingIntent) -> Unit)? = null

    private var pendingAuthorization: kotlinx.coroutines.CancellableContinuation<AppResult<String>>? =
        null

    override suspend fun authorize(interactive: Boolean): AppResult<String> {
        if (!isAvailable) return AppResult.Failure(AppError.DriveAuth)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()
        return try {
            val result = Identity.getAuthorizationClient(context).authorize(request).await()
            when {
                !result.hasResolution() ->
                    result.accessToken?.let { AppResult.Success(it) }
                        ?: AppResult.Failure(AppError.DriveAuth)
                !interactive ->
                    // Background/auto path must never pop consent UI (D-022).
                    AppResult.Failure(AppError.DriveAuth)
                else -> {
                    val pendingIntent = result.pendingIntent
                        ?: return AppResult.Failure(AppError.DriveAuth)
                    val launcher = resolutionLauncher
                        ?: return AppResult.Failure(AppError.DriveAuth)
                    suspendCancellableCoroutine { continuation ->
                        pendingAuthorization = continuation
                        continuation.invokeOnCancellation { pendingAuthorization = null }
                        launcher(pendingIntent)
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Exception) {
            AppResult.Failure(AppError.DriveAuth)
        }
    }

    /** Called by MainActivity with the consent-flow result. */
    fun onAuthorizationResult(data: Intent?) {
        val continuation = pendingAuthorization ?: return
        pendingAuthorization = null
        val result = try {
            val authorization =
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            authorization.accessToken?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.DriveAuth)
        } catch (t: Exception) {
            AppResult.Failure(AppError.DriveAuth)
        }
        continuation.resume(result)
    }

    override suspend fun revoke(): AppResult<Unit> {
        // Tokens live inside Play Services, not in our storage — revoke the
        // grant itself so a reconnect re-consents (FR-B6).
        val token = when (val silent = authorize(interactive = false)) {
            is AppResult.Success -> silent.value
            is AppResult.Failure -> return AppResult.Success(Unit) // nothing to revoke
        }
        return try {
            httpClient.submitForm(
                url = "https://oauth2.googleapis.com/revoke",
                formParameters = parameters { append("token", token) },
            )
            AppResult.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Exception) {
            AppResult.Success(Unit) // best effort — local disconnect proceeds regardless
        }
    }

    private companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}

/** Minimal Task await — avoids the coroutines-play-services dependency. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value -> continuation.resume(value) }
    addOnFailureListener { error -> continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}
