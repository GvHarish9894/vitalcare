# Contributing to VitalCare

Thanks for helping! VitalCare is a local-first Kotlin Multiplatform app — one
shared codebase (logic **and** Compose UI) for Android and iOS.

## Ground rules

The `.plan/` directory is the source of truth: product requirements,
architecture, data model, UI specs, and an ADR log. **Read
[`.plan/10-guidelines.md`](.plan/10-guidelines.md) before writing code** — it
is short and binding (Clean Architecture layering, MVVM contract, KMP
discipline, no PHI in telemetry, strings via Compose resources, tests with
every feature). Want to change a decision? Amend
[`.plan/02-design-decisions.md`](.plan/02-design-decisions.md) first, then code.

## Building

A fresh clone builds with **zero configuration** (D-027):

```bash
# Android
./gradlew :androidApp:assembleDebug

# iOS — open iosApp/ in Xcode and run (Gradle builds the Shared framework)
```

The only prerequisites are a JDK 17+, the Android SDK, and (for iOS) Xcode.

## Testing

```bash
./gradlew :shared:testAndroidHostTest        # shared tests on the JVM
./gradlew :shared:iosSimulatorArm64Test      # shared tests on the iOS simulator
```

Run the host tests before every commit; run the iOS task too when touching
`expect`/`actual` or `iosMain` code. Never commit failing tests.

## Optional: Google Drive backup

The Drive feature is disabled by default and the app is fully usable without
it. To develop against it you need your own OAuth client (never commit one):

1. In Google Cloud Console, create an **OAuth client ID → Android** with
   package `com.techgv.vitalcare` and your debug signing SHA-1
   (`./gradlew :androidApp:signingReport`).
2. Enable the **Google Drive API** for the project.
3. Add `vitalcare.drive.enabled=true` to `local.properties`.

iOS additionally needs the GoogleSignIn SDK added in Xcode plus an iOS OAuth
client; pass `driveEnabled: true` to `KoinIOSKt.doInitKoin` when wiring it.

## Pull requests

- Small, focused commits with imperative subjects ("Add vitals validator").
- Match the existing package layout (`core` / `domain` / `data` / `feature`).
- Every user-facing string goes through `composeResources/values/strings.xml`.
- New feature → new tests in `commonTest`; bug fix → regression test.

By contributing you agree your contributions are licensed under the
Apache License 2.0 (see [LICENSE](LICENSE)).
