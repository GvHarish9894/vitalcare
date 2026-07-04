# 02 — Design Decisions (ADR log)

Every significant technical or product decision, with its **why**. This is the file that keeps
the plan honest: when something must change, add or amend a decision here *first*, then update
the affected docs and code.

**Format:** each decision has an ID (`D-xxx`), a status, the decision, rationale, and the
alternatives that were rejected.

**Statuses:** `Settled` (locked in) · `Proposed` (recommended default — confirm/veto before the
relevant phase) · `Amended by D-xxx` (still holds, but modified) · `Superseded by D-xxx`.

> **2026-07-04 — Scope simplification (D-018 … D-027).** VitalCare was re-scoped to a
> **local-only app with no accounts**. Sign-up/login and mandatory cloud sync were removed;
> cloud backup became an **opt-in** feature (Google Drive) alongside **CSV export**. The app
> will be **open-sourced**, so it must build and run with **no secret configuration** (Firebase
> telemetry config aside, D-028).
> This superseded/amended D-004, D-005, D-006, D-007, D-009, D-011, D-014, and D-015. Read the
> D-018+ block below for the current shape; the older entries are kept for history.

---

## D-001 — Kotlin Multiplatform + Compose Multiplatform, shared UI — `Settled`
**Context:** Android and iOS apps needed by a very small team.
**Decision:** One Kotlin codebase. Business logic *and* UI live in the `shared` module (Compose Multiplatform); `androidApp`/`iosApp` are thin entry points.
**Rationale:** One implementation of validation, storage, and screens; features ship to both platforms simultaneously.
**Rejected:** Native Android + native iOS (double effort); Flutter/React Native (team is Kotlin-first; KMP gives full native interop when needed); KMP with native UIs (UI is most of this app — sharing it is most of the win).

## D-002 — Koin for dependency injection — `Settled`
**Decision:** Koin in `commonMain` for all DI.
**Rationale:** Works in Kotlin Multiplatform. **Hilt is Android-only** and cannot wire shared code.
**Rejected:** Hilt (Android-only), manual DI (boilerplate grows fast), kotlin-inject (less ecosystem/documentation).

## D-003 — Room KMP for local database — `Settled`
**Decision:** Room (androidx.room multiplatform) with KSP and the bundled SQLite driver. Entities + DAOs in `commonMain`; database *builder* per platform via `expect`/`actual` (Android needs `Context`; iOS uses a documents-directory path).
**Rationale:** First-party, type-safe, Flow-returning queries, mature migrations; the KMP variant keeps one schema for both platforms.
**Rejected:** SQLDelight (fine tool, but Room aligns with team experience and androidx tooling); raw SQLite (no type safety); Realm (sunset risk).

## D-004 — GitLive Firebase Kotlin SDK for Auth + Firestore — `Superseded by D-018 (auth) and D-020 (Firestore)`
**Original decision:** `dev.gitlive:firebase-auth` and `dev.gitlive:firebase-firestore` in `commonMain`, wrapping the native Firebase SDKs.
**Why superseded:** Authentication was removed entirely (D-018) and mandatory Firestore sync was replaced by opt-in Google Drive backup + CSV export (D-020). Firebase is no longer a dependency. Kept for history.

## D-005 — Offline-first: Room is the source of truth — `Amended by D-020`
**Decision:** Every read in the app comes from Room. Every write goes to Room first. The UI never waits on the network.
**Rationale:** Core product promise (NFR-3/4). Also simplifies the UI: one data source, reactive Flows.
**Amendment (D-020):** Room is now the **sole** store — there is no live cloud replica. The former "background sync engine replicates to Firestore" clause is replaced by opt-in backup/export. Room being the source of truth is more true than ever.
**Rejected:** Firestore SDK's built-in offline cache as primary store (opaque persistence guarantees, no local queries across our schema); dual-write (consistency bugs).

## D-006 — Background sync scheduled via expect/actual — `Superseded by D-022`
**Original decision:** A shared `SyncEngine` replicating to Firestore, scheduled by WorkManager/BGTaskScheduler on a periodic + expedited basis.
**Why superseded:** There is no mandatory sync anymore. The expect/actual scheduler seam is **retained** but now drives *optional auto-backup* only (D-022), and only when the user has enabled it.

## D-007 — Conflict resolution: last-write-wins by `updatedAt` — `Amended by D-024`
**Decision:** When the same record id exists with differing content, the version with the newer `updatedAt` (epoch millis, UTC, device clock) wins.
**Rationale:** Simple and predictable for an effectively single-writer app.
**Amendment (D-024):** With live sync gone, LWW no longer runs continuously — it applies **only during a restore merge** (reconciling a downloaded backup against the local DB). The rule itself is unchanged.
**Known trade-off:** device clock skew can pick the "wrong" winner on restore; acceptable.
**Rejected:** CRDTs / operational transforms (massive overkill); server-timestamp ordering (no server anymore).

