# 05 — Authentication

How auth works end-to-end. Provider: **Firebase Authentication (email/password)** accessed from
shared code via the **GitLive Firebase Kotlin SDK** (`dev.gitlive:firebase-auth`, D-004).
UI for these flows: [03-ui-ux-design.md](03-ui-ux-design.md) §3.2–3.4.

## 1. Principles

- **patientId = Firebase UID** (D-014). All local and cloud data is keyed by it.
- **Online only for the handshake:** register, first login, and password reset need network.
  Everything else — including staying logged in — works offline (FR-A6).
- The app **never stores passwords** (D-011). Token/session management belongs to the Firebase SDK.
- Auth state is exposed to the app as a single cold `Flow<AuthState>`; no screen asks Firebase directly.

## 2. Auth state model

```kotlin
sealed interface AuthState {
    data object Unknown : AuthState               // still restoring (Splash)
    data object LoggedOut : AuthState
    data class LoggedIn(val patient: Patient) : AuthState   // uid, name, email
}
```

`AuthRepository` (domain interface, data impl):

```kotlin
interface AuthRepository {
    val authState: Flow<AuthState>                        // wraps Firebase authStateChanged
    suspend fun login(email: String, password: String): AppResult<Patient>
    suspend fun register(name: String, email: String, password: String): AppResult<Patient>
    suspend fun sendPasswordReset(email: String): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
}
```

## 3. Flows

### 3.1 App launch / session restore (FR-A4)
1. Splash observes `authState`.
2. Firebase SDK restores the cached session from platform-secure storage (its own mechanism —
   no network call required).
3. `LoggedIn` → Dashboard; `LoggedOut` → Login. Decision typically < 500 ms.

### 3.2 Register (FR-A1)
1. Client-side validation: name non-empty, email format, password ≥ 8 chars.
2. `createUserWithEmailAndPassword(email, password)`.
3. Set display name (`updateProfile`) = entered name.
4. Create the Firestore profile doc `patients/{uid}` → `{ name, email, createdAt }`.
   If this write fails (e.g. connection dropped mid-flow), it is retried by the sync engine —
   registration still succeeds.
5. User is now logged in → Dashboard.

### 3.3 Login (FR-A2)
1. Client-side validation (format only).
2. `signInWithEmailAndPassword`.
3. **Different-user check (D-015):** compare `uid` to `lastKnownUid` in secure settings.
   If different → wipe local Room DB before proceeding; then store new `lastKnownUid`.
4. → Dashboard; sync scheduler kicks off a catch-up sync.

### 3.4 Forgot password (FR-A3)
1. `sendPasswordResetEmail(email)`.
2. UI always shows the generic success state, even for unknown emails
   (no account enumeration — see 07-security.md).

### 3.5 Logout (FR-A5, D-015)
1. Confirmation dialog (warns that internet is needed to log back in).
2. Stop/cancel scheduled sync work.
3. `Firebase.auth.signOut()` — clears the SDK's cached session.
4. Clear session-scoped settings (but **keep** `lastKnownUid` and the local database —
   unsynced readings must survive a re-login by the same user).
5. Navigate to Auth graph, clearing the back stack.

### 3.6 Offline behavior (FR-A6)
| Situation | Behavior |
|---|---|
| Already logged in, device offline | Full app works: record, history, analytics. Sync pill shows "Offline" |
| Login/Register attempted offline | Friendly inline error: "No connection — this step needs internet" |
| Session restore offline | Works — Firebase restores the cached user without network |
| Token refresh fails while offline | User stays logged in; Firestore writes queue locally as `PENDING` anyway |

## 4. What is stored where

| Data | Location | Notes |
|---|---|---|
| Firebase session/tokens | Managed by Firebase SDK (Keychain / Android internal storage) | Never touched by app code |
| `lastKnownUid` | `SecureSettings` (EncryptedSharedPreferences / Keychain) | For the D-015 wipe check |
| Profile (name/email) | Room (`PatientEntity`) mirrored from `patients/{uid}` | For offline display |
| Password | **Nowhere** | — |

## 5. Auth ↔ data boundary

- Every repository call that touches patient data resolves `uid` from the current `AuthState`;
  if `LoggedOut`, data calls fail fast with `AppError.Auth` (should be unreachable behind the nav guard).
- Firestore security rules enforce the same on the server (see 07-security.md §4):
  a user can only read/write `patients/{uid}` where `uid == request.auth.uid`.

## 6. Nav guard

The root `App()` composable observes `authState` and swaps graphs:
`LoggedIn` → Main graph, `LoggedOut` → Auth graph (back stack cleared both ways).
A mid-session sign-out (e.g. account disabled server-side) therefore lands on Login automatically.

## 7. Error mapping (Firebase → user-facing)

| Firebase error | UI message |
|---|---|
| invalid-credential / wrong-password / user-not-found | "Email or password is incorrect." |
| email-already-in-use | "An account with this email already exists." |
| weak-password | "Password must be at least 8 characters." |
| invalid-email | "Enter a valid email address." |
| too-many-requests | "Too many attempts. Try again in a few minutes." |
| network unavailable | "No connection — this step needs internet." |
| user-disabled | "This account has been disabled." |
| anything else | "Something went wrong. Please try again." (log details, see 07-security.md §6) |

Mapping lives in the data layer (`AuthRepositoryImpl`), producing `AppError.Auth(reason)`;
UI never sees raw Firebase exceptions.

## 8. Future (explicitly not MVP)

- Google / Apple sign-in (Apple sign-in becomes mandatory if other social logins are added on iOS).
- Biometric app-lock (FaceID/fingerprint) on top of the cached session.
- Email verification enforcement.
- Caregiver accounts linked to a patient (multi-user, D-014 revision).
