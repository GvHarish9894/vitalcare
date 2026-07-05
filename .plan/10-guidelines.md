# 10 — Coding Guidelines & AI Instructions

Rules for anyone — human or AI — writing code in this repo.

## 1. Role framing (for AI assistants)

Act as a **Senior Kotlin Multiplatform Engineer (Android + iOS)**. Follow the `.plan/` docs as
the source of truth; when the plan and an instinct disagree, the plan wins. If the plan is
silent, propose a decision in [02-design-decisions.md](02-design-decisions.md) before building.

## 2. Hard rules

1. **Kotlin only.** Swift only inside `iosApp` where iOS integration demands it.
2. **Compose Multiplatform only** for UI — shared in `shared/`, Material 3. No platform UI kits.
3. **Clean Architecture layering** per [04-architecture.md](04-architecture.md) §2 — the dependency
   rule is non-negotiable; no business logic in composables; no DAO/Firestore access from presentation.
4. **MVVM contract** per §04/4: one immutable `UiState` + sealed `Event` per screen, single
   `StateFlow`, effects as one-shot flows.
5. **KMP discipline:** shared code in `commonMain` and platform-agnostic; **no Android/iOS APIs
   in `commonMain`** — platform code goes behind `expect`/`actual` in `androidMain`/`iosMain`,
   kept as thin as possible. Every library added must support KMP (Koin, Room KMP, Ktor, kotlinx-*).
6. **Koin for DI** (not Hilt — Hilt is Android-only). **Room KMP** is the only data store — no
   login, no Firestore, no backend for user data (D-018/D-020). Google Drive backup uses **Ktor**
   (Drive REST). **Firebase is used ONLY for Analytics + Crashlytics** (D-028) — never for auth or data.
7. **Versions only in `gradle/libs.versions.toml`** (D-013); reference via `libs.*` /
   `projects.shared`. Never hardcode a version in a module build file.
8. **All user-facing strings via Compose resources** (D-017) — no hardcoded literals.
9. **No PHI off-device** (§07/6, D-028): never log — or send to Analytics/Crashlytics — vital
   values, remarks, or the profile name. Telemetry carries counts, screen names, and crash stacks only.
10. **No committed secrets** (D-027): the app must build and run with no secret configuration;
    Drive OAuth client IDs are contributor-supplied and never committed. The Firebase telemetry
    config (`google-services.json` / plist) is committed (client identifiers, not secrets, D-028).
11. **Tests required** with every feature — prefer `commonTest`; follow [08-testing.md](08-testing.md).
    Every bug fix includes a regression test.

## 3. Kotlin style

- Official Kotlin style (`kotlin.code.style=official`).
- Immutability by default: `val`, `data class`, immutable collections in UI state.
- Coroutines + Flow for all async; **no callbacks, no runBlocking in production code**;
  dispatchers via injected `DispatcherProvider`, never hardcoded `Dispatchers.IO`.
- Errors as `AppResult`/`AppError` at layer boundaries; exceptions don't cross the data layer.
- Small functions; small composables (extract when a composable exceeds ~50 lines or nests deeply).
- Naming: `*Screen` / `*ViewModel` / `*UiState` / `*Event` / `*Effect`; use cases are verb-phrases
  (`SaveVitalRecord`); DAOs `*Dao`; entities `*Entity`; backup/export DTOs `*Dto`.
- KDoc on public APIs of `domain/` and `core/`; comments explain *why*, not *what*.

## 4. Compose conventions

- State hoisting: screens take `uiState` + `onEvent`; only the top-level screen composable
  touches the ViewModel (`koinViewModel()`).
- `collectAsStateWithLifecycle()` for state observation.
- Stable/immutable types in composable parameters; avoid unstable lambdas in hot lists.
- Previews (`@Preview`) for every design-system component and screen state where practical.
- Reuse `core/designsystem` components (§03/4.5) — never restyle a raw Material component inline.

## 5. Git conventions

- Small, focused commits; imperative subject ("Add vitals validator", not "Added").
- Never commit failing tests to `master`; run `:shared:testAndroidHostTest` before committing,
  plus `:shared:iosSimulatorArm64Test` when touching expect/actual or iOS code.
- Plan changes (`.plan/`) commit separately from code when feasible.

## 6. Definition of done (any task)

1. Builds on Android (`:androidApp:assembleDebug`) and compiles for iOS.
2. Tests written and green on the host task (and iOS task when relevant).
3. Follows every hard rule above.
4. Matches the relevant `.plan/` spec — or the plan was updated first.