## D-008 — Navigation: Compose Multiplatform Navigation — `Settled`
**Decision:** `org.jetbrains.androidx.navigation:navigation-compose` with **type-safe routes** (`@Serializable` route classes), single `NavHost` in the shared `App()`.
**Rationale:** Official JetBrains multiplatform port of Jetpack Navigation; matches Android developers' mental model; type-safe routes remove string bugs.
**Rejected:** Voyager / Decompose (capable, but third-party); manual state-based navigation (back handling, deep links become manual).

## D-009 — Deletes sync via soft-delete tombstones — `Superseded by D-025`
**Original decision:** Deleting a record set `deleted = true` + `syncStatus = PENDING` so the deletion could propagate to Firestore.
**Why superseded:** Without live sync there is nothing to propagate a deletion *to* continuously. A backup is a full snapshot, so a deleted record simply isn't in the next snapshot. Deletes are now immediate **hard deletes** (D-025); the `deleted` and `syncStatus` columns are gone.

## D-010 — Single `shared` module for now — `Settled`
**Decision:** All layers live in one `shared` KMP module, organized by package (see 04-architecture.md §4). Split into `core` / `data` / `domain` / `feature-*` Gradle modules only when build times or team size demand it.
**Rationale:** Small codebase + solo development; premature modularization slows iteration. Package discipline now makes the later split mechanical.

## D-011 — Secure storage: multiplatform-settings over encrypted backends — `Amended by D-018/D-021`
**Decision:** Key-value needs (theme, last-backup time, profile name) via `multiplatform-settings`; anything sensitive uses the secure `actual`s — EncryptedSharedPreferences (Android) / Keychain (iOS).
**Amendment:** There are no passwords or auth sessions anymore (D-018). Secure storage now guards the **Google Drive OAuth tokens** (when the user connects Drive, D-021) and any optional backup-encryption key material (D-026). Non-sensitive settings stay in plain `multiplatform-settings`.
**Rationale:** One common API, platform-appropriate encryption.

## D-012 — Charts: custom Compose Canvas — `Proposed`
**Decision (proposed):** Draw the analytics line charts with Compose `Canvas` in `commonMain` (a `VitalTrendChart` composable): axis, gridlines, points, path, min/max/avg overlay. Revisit a library (Vico multiplatform, Koala Plot) at the Analytics phase if custom drawing proves too costly.
**Rationale:** Chart needs are narrow (line + points + 3 ranges); a hand-rolled chart avoids betting on young KMP chart libraries.

## D-013 — Versions centralized in the Gradle version catalog — `Settled`
**Decision:** All dependency versions in `gradle/libs.versions.toml`; modules reference `libs.*`; inter-module deps via typesafe accessors (`projects.shared`). Never hardcode a version in a `build.gradle.kts`.

## D-014 — One account = one patient (patientId = Firebase UID) — `Superseded by D-018/D-019`
**Original decision:** The authenticated user's UID *was* the `patientId`; all data lived under `patients/{uid}`.
**Why superseded:** There are no accounts and no UID (D-018). The app has a single implicit local user; there is no `patientId` (D-019).

## D-015 — Logout keeps local data; different-user login wipes it — `Superseded by D-018`
**Why superseded:** There is no login or logout, so neither the "keep on logout" nor the "wipe on different user" case exists. Local data simply persists until the user deletes it or uninstalls.

## D-016 — Timestamps: epoch millis UTC; "today" in device-local timezone — `Settled`
**Decision:** `createdAt`/`updatedAt` stored as epoch milliseconds (UTC). The record's `date`/`time` fields are the *local* civil date/time of the reading (kotlinx-datetime `LocalDate`/`LocalTime` persisted as ISO-8601 strings). BR-2's "today" check compares the record's civil date to today's date in the device's current timezone.
**Rationale:** Vitals are inherently local-time facts ("BP at 8 am"); epoch timestamps are for ordering and restore-merge LWW (D-024) only.

## D-017 — English-only strings via Compose resources — `Settled`
**Decision:** All user-facing text through Compose Multiplatform resources (`Res.string.*`) from day one, even though MVP ships English-only (NFR-7).

---

