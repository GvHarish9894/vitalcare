# 08 — Testing Strategy

## 1. Principles
- **Prefer `commonTest`** — a shared test runs on every target; platform test source sets
  (`androidHostTest`, `iosTest`) are only for platform-specific `actual`s.
- Test the *domain* hardest: validation, use cases, and the sync engine hold all the rules.
- Fakes over mocks: hand-written fake repositories/DAOs in `commonTest` (no mocking framework —
  most don't support Kotlin/Native anyway).
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
| **Use cases** | `commonTest`, fake repos | Save flow ordering (validate → save → schedule), BR-2 today-only edit/delete, analytics aggregation (averages, min/max, empty ranges) |
| **Sync engine** | `commonTest`, fake DAO + fake remote | Status transitions (§06/4), tombstone flow, poison-record isolation, backoff/FAILED after max attempts, stale-SYNCING recovery, LWW conflicts both directions |
| **Repositories** | `commonTest` | Mapping entity↔domain↔DTO, error mapping to `AppError` |
| **DAOs** | Room in-memory DB (`androidHostTest` first; iOS when driver setup permits) | Query correctness: today/range filters, tombstone exclusion, pending count, indexes used |
| **ViewModels** | `commonTest`, fake use cases, test dispatcher | UiState transitions per event, error surfacing, one-shot effects |
| **Auth** | `commonTest` with fake `AuthRepository` | State-driven nav guard, D-015 wipe-on-different-user, error mapping table (§05/7) |
| **Compose UI** | Compose Multiplatform UI tests (`androidHostTest` / `iosTest`) — smoke level | Record form validation display, dashboard empty/filled states |

## 4. Test data
- A shared `TestFixtures` object in `commonTest`: canonical valid `VitalRecord`, boundary
  records, tombstones, fixed `Clock`/`TimeZone` so "today" logic is deterministic (never use
  real system time in tests).

## 5. Coverage expectations
- Domain (validation + use cases + sync engine): near-total.
- ViewModels: all state transitions.
- UI tests: smoke-level only in MVP (the layers below carry the correctness load).
- Performance (NFR-1/2): manual measurement in Phase 8; no automated perf gates in MVP.
