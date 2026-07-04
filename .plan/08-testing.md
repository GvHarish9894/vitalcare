# 08 — Testing Strategy

## 1. Principles
- **Prefer `commonTest`** — a shared test runs on every target; platform test source sets
  (`androidHostTest`, `iosTest`) are only for platform-specific `actual`s.
- Test the *domain* hardest: validation, use cases, and the backup/restore logic hold all the rules.
- Fakes over mocks: hand-written fake repositories/DAOs/`DriveClient` in `commonTest` (no mocking
  framework — most don't support Kotlin/Native anyway).
- Every bug fix lands with a regression test.

## 2. Commands

| What | Command |
|---|---|
| Shared tests on JVM/host (fast, default) | `./gradlew :shared:testAndroidHostTest` |
| Shared tests on iOS simulator | `./gradlew :shared:iosSimulatorArm64Test` |
| Single class | `./gradlew :shared:testAndroidHostTest --tests "com.techgv.vitalcare.SomeTest"` |
| Single method | append `.methodName` to the class filter |

There is no standalone "commonTest task" — common tests execute through each target's task.
CI (Future): run both tasks on every PR.

## 3. What gets tested, per layer

| Layer | Tests (location) | Focus |
|---|---|---|
| **Validation** | `commonTest` | Every boundary of every range (69/70/100/101 for SpO₂ …), dia < sys cross-field, at-least-one-vital, remarks length, future-time rejection |
| **Use cases** | `commonTest`, fake repos | Save flow ordering (validate → save), BR-2 today-only edit/delete, hard-delete (D-025), analytics aggregation (averages, min/max, empty ranges) |
| **CSV export** | `commonTest` | RFC 4180 encoding: quoting fields with commas/quotes/newlines, doubled quotes, null vitals → empty cells, header + row order, filtered scope |
| **Backup serialize/restore** | `commonTest`, fake DAO | JSON round-trip (records → JSON → records is identity); restore merge by id with newer-`updatedAt`-wins (D-024); local-only records preserved; idempotent re-restore; unsupported `schemaVersion` rejected; (D-026) encrypt→decrypt round-trip and wrong-password failure |
| **DriveClient** | `commonTest`, Ktor `MockEngine` | `appDataFolder` file find/create/overwrite requests, auth header present, error-status → `AppError.Drive` mapping |
| **Repositories** | `commonTest` | Mapping entity↔domain↔DTO↔CSV; error mapping to `AppError` |
| **DAOs** | Room in-memory DB (`androidHostTest` first; iOS when driver setup permits) | Query correctness: today/range filters, `getAll`/`upsertAll`, `countSince`, index used |
| **ViewModels** | `commonTest`, fake use cases, test dispatcher | UiState transitions per event, error surfacing, one-shot effects (share CSV, snackbars) |
| **Compose UI** | Compose Multiplatform UI tests (`androidHostTest` / `iosTest`) — smoke level | Record form validation display, dashboard empty/filled states, Settings backup section states (not-connected / connected / not-configured-in-build) |

*(No auth tests and no sync-engine tests — both were removed, D-018/D-020.)*

## 4. Test data
- A shared `TestFixtures` object in `commonTest`: canonical valid `VitalRecord`, boundary
  records, a sample `BackupFile`, fixed `Clock`/`TimeZone` so "today" logic and `updatedAt`
  comparisons are deterministic (never use real system time in tests).

## 5. Coverage expectations
- Domain (validation + use cases + backup/restore + CSV): near-total.
- ViewModels: all state transitions.
- UI tests: smoke-level only in MVP (the layers below carry the correctness load).
- Performance (NFR-1/2): manual measurement in the hardening phase; no automated perf gates in MVP.
