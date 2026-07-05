package com.techgv.vitalcare.domain.backup

import com.techgv.vitalcare.core.util.AppResult

/**
 * Google authorization seam (D-021): used solely to obtain a Drive
 * `drive.file`-scope access token when the user connects Drive — never an
 * app-level login. Platform implementations wrap Play Services authorization
 * (Android) / Google Sign-In (iOS); tokens live inside those SDKs, not ours.
 */
interface DriveAuthorizer {

    /**
     * Whether this build has a Drive OAuth client configured (D-027 —
     * contributor-supplied, never committed). When false, the Settings row is
     * shown disabled ("Not set up in this build").
     */
    val isAvailable: Boolean

    /**
     * Obtains a fresh access token. [interactive] = true may show the Google
     * consent UI (user-initiated connect); false must stay silent (background
     * auto-backup) and fail with [com.techgv.vitalcare.core.util.AppError.DriveAuth]
     * if consent would be required.
     */
    suspend fun authorize(interactive: Boolean): AppResult<String>

    /** Revokes the grant on disconnect (FR-B6). */
    suspend fun revoke(): AppResult<Unit>
}

/** Default when the platform has no OAuth client configured. */
class UnavailableDriveAuthorizer : DriveAuthorizer {
    override val isAvailable: Boolean = false
    override suspend fun authorize(interactive: Boolean): AppResult<String> =
        AppResult.Failure(com.techgv.vitalcare.core.util.AppError.DriveAuth)
    override suspend fun revoke(): AppResult<Unit> = AppResult.Success(Unit)
}
