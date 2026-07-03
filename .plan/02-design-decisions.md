# 02 — Design Decisions (ADR log)

Every significant technical or product decision, with its **why**. This is the file that keeps
the plan honest: when something must change, add or amend a decision here *first*, then update
the affected docs and code.

**Format:** each decision has an ID (`D-xxx`), a status, the decision, rationale, and the
alternatives that were rejected.

**Statuses:** `Settled` (locked in) · `Proposed` (recommended default — confirm/veto before the
relevant phase) · `Superseded by D-xxx`.

---

## D-001 — Kotlin Multiplatform + Compose Multiplatform, shared UI — `Settled`
**Context:** Android and iOS apps needed by a very small team.
**Decision:** One Kotlin codebase. Business logic *and* UI live in the `shared` module (Compose Multiplatform); `androidApp`/`iosApp` are thin entry points.
**Rationale:** One implementation of validation, sync, and screens; features ship to both platforms simultaneously.
**Rejected:** Native Android + native iOS (double effort); Flutter/React Native (team is Kotlin-first; KMP gives full native interop when needed); KMP with native UIs (UI is most of this app — sharing it is most of the win).

## D-002 — Koin for dependency injection — `Settled`
**Decision:** Koin in `commonMain` for all DI.
**Rationale:** Works in Kotlin Multiplatform. **Hilt is Android-only** and cannot wire shared code.
**Rejected:** Hilt (Android-only), manual DI (boilerplate grows fast), kotlin-inject (less ecosystem/documentation).

## D-003 — Room KMP for local database — `Settled`
**Decision:** Room (androidx.room multiplatform) with KSP and the bundled SQLite driver. Entities + DAOs in `commonMain`; database *builder* per platform via `expect`/`actual` (Android needs `Context`; iOS uses a documents-directory path).
**Rationale:** First-party, type-safe, Flow-returning queries, mature migrations; the KMP variant keeps one schema for both platforms.
**Rejected:** SQLDelight (fine tool, but Room aligns with team experience and androidx tooling); raw SQLite (no type safety); Realm (sunset risk).

## D-004 — GitLive Firebase Kotlin SDK for Auth + Firestore — `Settled`
**Decision:** `dev.gitlive:firebase-auth` and `dev.gitlive:firebase-firestore` in `commonMain`, wrapping the native Firebase SDKs on each platform.
**Rationale:** Only practical way to call Firebase from shared code; wraps the official SDKs (offline persistence, auth caching still work). Platform setup remains native: `google-services.json` (Android), `GoogleService-Info.plist` (iOS).
**Rejected:** Firebase REST API via Ktor (loses offline persistence + auth token management); duplicating Firebase calls per platform behind expect/actual (boilerplate, drift).

## D-005 — Offline-first: Room is the source of truth — `Settled`
**Decision:** Every read in the app comes from Room. Every write goes to Room first and is marked `PENDING`; a background sync engine replicates to Firestore. The UI never waits on the network.
**Rationale:** Core product promise (NFR-3/4). Also simplifies the UI: one data source, reactive Flows.
**Rejected:** Firestore SDK's built-in offline cache as primary store (opaque persistence guarantees, no local queries across our schema, sync status not inspectable); dual-write (consistency bugs).

## D-006 — Background sync scheduled via expect/actual — `Settled`
**Decision:** Sync *logic* is shared in `commonMain` (`SyncEngine`). Scheduling is platform-specific behind a common `SyncScheduler` interface: **WorkManager** (Android, network-constrained periodic + expedited one-shot) and **BGTaskScheduler** (iOS, `BGAppRefreshTask`), plus an on-foreground trigger on both platforms.
**Rationale:** No cross-platform background scheduler exists; this keeps 95 % of sync code shared.
**Rejected:** Foreground-only sync (readings could sit unsynced for days).

## D-007 — Conflict resolution: last-write-wins by `updatedAt` — `Settled`
**Decision:** When the same record differs locally and remotely, the version with the newer `updatedAt` (epoch millis, UTC, device clock) wins.
**Rationale:** Effectively single-writer (one patient, one or two devices); LWW is simple and predictable. Field-level merging is not worth the complexity for vitals records that are rarely edited.
**Known trade-off:** device clock skew can pick the "wrong" winner; acceptable for MVP.
**Rejected:** CRDTs / operational transforms (massive overkill); server-timestamp ordering (records must be orderable while offline).

