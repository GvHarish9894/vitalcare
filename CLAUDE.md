# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

- **Product:** "VitalCare", an **offline-first patient-vitals app** — record vitals, save locally first, sync to Firestore when online, show history/analytics.
- **Platform:** **Kotlin Multiplatform (KMP) + Compose Multiplatform**, targeting **Android and iOS** from one shared codebase. This is settled — the project is KMP, not Android-only.
- **State:** only the default template code exists so far (`App.kt` with a "Click me!" button, `Greeting`, `Platform` expect/actual). No product features are built yet.

`.plan/` (00–15, numbered by topic) is the reconciled source of truth for intent — vision, requirements, data model, sync design, security, roadmap. Its tech-stack docs have been aligned to KMP; the chosen stack is:

- **DI:** Koin (not Hilt — Hilt is Android-only)
- **Local DB:** Room KMP (entities/DAOs in `commonMain`; DB builder per platform via `expect`/`actual`)
- **Background sync:** shared sync logic in `commonMain`, scheduled via an `expect`/`actual` scheduler — WorkManager on Android, BGTaskScheduler on iOS
- **Auth + cloud:** GitLive Firebase Kotlin SDK (`dev.gitlive:firebase-auth` / `-firestore`) wrapping the native Firebase SDKs
- **Also:** kotlinx.serialization, Ktor client (future REST), kotlinx-datetime, multiplatform-settings + secure storage (`expect`/`actual`: EncryptedSharedPreferences / Keychain)

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

Note: there is currently **no lint task and no CI configured**. `assembleDebug` / the test tasks are the only verification available.

## Toolchain & conventions

- Versions are centralized in `gradle/libs.versions.toml` (the version catalog). Add/upgrade dependencies there and reference via `libs.*`; never hardcode versions in a module's `build.gradle.kts`. Inter-module deps use typesafe accessors (`projects.shared`).
- Kotlin **2.4.0**, AGP **9.0.1** (via the `com.android.kotlin.multiplatform.library` plugin for `shared`), Compose Multiplatform **1.11.1**, Material 3 **1.11.0-alpha07**, JVM target **11**, `compileSdk`/`targetSdk` 36, `minSdk` 24.
- The `shared` module uses AGP 9's `androidLibrary { }` KMP DSL (not the legacy `com.android.library`). Keep that in mind for the AGP-9 KMP skill if build issues arise.
- Gradle **configuration cache and build cache are enabled** (`gradle.properties`). Custom Gradle tasks/plugins must be configuration-cache compatible.

## Target design (from `.plan/`, for when building features)

Before writing feature code, read `.plan/14-coding-guidelines.md` (the coding rules) and `.plan/15-ai-instructions.md` (the AI build directives); the numbered `.plan/` docs (07 data-model, 08 database, 09 sync-engine, 11 security) are the spec for each subsystem.

- **Offline-first flow:** validate → save to local DB (marked `Pending`) → update UI → queue background sync. Sync uploads `Pending` records to Firestore (`patients/{patientId}/vitals/{recordId}`), marks `Synced`, retries with exponential backoff. Conflict resolution: last-updated-timestamp wins.
- **Vitals validation ranges:** SpO₂ 70–100, Heart Rate 20–250, Systolic BP 50–250, Diastolic BP 30–180.
- **Layering:** Clean Architecture (Presentation / Domain / Data), MVVM, Repository pattern, immutable UI state, Coroutines + Flow, no business logic in Composables.
