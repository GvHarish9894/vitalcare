# 07 — Security & Privacy

VitalCare stores **health data**. Security posture, from device to cloud.

## 1. Authentication
- Firebase Authentication, email/password (see [05-authentication.md](05-authentication.md)).
- Passwords never stored or logged by app code; session/token management is entirely the
  Firebase SDK's (platform-secure storage).
- Password policy: min 8 characters (enforced client-side and by Firebase settings).
- No account enumeration: forgot-password always reports generic success; login errors do not
  distinguish "wrong password" from "no such user".

## 2. Data in transit
- HTTPS/TLS only — Firebase SDKs enforce this; any future Ktor client pins to `https` base URLs.
- No cleartext traffic: Android manifest keeps `usesCleartextTraffic=false` (default);
  iOS App Transport Security stays at defaults (no exceptions added).

## 3. Data at rest (device)
| Data | Protection |
|---|---|
| Room database | OS-level app-sandbox + full-disk encryption (both platforms). DB-level encryption (SQLCipher) is **Future** — reassess before any store release |
| Session tokens | Firebase SDK internal (Keychain / Android protected storage) |
| App key-values (lastKnownUid, last sync) | `SecureSettings`: EncryptedSharedPreferences (Android) / Keychain (iOS) (D-011) |
| Exports/logs | No data export in MVP; logs carry no PHI (§6) |

- Android `allowBackup`: set to **false** (health data must not land in device-to-device
  backups outside our control) — *note: currently `true` in the template manifest; change in Phase 1.*
- Wipe-on-different-user login prevents cross-account leakage on shared devices (D-015).

## 4. Firestore security rules
Deployed alongside Phase 2 (auth). One patient, own data only (D-014):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /patients/{patientId} {
      allow read, write: if request.auth != null && request.auth.uid == patientId;
      match /vitals/{recordId} {
        allow read, write: if request.auth != null && request.auth.uid == patientId;
      }
    }
    match /{document=**} { allow read, write: if false; }   // default deny
  }
}
```

Server-side validation of vital ranges in rules is **Proposed** (defense in depth):
e.g. `request.resource.data.spo2 == null || (request.resource.data.spo2 >= 70 && <= 100)`.

## 5. Permissions (least privilege)
- Android: `INTERNET` only (WorkManager needs nothing extra). No location, no contacts, no storage.
- iOS: no special entitlements beyond Background App Refresh + push-free Firebase.

## 6. Logging hygiene
- **No PHI in logs**: never log vital values, remarks, email, or names. Log record *ids* and
  counts only ("synced 3 records", not their contents).
- No third-party analytics/crash SDKs in MVP. If added later (e.g. Crashlytics), scrub PHI first
  and record the decision in 02-design-decisions.md.
- Debug-only verbose logging is stripped from release builds.

## 7. Dependency & build posture
- Dependencies via version catalog only; prefer official/first-party libraries (androidx, JetBrains, Firebase, GitLive).
- Release builds: enable R8 minification before store release (currently off — Phase 9 item).
- Firebase config files (`google-services.json`, `GoogleService-Info.plist`) contain no secrets
  (they're client identifiers) but keep API keys restricted in the Firebase console.

## 8. Privacy commitments (NFR-8)
- Health data goes **only** to the user's own Firestore documents — no third parties.
- Account deletion (Future, pre-store-release requirement): delete auth user + all
  `patients/{uid}` data. App stores require this; plan it in Phase 9.
