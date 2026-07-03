# 09 — Roadmap

Build order with deliverables and acceptance criteria. A phase is **done** only when its
criteria pass on **both** Android and iOS. IDs reference [01-product-requirements.md](01-product-requirements.md).

## Phase 1 — Foundation
**Scope:** project scaffolding that every feature builds on.
- Add to version catalog + wire: Koin, Room KMP (+KSP), kotlinx-datetime, kotlinx.serialization,
  multiplatform-settings, Compose Navigation.
- Package skeleton per [04-architecture.md](04-architecture.md) §3; `AppResult`/`AppError`; `DispatcherProvider`.
- `VitalCareTheme` (light + dark palettes, §03/4) + core design-system components.
- NavHost with placeholder screens + bottom bar; Koin `initKoin` from both entry points.
- Room DB with `vital_records`/`patients` tables + platform builders; `SecureSettings` actuals.
- Manifest hygiene: `allowBackup=false` (§07/3).

**Acceptance:** app builds and runs the shared UI on Android + iOS simulator; DB opens on both;
navigation between placeholder tabs works; `testAndroidHostTest` and `iosSimulatorArm64Test` pass.

## Phase 2 — Authentication (F1)
**Scope:** Firebase project + apps, GitLive auth, Login/Register/Forgot screens, session restore,
nav guard, Firestore security rules deployed, D-015 wipe logic.
**Acceptance:** FR-A1..A6; §05/7 error mapping; rules deny cross-user access (verified with the
rules emulator); relaunch lands logged-in user on Dashboard with no network.

## Phase 3 — Record Vitals (F3) *(moved before Dashboard: Dashboard needs data to show)*
**Scope:** domain model, validator, DAO + repository, Record Vitals screen (create mode),
save-local-first flow (records stay `PENDING` — sync comes in Phase 7).
**Acceptance:** FR-R1..R3, BR-1/4/6, all §01/2 validation boundaries unit-tested; NFR-1 save < 200 ms;
record survives app kill (NFR-4).

## Phase 4 — Dashboard (F2)
**Scope:** today summary, latest reading, quick actions, sync pill (shows Pending/Offline states;
"synced" appears in Phase 7).
**Acceptance:** FR-D1..D5; reactive update when a record is saved; empty states per §03/3.5.

## Phase 5 — History & Record Details (F4, F5)
**Scope:** grouped history list, filters, search, details screen, today-only edit + tombstone delete.
**Acceptance:** FR-H1..H5, FR-RD1..RD2, BR-2/3/5; edit resets record to `PENDING` (FR-R4);
deleted records vanish from all queries.

## Phase 6 — Analytics (F6)
**Scope:** `GetAnalytics` use case (aggregation), `VitalTrendChart` (confirm/veto D-012 first),
Analytics screen with 3 ranges.
**Acceptance:** FR-AN1..AN4; aggregation unit-tested including empty/single-point ranges.

## Phase 7 — Sync Engine (F7)
**Scope:** Firestore vitals source, `SyncEngine` (§06/5), `SyncScheduler` actuals
(WorkManager / BGTaskScheduler), foreground + manual triggers, backoff, tombstone completion,
initial download on fresh device, sync status surfaced end-to-end.
**Acceptance:** FR-S1..S4; airplane-mode scenario: record offline → go online → synced within
1 min foreground (NFR-5); kill mid-sync → no data loss, `SYNCING` rows recover; Firestore
document shape matches §06/3.

## Phase 8 — Settings + Hardening (F8)
**Scope:** Settings screen (theme, sync info, logout), accessibility pass (§03/5),
performance pass (NFR-1/2), full regression on both platforms.
**Acceptance:** FR-SE1..SE5; NFR-6 checklist; no PHI in logs (§07/6).

## Phase 9 — Release
**Scope:** R8/minification + config, app icons/splash, store metadata, account-deletion flow
(§07/8 — store requirement), Play Store + App Store submission.
**Acceptance:** release builds pass full regression; store review requirements met.

---

### Sequencing rules
- Phases are strictly ordered; do not start N+1 with N's acceptance criteria failing.
- Anything marked `Proposed` in these docs must be confirmed no later than the phase that builds it.
- Scope changes go through [02-design-decisions.md](02-design-decisions.md) first.
