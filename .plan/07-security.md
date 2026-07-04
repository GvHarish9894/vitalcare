# 07 — Security & Privacy

VitalCare stores **health data**. Because the app is **local-only with no account** (D-018), the
security story is dominated by on-device protection and by keeping the optional Google Drive
backup least-privilege. By default, **health data never leaves the device**.

## 1. Identity & access
- **No authentication, no accounts, no passwords, no sessions** (D-018). Nothing to phish, leak,
  or enumerate. The device's own lock screen is the access control for the app's data.
- The only credential the app ever holds is an **optional Google Drive OAuth token**, and only
  after the user explicitly connects Drive (D-021).

## 2. Data in transit
- The app makes network calls **only** for Google Drive backup/restore, and only when the user
  triggers them. All Drive REST calls are HTTPS/TLS (Ktor pinned to `https` base URLs).
- No cleartext traffic: Android manifest keeps `usesCleartextTraffic=false` (default);
  iOS App Transport Security stays at defaults (no exceptions added).

## 3. Data at rest (device)
| Data | Protection |
|---|---|
| Room database | OS-level app-sandbox + full-disk encryption (both platforms). DB-level encryption (SQLCipher) is **Future** — reassess before any store release |
| Drive OAuth token | `SecureSettings`: EncryptedSharedPreferences (Android) / Keychain (iOS) (D-011). Never in plain settings, never logged |
| App key-values (theme, last-backup time, cadence, profile name) | plain `multiplatform-settings` (non-sensitive) |
| Backup encryption key material (if D-026 enabled) | only a KDF salt is stored; the password is never persisted |
| CSV/JSON produced for export | handed straight to the platform save/share sheet; not retained by the app |

- Android `allowBackup`: set to **false** so health data doesn't land in device-to-device backups
  outside our control — *note: currently `true` in the template manifest; change in Phase 1.*

## 4. Google Drive least-privilege (D-021)
- **Scope:** request only `https://www.googleapis.com/auth/drive.file` — the app can read/write
  only the files it created. It can never see the user's other Drive contents.
- **Storage location:** backups live in Drive's hidden **`appDataFolder`**, isolated from the
  user's visible files (D-023).
- **Revocation:** "Disconnect Drive" revokes the token and clears it from secure storage; the user
  can also revoke access from their Google account at any time.
- The backup contains PHI, so if the user enables optional encryption (D-026) it is applied
  client-side before upload — the plaintext never touches the network in that mode.

## 5. Permissions (least privilege)
- Android: `INTERNET` only (needed for Drive backup; nothing else). No location, no contacts, no
  broad storage permission — CSV export uses the Storage Access Framework (scoped, user-picked).
- iOS: no special entitlements beyond Background App Refresh (for optional auto-backup) and the
  Google Sign-In URL scheme (only if Drive is configured in the build).

## 6. Logging, analytics & crash hygiene
- **No PHI anywhere off-device**: never log, and never send to Analytics or Crashlytics, any
  vital value, remark, or the profile name. This is a hard rule (D-028).
- Local logs and telemetry carry *ids*, *counts*, screen names, and feature-usage flags only
  ("backed up 12 records", not their contents).
- **Firebase Analytics + Crashlytics** are the only third-party SDKs (D-028) — used purely for
  anonymous usage insight and crash reporting, never for user/health data. Crash reports must not
  attach PHI (guard any custom keys / breadcrumbs); rely on the `Telemetry` seam so nothing PHI
  can be passed in.
- A Settings opt-out disables all telemetry (D-029). Debug-only verbose logging is stripped from
  release builds.

## 7. Open-source & build posture (D-027)
- **Zero committed secrets.** The repo builds and runs the full local app with no config. There is
  no `google-services.json`, no API keys, no backend.
- The Drive OAuth **client ID(s)** are contributor-supplied via untracked config
  (`local.properties` / gitignored resource) and documented in the README — never committed.
- Dependencies via the version catalog only; prefer official/first-party libraries (androidx,
  JetBrains, Ktor, Google Identity / GoogleSignIn).
- Release builds: enable R8 minification before store release (currently off — release-phase item).
- **Firebase is scoped to Analytics + Crashlytics only** (D-028). The committed app config
  (`androidApp/google-services.json`, `iosApp/iosApp/GoogleService-Info.plist`) is client
  identifiers, not secrets, so it stays committed; the app still builds without it. The
  Firestore-only artifacts (`firestore.rules`, `firestore.indexes.json`, and the Firestore parts
  of `firebase.json`/`.firebaserc`) are inactive (no Firestore) but are not being deleted. Keep
  the Firebase API keys restricted in the console; never enable Firestore/Auth rules for prod use.

## 8. Privacy commitments (NFR-8)
- **Health data** stays **on the device** unless the user chooses to export (CSV) or connect Drive.
  It is never sent to Firebase or any third party.
- When backup is used, health data goes **only** to the user's own Google Drive — never to us
  (we operate no server for user data).
- The **only** data that leaves the device by default is anonymous, **PHI-free** usage/crash
  telemetry to Firebase (D-028), which the user can turn off (D-029). This is disclosed in the
  README / privacy note.
- There is no account to delete; uninstalling the app removes all local data. A user who used
  Drive can delete the backup from their own Drive (and revoke access) at any time.