## D-018 — No authentication; the app is local-only, no accounts — `Settled`
**Context (2026-07-04):** VitalCare is being open-sourced. Sign-up/login is friction that isn't needed for a personal vitals record book, and a mandatory account forces every contributor and user to depend on our backend.
**Decision:** Remove all authentication. There are no register/login/forgot-password screens, no session, no nav guard. The app opens straight to the Dashboard and is fully usable immediately. All data is stored locally (D-005) and belongs to the person holding the device.
**Rationale:** Zero friction to first use; no backend required to run the app; a clean, self-contained open-source project. Privacy improves — by default nothing ever leaves the device.
**Consequences:** supersedes D-004 (auth), D-014, D-015; removes feature F1 and doc 05's original auth content; removes the Firebase Auth dependency.
**Rejected:** Optional/anonymous accounts (still couples the app to a backend and adds surface area); keeping email/password "just in case" (unused complexity in an open-source repo).

## D-019 — Single local profile; no `patientId` — `Settled`
**Decision:** The app has one implicit local user. There is **no `patientId`** and no `patients` table keyed by an account. An optional local **profile** (just a display name, editable in Settings, stored in settings) may be shown in the UI and included in CSV/backup headers. Vital records carry no owner foreign key.
**Rationale:** With no accounts (D-018) there is nothing to key data by; a single-user model is the simplest correct thing.
**Future:** multi-profile (e.g. logging vitals for several family members) is a possible later feature — it would add a `profileId` and a profiles table, extending the schema rather than breaking it.
**Rejected:** Keeping a nullable `patientId` "for later" (dead column, misleading in an open-source schema).

## D-020 — Optional cloud backup (Google Drive) + CSV export, replacing mandatory Firestore sync — `Settled`
**Context:** Users still want a way to not lose their history and to move it off-device — but it should be their choice, not a requirement.
**Decision:** Replace the always-on Firestore sync engine with two **opt-in** mechanisms:
1. **CSV export** — write all (or filtered) records to a `.csv` file the user saves or shares. Pure on-device; needs no account and no network.
2. **Google Drive backup** — the user connects their own Google Drive and the app uploads a single versioned backup file (a full snapshot). Manual "Back up now" plus optional auto-backup (D-022). Restore downloads and merges (D-024).
**Rationale:** Preserves the "never lose a reading" promise without a backend we operate, without forcing sign-in, and in portable formats users control. Drive backups land in the user's *own* account.
**Consequences:** supersedes D-004 (Firestore) and the sync engine of D-006; amends D-005 (no replica); removes feature F7 as "sync" and replaces it with "Backup & Export"; the Ktor client (previously "future REST") is now used for the Drive REST API.
**Rejected:** Our own sync backend (operational cost, contributors can't run it); Firestore-only (backend dependency + account requirement); export-only with no cloud option (users on a new phone lose everything).

## D-021 — Google Sign-In is authorization for Drive only, never app login — `Settled`
**Decision:** Google Sign-In is invoked **solely** when the user chooses to connect Google Drive, purely to obtain an OAuth token for Drive. It is **not** an app-level login: the app is fully functional signed-out, and disconnecting Drive returns to the same signed-out-but-usable state. Request the **minimal Drive scope** — `https://www.googleapis.com/auth/drive.file` (app-created files only) — never full-Drive access. Tokens are held in secure storage (D-011).
**Rationale:** Keeps D-018 intact (no login gate) while enabling Drive; least-privilege scope means the app can never read the user's other Drive files.
**Open-source note:** using Drive requires a Google Cloud OAuth **client ID**, which is *contributor-supplied* config (D-027), not committed. The core app builds and runs without it; only the Drive feature needs it.
**Rejected:** Full `drive` scope (over-broad for a backup app); using Google Sign-In as the app's identity (re-introduces the login gate we removed).

## D-022 — Backup cadence: manual + optional auto (daily/weekly/monthly) — `Settled`
**Decision:** Backup to Drive runs (a) on demand via "Back up now", and (b) optionally on a schedule the user picks — **Off (default) / Daily / Weekly / Monthly**. Auto-backup reuses the platform scheduler seam from the old design: **WorkManager** (Android) / **BGTaskScheduler** (iOS) behind a common `BackupScheduler` interface, network-constrained. Auto-backup only runs when Drive is connected and a cadence other than Off is selected.
**Rationale:** Respects "reduce friction" (defaults to nothing running in the background) while still offering set-and-forget protection. Keeps ~95% of backup logic shared.
**Consequences:** supersedes D-006 (mandatory periodic sync).
**Rejected:** Always-on background sync (the friction/complexity D-018/D-020 removed); backup only on app foreground (misses users who rarely reopen the app).

## D-023 — Backup/export formats: versioned JSON backup + RFC 4180 CSV — `Settled`
**Decision:**
- **Backup file (Drive):** a single JSON document — `{ schemaVersion, exportedAt, appVersion, profileName?, records: [...] }` — serialized with kotlinx.serialization. Stored in Drive's hidden **`appDataFolder`** (D-021 scope covers it; invisible in the user's normal Drive UI, WhatsApp-style). One file, overwritten each backup (full snapshot).
- **CSV export:** RFC 4180 — header row + one row per record, columns `date,time,spo2,heart_rate,systolic,diastolic,remarks,created_at,updated_at`; fields with commas/quotes/newlines are quoted and quotes doubled.
- Both formats are documented in 06-data-and-storage.md so they're stable and portable.
**Rationale:** JSON round-trips losslessly for restore and is easy to version; CSV opens in any spreadsheet and is the universal "give me my data" format. `appDataFolder` keeps backups out of the user's file clutter and scoped to the app.
**Rejected:** SQLite file upload (opaque, version-fragile, not human-inspectable); proprietary binary format (hostile to an open-source, data-portable ethos).