## D-008 — Navigation: Compose Multiplatform Navigation — `Settled`
**Decision:** `org.jetbrains.androidx.navigation:navigation-compose` with **type-safe routes** (`@Serializable` route classes), single `NavHost` in the shared `App()`.
**Rationale:** Official JetBrains multiplatform port of Jetpack Navigation; matches Android developers' mental model; type-safe routes remove string bugs.
**Rejected:** Voyager / Decompose (capable, but third-party; official lib matches the rest of the androidx stack); manual state-based navigation (back handling, deep links become manual).

## D-009 — Deletes sync via soft-delete tombstones — `Settled`
**Decision:** Deleting a record sets `deleted = true` + `syncStatus = PENDING` locally (a tombstone, hidden from all UI queries). Sync then deletes the Firestore doc and only afterwards purges the local row. Records that were never synced are hard-deleted immediately.
**Rationale:** An offline delete must survive app restarts and eventually reach the cloud; without tombstones a deleted-offline record resurrects on next pull.
**Rejected:** Immediate hard delete + "pending deletes" side table (two sources of truth).

## D-010 — Single `shared` module for now — `Settled`
**Decision:** All layers live in one `shared` KMP module, organized by package (see 04-architecture.md §4). Split into `core` / `data` / `domain` / `feature-*` Gradle modules only when build times or team size demand it.
**Rationale:** Small codebase + solo development; premature modularization slows iteration. Package discipline now makes the later split mechanical.

## D-011 — Secure storage: multiplatform-settings over encrypted backends — `Settled`
**Decision:** Key-value needs (session flags, last-sync time, theme) via `multiplatform-settings`; anything sensitive uses the secure `actual`s — EncryptedSharedPreferences (Android) / Keychain (iOS). Passwords are **never** stored (Firebase SDK manages tokens).
**Rationale:** One common API, platform-appropriate encryption.

## D-012 — Charts: custom Compose Canvas — `Proposed`
**Decision (proposed):** Draw the analytics line charts with Compose `Canvas` in `commonMain` (a `VitalTrendChart` composable): axis, gridlines, points, path, min/max/avg overlay. Revisit a library (Vico multiplatform, Koala Plot) at Phase 6 if custom drawing proves too costly.
**Rationale:** Chart needs are narrow (line + points + 3 ranges); a hand-rolled chart avoids betting on young KMP chart libraries.

## D-013 — Versions centralized in the Gradle version catalog — `Settled`
**Decision:** All dependency versions in `gradle/libs.versions.toml`; modules reference `libs.*`; inter-module deps via typesafe accessors (`projects.shared`). Never hardcode a version in a `build.gradle.kts`.

## D-014 — One account = one patient (patientId = Firebase UID) — `Settled`
**Decision:** The MVP has no patient selection. The authenticated user's UID *is* the `patientId`; all data lives under `patients/{uid}` in Firestore.
**Rationale:** Radically simplifies auth→data mapping and Firestore security rules (`request.auth.uid == patientId`). Multi-patient/caregiver-linking is a Future feature that will extend the schema, not break it.

## D-015 — Logout keeps local data; different-user login wipes it — `Proposed`
**Decision (proposed):** On logout: stop sync, clear session-scoped settings, **keep** the local vitals database (data is inaccessible behind login). On login, if the new UID differs from the last-known UID, wipe the local database before use.
**Rationale:** Same-user re-login (common for elderly users who get logged out) must not lose unsynced offline data. The wipe-on-different-user rule prevents cross-account data leaks on shared devices.
**Rejected:** Always wipe on logout (destroys unsynced readings — unacceptable for a medical record book).

## D-016 — Timestamps: epoch millis UTC; "today" in device-local timezone — `Settled`
**Decision:** `createdAt`/`updatedAt` stored as epoch milliseconds (UTC). The record's `date`/`time` fields are the *local* civil date/time of the reading (kotlinx-datetime `LocalDate`/`LocalTime` persisted as ISO-8601 strings). BR-2's "today" check compares the record's civil date to today's date in the device's current timezone.
**Rationale:** Vitals are inherently local-time facts ("BP at 8 am"); epoch timestamps are for ordering and LWW only.

## D-017 — English-only strings via Compose resources — `Settled`
**Decision:** All user-facing text through Compose Multiplatform resources (`Res.string.*`) from day one, even though MVP ships English-only (NFR-7).

---

## How to add a decision

1. Add the next `D-xxx` entry here (Context → Decision → Rationale → Rejected).
2. Update the affected plan docs to reference it.
3. Only then change code.
