# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

- **Product:** "VitalCare", a **local-first, no-account patient-vitals app** — record vitals, save on-device, show history/analytics. **No login/signup and no backend for user data** (D-018). Backing up is **optional**: CSV export or the user's own Google Drive (D-020). To be **open-sourced** — builds/runs with no secret config (D-027).
- **Platform:** **Kotlin Multiplatform (KMP) + Compose Multiplatform**, targeting **Android and iOS** from one shared codebase. This is settled — the project is KMP, not Android-only.
- **State:** the app is **built** — all MVP features exist (Dashboard, Record Vitals, History, Record Details, Analytics, Settings), backed by Room, Koin DI, Google Drive backup + CSV export, and a shared Compose UI ("Soft Clinical" design system), with tests. The only template remnant is an unused `Platform`/`getPlatform()` expect/actual. **Fluid Balance (F9, D-032)** — water intake + urine output, a separate feature — is in progress. `.plan/` remains the source of truth for intent.

`.plan/` (00–10, numbered by topic — overview, requirements, design decisions, UI/UX, architecture, backup-and-export, data-and-storage, security, testing, roadmap, guidelines) is the source of truth for intent. Decisions are logged ADR-style in `.plan/02-design-decisions.md` — change a decision there first, then code. **Scope was simplified 2026-07-04 (D-018…D-027): auth and Firestore sync removed; see that ADR block.** The chosen stack is:

- **DI:** Koin (not Hilt — Hilt is Android-only)
- **Local DB:** Room KMP (entities/DAOs in `commonMain`; DB builder per platform via `expect`/`actual`) — the **only** data store; no cloud replica
- **Backup/export (optional):** CSV export via a platform `FileExporter` (`expect`/`actual`); Google Drive backup via **Ktor** (Drive REST, `appDataFolder`) with authorization-only Google Sign-In (`drive.file` scope, D-021). Optional auto-backup scheduled via an `expect`/`actual` scheduler — WorkManager on Android, BGTaskScheduler on iOS (D-022)
- **No auth, no Firestore, no cloud user data** (D-018/D-020) — no account, no data backend. **Firebase is used ONLY for Analytics + Crashlytics** (D-028) — PHI-free, opt-out; its config files (`google-services.json` / `GoogleService-Info.plist`) stay committed. Telemetry sits behind an `expect`/`actual` `Telemetry` seam.
- **Also:** kotlinx.serialization (backup JSON), kotlinx-datetime, multiplatform-settings + secure storage (`expect`/`actual`: EncryptedSharedPreferences / Keychain — guards the Drive token)

None of these libraries are wired up yet — add them to `gradle/libs.versions.toml` as features are built. When picking a library, KMP support is a hard requirement: keep `commonMain` platform-agnostic and push anything platform-specific into `androidMain`/`iosMain` via `expect`/`actual`.

## Module structure

- `shared/` — KMP library module (namespace `com.techgv.vitalcare.shared`). Shared logic **and** shared Compose UI live here.
  - `src/commonMain` — code for all targets; `src/androidMain` & `src/iosMain` — platform `actual` implementations; `src/commonTest`, `src/androidHostTest`, `src/iosTest` — tests per source set.
  - Cross-platform code uses `expect`/`actual` (see `Platform.kt` → `Platform.android.kt` / `Platform.ios.kt`).
- `androidApp/` — Android application module; `MainActivity` hosts the shared `App()` composable.
- `iosApp/` — Xcode project; entry point for the iOS app and any SwiftUI. Consumes `shared` as a static framework named `Shared`.

Base package / applicationId is `com.techgv.vitalcare` (shared module namespace `com.techgv.vitalcare.shared`). Compose resources are generated under `vitalcare.shared.generated.resources` — that package is derived from `rootProject.name` (`VitalCare`), so if the project is ever renamed again, update those imports too.

## Commands

Build:
- Android debug APK: `./gradlew :androidApp:assembleDebug`
- iOS: open `iosApp/` in Xcode and run (Gradle assembles the `Shared` framework as part of the Xcode build).

Test:
- Shared tests on JVM/host: `./gradlew :shared:testAndroidHostTest`
- Shared tests on iOS simulator: `./gradlew :shared:iosSimulatorArm64Test`
- Single test (host task): `./gradlew :shared:testAndroidHostTest --tests "com.techgv.vitalcare.SharedCommonTest"` (append `.example` for a single method).
- `commonTest` code runs through each target's test task above; there is no standalone "commonTest" task.

Note: there is **no lint task**. CI is **release-only** — `.github/workflows/` has `android_release.yml` (manual version-bump → signed AAB/APK + GitHub release) and `firebase_app_distribution.yml` (signed APK → Firebase App Distribution), both manual/tag-triggered (nothing runs on ordinary pushes). Version lives in `version.properties`; `androidApp` signs release only when `signing/keystore.jks` is present (unsigned otherwise, so plain clones build). See `.github/workflows/README.md` for required secrets. For local verification, `assembleDebug` / the test tasks remain the go-to.

## Toolchain & conventions

- Versions are centralized in `gradle/libs.versions.toml` (the version catalog). Add/upgrade dependencies there and reference via `libs.*`; never hardcode versions in a module's `build.gradle.kts`. Inter-module deps use typesafe accessors (`projects.shared`).
- Kotlin **2.4.0**, AGP **9.0.1** (via the `com.android.kotlin.multiplatform.library` plugin for `shared`), Compose Multiplatform **1.11.1**, Material 3 **1.11.0-alpha07**, JVM target **11**, `compileSdk`/`targetSdk` 36, `minSdk` 24.
- The `shared` module uses AGP 9's `androidLibrary { }` KMP DSL (not the legacy `com.android.library`). Keep that in mind for the AGP-9 KMP skill if build issues arise.
- Gradle **configuration cache and build cache are enabled** (`gradle.properties`). Custom Gradle tasks/plugins must be configuration-cache compatible.

## Target design (from `.plan/`, for when building features)

Before writing feature code, read `.plan/10-guidelines.md` (coding rules + AI directives); the numbered `.plan/` docs are the spec for each subsystem — 03 UI/UX (per-screen specs + design system), 04 architecture, 05 backup-and-export, 06 data-and-storage, 07 security. Build order and acceptance criteria live in `.plan/09-roadmap.md`.

- **Local-first flow:** validate → save to local DB → update UI. That's it — no network, no sync, no per-record status. Room is the source of truth (D-005). Deletes are permanent hard deletes (D-025).
- **Backup/export (opt-in only):** CSV export produces an RFC 4180 file the user saves/shares (06 §3). Google Drive backup uploads one versioned JSON snapshot to the user's `appDataFolder`, overwriting the previous one; restore **merges** it back in, newer-`updatedAt`-wins, never destructive (D-023/D-024).
- **Vitals validation ranges:** SpO₂ 70–100, Heart Rate 20–250, Systolic BP 50–250, Diastolic BP 30–180.
- **Layering:** Clean Architecture (Presentation / Domain / Data), MVVM, Repository pattern, immutable UI state, Coroutines + Flow, no business logic in Composables.
