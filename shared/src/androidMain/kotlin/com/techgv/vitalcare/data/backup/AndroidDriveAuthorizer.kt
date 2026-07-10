package com.techgv.vitalcare.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.debugLog
import com.techgv.vitalcare.domain.backup.DriveAuthorizer
import com.techgv.vitalcare.domain.backup.DriveConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
) : DriveAuthorizer {

    override val isAvailable: Boolean get() = config.enabled

    /** Set by MainActivity while it is alive; launches the consent UI. */
    var resolutionLauncher: ((PendingIntent) -> Unit)? = null

    private var pendingAuthorization: kotlinx.coroutines.CancellableContinuation<AppResult<String>>? =
        null

    override suspend fun authorize(interactive: Boolean): AppResult<String> {
        if (!isAvailable) return AppResult.Failure(AppError.DriveAuth)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
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
            debugLog(TAG, "authorize failed — ${t::class.simpleName}: ${t.message}")
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
                ?: run {
                    debugLog(TAG, "consent returned no access token (denied / not a test user)")
                    AppResult.Failure(AppError.DriveAuth)
                }
        } catch (t: Exception) {
            debugLog(TAG, "consent result failed — ${t::class.simpleName}: ${t.message}")
            AppResult.Failure(AppError.DriveAuth)
        }
        continuation.resume(result)
    }

    override suspend fun revoke(): AppResult<Unit> {
        // Disconnect = forget locally + evict the cached access token so a
        // future connect re-mints a fresh one. We deliberately do NOT hit the
        // OAuth revoke endpoint: the Authorization API has no revoke concept
        // and keeps serving the (now-dead) token, which caused 401s on
        // reconnect. Full account-level revocation stays available to the user
        // in their Google Account settings. `drive.appdata` only ever exposes
        // our own hidden folder, so retaining the grant until then is low-risk.
        val token = when (val silent = authorize(interactive = false)) {
            is AppResult.Success -> silent.value
            is AppResult.Failure -> return AppResult.Success(Unit) // nothing cached
        }
        try {
            withContext(Dispatchers.IO) { GoogleAuthUtil.clearToken(context, token) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            debugLog(TAG, "clearToken best-effort failed — ${t::class.simpleName}: ${t.message}")
        }
        return AppResult.Success(Unit)
    }

    private companion object {
        // appDataFolder (D-023) requires drive.appdata — `drive.file` can't
        // reach the hidden app folder. This scope is also strictly more
        // private: it can ONLY see our own app data, never the user's files.
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val TAG = "VitalCareDrive"
    }
}

/** Minimal Task await — avoids the coroutines-play-services dependency. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value -> continuation.resume(value) }
    addOnFailureListener { error -> continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}
