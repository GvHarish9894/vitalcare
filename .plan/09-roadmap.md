# 09 — Roadmap

Build order with deliverables and acceptance criteria. A phase is **done** only when its
criteria pass on **both** Android and iOS. IDs reference [01-product-requirements.md](01-product-requirements.md).

> **Re-scoped 2026-07-04:** the Authentication phase was removed and the Sync phase was replaced
> by CSV Export + Google Drive Backup (D-018/D-020). Core feature phases moved earlier.

## Phase 1 — Foundation
**Scope:** project scaffolding that every feature builds on.
- Add to version catalog + wire: Koin, Room KMP (+KSP), kotlinx-datetime, kotlinx.serialization,
  multiplatform-settings, Compose Navigation, Ktor client.
- Package skeleton per [04-architecture.md](04-architecture.md) §3; `AppResult`/`AppError`; `DispatcherProvider`.
- `VitalCareTheme` (light + dark palettes, §03/4) + core design-system components.
- NavHost with placeholder screens + bottom bar (Dashboard start destination, **no auth graph**);
  Koin `initKoin` from both entry points.
- Room DB with the `vital_records` table + platform builders; `SecureSettings` actuals.
- Manifest hygiene: `allowBackup=false` (§07/3).
- **Telemetry (D-028):** wire Firebase **Analytics + Crashlytics** behind the `Telemetry` seam
  (`core/telemetry`, PHI-free); keep the committed app config; add the Crashlytics Gradle plugin
  (Android) and the native pods/SPM deps (iOS). Verify a forced test crash and a test event report.
  Firebase Auth/Firestore stay unused. Analytics *events* are instrumented per-feature thereafter.
- **Open-source hygiene (D-027):** no committed secrets (Drive OAuth client is contributor-supplied);
  confirm the app still builds and runs with the Firebase config **absent** (telemetry no-ops),
  preserving NFR-9.

**Acceptance:** app builds and runs the shared UI on Android + iOS simulator (with and without the
Firebase config present); a test crash reaches Crashlytics; no PHI in any event (§07/6); DB opens
on both; navigation between placeholder tabs works; `testAndroidHostTest` and
`iosSimulatorArm64Test` pass.

## Phase 2 — Record Vitals (F3) *(first, since everything else needs data)*
**Scope:** domain model, validator, DAO + repository, Record Vitals screen (create mode),
save-locally-first flow.
**Acceptance:** FR-R1..R3, BR-1/4/6, all §01/2 validation boundaries unit-tested; NFR-1 save < 200 ms;
record survives app kill (NFR-4).

## Phase 3 — Dashboard (F2)
**Scope:** today summary, latest reading, quick actions. (No sync/backup UI yet.)
**Acceptance:** FR-D1/D2/D4/D5; reactive update when a record is saved; empty states per §03/3.5.

## Phase 4 — History & Record Details (F4, F5)
**Scope:** grouped history list, filters, search, details screen, today-only edit + permanent delete.
**Acceptance:** FR-H1..H5, FR-RD1..RD2, BR-2/3/5; edit updates `updatedAt` (FR-R4);
deleted records are gone permanently (D-025).

## Phase 5 — Analytics (F6)
**Scope:** `GetAnalytics` use case (aggregation), `VitalTrendChart` (confirm/veto D-012 first),
Analytics screen with 3 ranges.
**Acceptance:** FR-AN1..AN4; aggregation unit-tested including empty/single-point ranges.

## Phase 6 — CSV Export (F7, part 1) *(no external deps — ship early)*
**Scope:** `CsvEncoder` (RFC 4180, §06/3), `ExportCsv` use case, platform `FileExporter`
(`expect`/`actual`), export entry points (Settings + History overflow), scope = current filter.
**Acceptance:** FR-B1, FR-H6; CSV encoding unit-tested (quoting/nulls/order); file saved & shared
on both platforms; NFR-5 (export reproduces every record); needs no account and no network.

## Phase 7 — Google Drive Backup & Restore (F7, part 2)
**Scope:** `BackupSerializer` (JSON, §06/4), shared `DriveClient` (Ktor → Drive REST,
`appDataFolder`), platform `DriveAuthorizer` (`drive.file` scope, D-021), `BackupNow` /
`RestoreFromDrive` / `ConnectDrive` / `Disconnect` use cases, `BackupScheduler` actuals + auto
cadence (D-022), Settings Backup subsection, `lastBackupAt` + optional Dashboard hint.
Contributor-supplied OAuth client wiring documented (D-027); build without it degrades gracefully.
**Acceptance:** FR-B2..B6; backup→restore round-trips losslessly and restore merge is
non-destructive + idempotent (NFR-5, D-024); Drive errors surface per §05/9; `drive.file` scope
only (§07/4); app still fully usable with Drive not configured.

## Phase 8 — Settings, Profile & Hardening (F8)
**Scope:** Settings screen (profile name, theme, backup section, about), accessibility pass
(§03/5), performance pass (NFR-1/2), full regression on both platforms.
Optional: password-encrypted backup (FR-B7/D-026) if confirmed.
**Acceptance:** FR-SE1..SE4; NFR-6 checklist; no PHI in logs (§07/6).

## Phase 9 — Open-source Release
**Scope:** `LICENSE`, `README` (features, screenshots, build-with-zero-config instructions,
optional Drive OAuth setup), `CONTRIBUTING`; verify no committed secrets (D-027); R8/minification
+ config; app icons/splash; store metadata; Play Store + App Store submission.
**Acceptance:** fresh clone builds and runs with no config; release builds pass full regression;
no secrets in git history; store review requirements met.

---

### Sequencing rules
- Phases are strictly ordered; do not start N+1 with N's acceptance criteria failing.
- Anything marked `Proposed` in these docs must be confirmed no later than the phase that builds it.
- Scope changes go through [02-design-decisions.md](02-design-decisions.md) first.
