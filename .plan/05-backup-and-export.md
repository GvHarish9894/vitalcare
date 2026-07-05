# 05 — Backup, Restore & Export

How data leaves the device — entirely at the user's choice (D-020). There is **no account and no
login** (D-018); this feature is the only thing that ever talks to a network, and only when the
user turns it on. Data formats (CSV, JSON) are defined in
[06-data-and-storage.md](06-data-and-storage.md); this doc covers the *flows*, the Google Drive
authorization model, scheduling, and the platform seams. UI: [03-ui-ux-design.md](03-ui-ux-design.md) §3.9.

## 1. Principles

- **Opt-in, never required.** The app is fully usable with zero backup configured. Nothing runs
  in the background until the user connects Drive *and* chooses an auto cadence (D-022).
- **The user owns the destination.** CSV goes to a file the user picks; Drive backup goes to the
  user's *own* Drive (`appDataFolder`). We operate no server and hold no data.
- **Local is never at risk.** Backup reads the DB; restore only ever *merges in* (D-024). No flow
  deletes local data.
- **Least privilege.** Drive access uses the narrowest scope, `drive.file` (D-021).

## 2. Two mechanisms

| | CSV export | Google Drive backup |
|---|---|---|
| Needs network | No | Yes (upload/download) |
| Needs Google sign-in | No | Yes — Drive authorization only (D-021) |
| Direction | Out only | Out (backup) + in (restore) |
| Format | RFC 4180 CSV (06 §3) | Versioned JSON snapshot (06 §4) |
| Cadence | Manual, on demand | Manual + optional auto (D-022) |
| Use case | "Give me my data" / spreadsheets / share with a doctor | "Don't lose my history" / new phone |

## 3. CSV export flow

1. User taps **Export to CSV** (Settings, or History overflow) → chooses scope (defaults to the
   current History filter: All / Today / Week / Month).
2. Use case reads records (`getAll()` / `getByDateRange`), encodes CSV (06 §3) in shared code.
3. Hand the bytes to the platform `FileExporter` (`expect`/`actual`):
   - **Android:** `ACTION_CREATE_DOCUMENT` (Storage Access Framework) to save, or a share
     `Intent` (`ACTION_SEND`, `text/csv`) to send.
   - **iOS:** `UIActivityViewController` / `UIDocumentPicker` to save or share.
4. Filename `vitalcare-YYYYMMDD-HHmm.csv`. No network, no account, instant.

## 4. Google Drive authorization (D-021 — authorization, not login)

Google Sign-In here **only** obtains a Drive OAuth token; it is not an app identity and never
gates the app.

```kotlin
interface DriveAuthorizer {                 // expect/actual per platform
    suspend fun connect(): AppResult<Unit>          // launches Google consent, stores token
    suspend fun accessToken(): AppResult<String>    // cached; refreshes silently when possible
    suspend fun disconnect()                         // revoke + clear local token
    val isConnected: Flow<Boolean>
}
```

- **Scope:** `https://www.googleapis.com/auth/drive.file` only — the app can see/manage the files
  it creates, nothing else in the user's Drive.
- **Platform actuals:**
  - **Android:** Google Identity Services / `AuthorizationClient` (or Credential Manager) to get
    an authorized `GoogleSignInAccount`/token with the Drive scope.
  - **iOS:** GoogleSignIn SDK (added to `iosApp` via SPM/CocoaPods) requesting the Drive scope.
- **Tokens** are stored in `SecureSettings` (EncryptedSharedPreferences / Keychain, D-011), never
  in plain settings and never logged.
- **Config (D-027):** the OAuth **client ID(s)** are contributor-supplied (README + untracked
  `local.properties`), never committed. Without them, the Drive UI shows a "not configured in
  this build" state and CSV export still works.

## 5. Backup flow (Drive)

```
suspend fun backupNow(): AppResult<Unit> {
    val token = driveAuthorizer.accessToken().orReturn(failure)
    val snapshot = buildBackup(dao.getAll(), profileName, appVersion)   // JSON, 06 §4
    val body = if (encryptionEnabled) encrypt(snapshot, password) else snapshot   // D-026
    driveClient.upsertAppDataFile("vitalcare-backup.json", body, token)  // create or overwrite
    settings.lastBackupAt = clock.now()
    return Success
}
```