## D-024 — Restore is a non-destructive merge by record id (LWW by `updatedAt`) — `Settled`
**Decision:** Restoring a backup **merges** it into the local DB: union by record `id`; when both sides have the same id, the newer `updatedAt` wins (D-007). Records that exist only locally are kept; records only in the backup are added. Restore never wipes local data. (A separate explicit "replace all" is a possible future option, gated behind a strong confirmation.)
**Rationale:** Safest default — a user restoring an old backup onto a phone with newer local readings must not lose the newer readings.
**Consequences:** rescopes D-007 (LWW now only runs here).
**Rejected:** Destructive replace-on-restore (silent data loss); append-without-dedup (duplicate records after restoring twice).

## D-025 — Deletes are immediate hard deletes; no tombstones — `Settled`
**Decision:** Deleting a record removes the row immediately (`DELETE FROM vital_records WHERE id = ?`). No `deleted` flag, no tombstone lifecycle. A backup taken after a delete simply won't contain the record.
**Rationale:** Tombstones existed only to propagate deletes to a live cloud store (D-009); with snapshot backups they're pointless. Simpler schema, simpler mental model.
**Trade-off:** restoring a backup that predates a deletion resurrects that record — expected snapshot/restore semantics (same as WhatsApp), and acceptable.
**Consequences:** supersedes D-009; removes the `deleted` and `syncStatus` columns.

