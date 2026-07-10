# CI / Release workflows

Adapted from the LocalOffers project for VitalCare's KMP layout (`:androidApp` +
`:shared`). Both are **manual/tag triggered** — nothing runs on ordinary pushes.

| Workflow | File | Trigger | What it does |
|---|---|---|---|
| Android Release | `android_release.yml` | `workflow_dispatch` (pick patch/minor/major) | Runs `:shared:testAndroidHostTest`, bumps `version.properties`, builds a **signed AAB + APK**, uploads the R8 mapping to Firebase Crashlytics, commits the version bump, and cuts a GitHub Release with the artifacts. |
| Firebase App Distribution | `firebase_app_distribution.yml` | `workflow_dispatch` or a `v*` tag push | Builds a **signed release APK** and ships it to Firebase App Distribution testers (+ uploads the R8 mapping). |

## Version source

Version lives in `version.properties` (`VERSION_NAME` / `VERSION_CODE`) at the repo
root. `androidApp/build.gradle.kts` reads it, overridable per build with
`-PVERSION_NAME=… -PVERSION_CODE=…`. `./gradlew -q :androidApp:printVersionName`
prints the current name (used by the workflows).

## Required GitHub secrets

Set these under **Settings → Secrets and variables → Actions**. Without them the
workflows fail at the signing/distribution step; a plain clone still builds an
**unsigned** release locally (no keystore present), so day-to-day dev needs none
of this.

| Secret | Used by | Notes |
|---|---|---|
| `KEYSTORE` | both | base64 of your `keystore.jks` (`base64 -i keystore.jks | pbcopy`). Decoded to `signing/keystore.jks` at build time. |
| `SIGNING_STORE_PASSWORD` | both | Keystore password. |
| `SIGNING_KEY_ALIAS` | both | Signing key alias. |
| `SIGNING_KEY_PASSWORD` | both | Key password. |
| `FIREBASE_APP_ID` | app distribution | Android Firebase app id (`1:…:android:…`). If unset, the distribute step self-skips. |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | app distribution | Service-account JSON with the *Firebase App Distribution Admin* role. |
| `GITHUB_TOKEN` | android release | Auto-provided by Actions; used to push the version-bump commit and create the release. |

**Crashlytics mapping upload** needs no extra secret — the Crashlytics Gradle
plugin uploads using the committed `androidApp/google-services.json`. Uploads only
happen when the build is invoked with `-PenableCrashlyticsMappingUpload` (the
workflows pass it); local builds never touch the network.

## Notes / deviations from LocalOffers

- **iOS workflow not included** (skipped by request). Add later mirroring
  `~/Projects/LocalOffers/.github/workflows/ios_release.yml`, adapting
  `iosApp.xcworkspace` → `iosApp/iosApp.xcodeproj` and adding an `exportOptions.plist`.
- **Sentry → Crashlytics.** LocalOffers uploaded symbols to Sentry; VitalCare uses
  Firebase Crashlytics (D-028), so those steps became the Crashlytics mapping upload.
- **No staging build type.** VitalCare is local-first with no DB environments, so the
  LocalOffers staging/production split is dropped — there is a single `release` build.
- The Android Release job creates the tag via `GITHUB_TOKEN`, which does **not**
  re-trigger the tag-based App Distribution workflow (Actions suppresses that to avoid
  recursion). Run App Distribution manually, or push a tag yourself, to distribute.