- **`DriveClient`** is shared Ktor code calling the Drive REST API v3 (`files` in `appDataFolder`):
  find the existing backup file by name, `PATCH` its media if present else `POST` (multipart)
  to create. One file, full-snapshot overwrite (D-023).
- Idempotent and safe to retry; a failed upload leaves the previous backup intact.
- On success, update `lastBackupAt` (drives the "backed up N ago" / "unbacked-up changes" hints).

## 6. Restore flow (Drive)

1. User taps **Restore from Drive** (Settings) → confirmation explaining it *merges* (never wipes).
2. Download `vitalcare-backup.json` via `DriveClient`; if encrypted (D-026), prompt for password
   and decrypt; validate `schemaVersion` (06 §4/5).
3. **Merge** into Room by record id, newer `updatedAt` wins (D-024). Local-only records survive.
4. Reactive Room Flows refresh the UI automatically; show "Restored N records".

Empty/absent backup → friendly "No backup found in your Drive yet."

## 7. Scheduling (D-022)

| Trigger | Android | iOS |
|---|---|---|
| Manual "Back up now" / "Restore" / "Export CSV" | direct call | direct call |
| Auto-backup (Off / Daily / Weekly / Monthly) | WorkManager periodic, network-constrained | `BGAppRefreshTask` (opportunistic) |
| App foregrounded with auto on + unbacked-up changes | catch-up backup | same |

```kotlin
interface BackupScheduler {                 // expect/actual (reuses the old sync-scheduler seam)
    fun setAutoBackup(cadence: BackupCadence) // Off | Daily | Weekly | Monthly
    fun requestBackupNow()
}
enum class BackupCadence { OFF, DAILY, WEEKLY, MONTHLY }
```

- **Default cadence: Off** — nothing runs in the background unless the user opts in (friction goal).
- Auto-backup only schedules when Drive is connected. Disconnecting Drive cancels scheduled work.
- Retry: rely on WorkManager's `BackoffPolicy.EXPONENTIAL` / the OS's opportunistic scheduling; a
  missed backup simply happens at the next slot — no per-record retry bookkeeping (there are no
  per-record states anymore).

## 8. What is stored where

| Data | Location | Notes |
|---|---|---|
| Vital records | Room (device) | The only source of truth (D-005) |
| Profile name (optional) | `multiplatform-settings` | D-019 |
| `lastBackupAt`, `driveConnected`, auto cadence | `multiplatform-settings` | Non-sensitive |
| Drive OAuth token | `SecureSettings` (Keychain / EncryptedSharedPreferences) | D-011; never logged |
| Backup encryption password | **Nowhere** | Prompted each time; only a KDF salt is stored if enabled (D-026) |
| Backup snapshot | User's Drive `appDataFolder` | User owns it; we never see it |

## 9. Error handling (Drive)

Failures map to `AppError` (04 §8) and surface as inline, non-blocking messages:

| Situation | UI message |
|---|---|
| Offline during backup/restore | "No connection — backup needs internet." |
| Drive not configured in this build (D-027) | "Google Drive backup isn't set up in this build." |
| Consent cancelled | Silent return to the not-connected state. |
| Token expired / revoked | Re-prompt to reconnect Drive. |
| Restore of a newer `schemaVersion` | "This backup was made by a newer version of the app." |
| Wrong decryption password (D-026) | "Couldn't unlock this backup — check the password." |

No PHI is ever logged (07 §6): log record *counts* and file ids, never values.

## 10. Future (explicitly not MVP)

- Multiple named backups / backup history on Drive (versions), instead of one overwritten file.
- Other providers (Dropbox, iCloud Drive, generic WebDAV) behind the same `DriveClient` seam.
- Scheduled CSV export (e.g. weekly email/share).
- A destructive "replace all from backup" option (behind a strong confirmation, D-024).
- Backup encryption promoted from optional (D-026) to a first-class, key-managed flow.