## D-026 — Optional password-based backup encryption; default off — `Proposed`
**Decision (proposed):** Ship the Drive backup as plaintext JSON by default (it already lives in the user's own private Drive), and offer an **optional** "encrypt my backup with a password" toggle. When on, the backup blob is encrypted client-side (AES-GCM) from a password-derived key (PBKDF2/Argon2); the password is never stored and restore prompts for it.
**Rationale:** The stated goal is *reduced friction* — forcing key management (WhatsApp-style) contradicts that, and the data is already behind the user's Google account. Making encryption opt-in serves privacy-conscious users without taxing everyone. Confirm/veto before building the Drive feature.
**Trade-off:** a forgotten password means that backup is unrecoverable — surfaced clearly in the UI when enabling.
**Rejected (for MVP):** mandatory E2E encryption with a recovery key (the exact friction we're removing); no encryption option at all (some users legitimately want it).

## D-027 — Open-source posture: no-secret build; runnable with no setup — `Amended by D-028`
**Decision:** The repository must **build and run the full app with no secret configuration and no backend for user data**. No API keys, no auth, no Firestore. The Google Drive feature's OAuth **client ID(s)** are contributor-supplied (via `local.properties` / an untracked config, documented in the README) and are **never committed**. Ship `LICENSE`, `README`, and `CONTRIBUTING` (release phase).
**Amendment (D-028):** the Firebase **app config** files (`androidApp/google-services.json`, `iosApp/iosApp/GoogleService-Info.plist`) **are kept and committed** — they are client identifiers, not secrets, and power Analytics/Crashlytics only. The app still builds and runs without them (telemetry no-ops), so forks aren't blocked. The Firestore-only artifacts (`firestore.rules`, `firestore.indexes.json`, and the Firestore parts of `firebase.json`/`.firebaserc`) are **inactive** (no Firestore in use) but are not being deleted.
**Rationale:** An open-source app must be clonable-and-runnable by anyone in one step; the only never-committed item is the Drive OAuth client (abuse/quota risk).
**Rejected:** Committing a shared/demo OAuth client for Drive (abuse + quota risk, ties contributors to our Google project).

## D-028 — Firebase retained for Analytics + Crashlytics only (never auth or data) — `Settled`
**Context (2026-07-04):** maintainers want crash visibility and product usage insight even though auth and Firestore were removed (D-018/D-020).
**Decision:** Keep the Firebase project and its committed app config (D-027). Use Firebase **only** for **Analytics** and **Crashlytics**. **Firebase Auth and Firestore stay removed** — no user, health, or account data is ever written to Firebase. Telemetry is **strictly PHI-free** (D-011/§07/6): screen names, action counts, feature-usage flags, and crash stacks only — never vital values, remarks, or the profile name. Telemetry sits behind an `expect`/`actual` `Telemetry` seam in `commonMain`, with actuals wrapping the **native Firebase SDKs** per platform (Android: `firebase-analytics` + `firebase-crashlytics` via the Firebase BoM and the Crashlytics Gradle plugin; iOS: FirebaseAnalytics + FirebaseCrashlytics in `iosApp`; exact artifacts chosen at the telemetry phase). The app must build/run with the config absent (telemetry no-ops), preserving NFR-9 for forks. A **Settings opt-out** is offered (D-029/§03).
**Rationale:** Operational visibility for maintainers without compromising the local-first, no-account, private-by-default data model — the health data itself never touches Firebase.
**Trade-off:** anonymized, PHI-free telemetry does leave the device, softening the absolute "nothing leaves the device"; mitigated by the PHI-free rule and the opt-out. This is disclosed in the README/privacy note.
**Rejected:** No telemetry (flying blind on crashes in the field); routing PHI through analytics/crash reports (never — hard rule §07/6); a self-hosted analytics stack (ops burden; unnecessary when the data is PHI-free).

## D-029 — Telemetry opt-out in Settings; on by default — `Proposed`
**Decision (proposed):** Analytics + Crashlytics are enabled by default, with a **"Share anonymous usage & crash data"** toggle in Settings to turn them off. When off, no events or crash reports are sent. The choice is stored in settings and honored by the `Telemetry` seam.
**Rationale:** Pragmatic default that still yields useful signal, while respecting users who want zero outbound data — consistent with the private-by-default ethos. Confirm/veto (opt-in vs opt-out) before the telemetry phase.
**Rejected (for now):** Opt-in only (much lower signal); no control at all (poor fit for a health app being open-sourced).

## D-030 — Visual language: "Soft Clinical" (soft-pastel bento + bold type), tuned for accessibility — `Settled` (direction + primary color; display font `Proposed`)
**Context (2026-07-04):** the earlier "Material 3 Expressive (blue medical)" brief in DESIGN.md was never adopted as an ADR. The team picked a modern reference (a soft-pastel wellness / bento-tile look) to align the regenerated screens with.
**Decision:** Adopt a **"Soft Clinical"** design language — off-white backgrounds; muted multi-pastel **bento** tiles (sage / blue / lavender / peach / cream); bold geometric display type with big hero numerals; an **indigo primary `#4849A1`** (buttons, active states, hero tiles, nav indicator) with **near-black ink `#161616` for text**; full-pill shapes; circular floating icon buttons; soft diffuse depth; and one **limited warm accent** (orange `#F97A45`, complementary to the indigo). Implemented as a **custom Material 3 theme** (color scheme + shapes + typography + bespoke components) — **not** a framework change (D-001 stands). Retunes 03 §4 and the Stitch brief (DESIGN.md §Design language + Color).
**Guardrails (non-negotiable — override the aesthetic):** all body/number text is near-black on light tints (never pastel-on-pastel or light-on-pastel) so contrast ≥ 4.5:1 (NFR-6); white-on-indigo primary ≈ 7.6:1 (passes); out-of-range/error uses a clear **red + icon + label**, never a pastel or the accent; touch targets ≥ 48 dp (primary ≥ 56 dp); the settled "grandparent-proof" clarity (one primary action, big legible values) is preserved. This is what makes a consumer-wellness look safe for critical/elderly users.
**Primary color = indigo `#4849A1`** (chosen 2026-07-04; supersedes both the old `#2563EB` blue and the interim ink-black proposal). Still `Proposed`: display font = **Plus Jakarta Sans**; the exact pastel/tint hexes in 03 §4.1.
**Rejected:** copying the reference's low-contrast pastel-on-pastel text (fails elderly accessibility); a full neumorphic treatment (low contrast, poor a11y); dropping Material 3 as the toolkit (unnecessary — theme it instead).

---

## How to add a decision

1. Add the next `D-xxx` entry here (Context → Decision → Rationale → Rejected).
2. Update the affected plan docs to reference it.
3. Only then change code.
