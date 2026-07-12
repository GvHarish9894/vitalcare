# Play Store Submission Checklist — VitalCare

This is the pre-launch checklist for publishing VitalCare to Google Play. Items
marked **[repo]** are handled in this repository; items marked **[console]** are
done in the Google Play Console; **[external]** items live outside both.

> VitalCare stores **health data**, so Google applies extra scrutiny. The
> Data Safety form and a hosted privacy policy are hard requirements.

## Build & signing — DONE [repo]
- [x] Signed **AAB** produced by CI (`android_release.yml` → `bundleRelease`)
- [x] `targetSdk 36` (exceeds Play's current API-35 minimum for new apps)
- [x] `minSdk 24`
- [x] R8 minification + resource shrinking enabled for release
- [x] `allowBackup=false` (health data excluded from OS device-transfer backups)
- [x] Custom adaptive launcher icon (not the template icon)
- [x] Least-privilege permissions (only `POST_NOTIFICATIONS`, requested at runtime)
- [x] R8 mapping uploaded to Crashlytics for de-obfuscated crash traces

## Legal & policy content — DONE in repo, needs hosting [repo] + [external]
- [x] **Privacy policy** written → `PRIVACY.md`
- [ ] **Host the privacy policy at a public URL.** Options:
      - GitHub blob URL (accepted by Play):
        `https://github.com/GvHarish9894/vitalcare/blob/master/PRIVACY.md`
      - **Preferred:** enable GitHub Pages and serve it at a clean URL, e.g.
        `https://gvharish9894.github.io/vitalcare/privacy-policy`
- [x] **In-app privacy-policy link** added (Settings → About)
- [x] **In-app medical disclaimer** added (Settings → About)

## Store listing assets — TEXT DONE, GRAPHICS NEEDED
- [x] App title → `fastlane/metadata/android/en-US/title.txt`
- [x] Short description → `.../short_description.txt` (≤ 80 chars)
- [x] Full description → `.../full_description.txt` (≤ 4000 chars)
- [x] Release notes → `.../changelogs/1.txt` (matches `versionCode 1`)
- [ ] **App icon** — 512×512 PNG, 32-bit (derive from the in-app adaptive icon) [console]
- [ ] **Feature graphic** — 1024×500 PNG/JPG (required) [console]
- [ ] **Phone screenshots** — 2–8, 16:9 or 9:16, min 320px (required) [console]
      Suggested: Dashboard, Record Vitals, History, Analytics, Fluid balance,
      Settings. Capture on an emulator (see the emulator smoke-testing notes).
- [ ] *(optional)* 7" / 10" tablet screenshots

## Play Console configuration — [console]
- [ ] Enroll in **Play App Signing** (first upload)
- [ ] **Data Safety form** — declare exactly what the shipping build sends.
      NOTE: the runtime Crashlytics/Analytics SDK is **not wired yet** (only the
      mapping-upload plugin is — see `androidApp/build.gradle.kts`). Confirm what
      the release build actually transmits before answering. If telemetry is not
      live, the honest answer is "no data collected/shared", which is simplest.
      If it is live, declare: Crash logs + App diagnostics, anonymous, not linked
      to identity, with a user opt-out.
- [ ] **Health apps declaration** — choose category **Medical** or
      **Health & Fitness** and complete Google's health-apps declaration.
- [ ] **Content rating** questionnaire (IARC)
- [ ] **Target audience & content** (not directed at children)
- [ ] **Ads** declaration → contains no ads
- [ ] **Government apps** / **News** → N/A
- [ ] App category, contact email, and website
- [ ] Countries/regions for distribution

## Pre-production testing — [console]
- [ ] If this is a **newer personal developer account**, Google requires a
      **closed test with 12+ testers for 14 days** before you can promote to
      production. Plan for this lead time.

## Google Drive backup decision — [repo] + [console]
- The `DRIVE_ENABLED` flag defaults to **false** (OAuth client is
  contributor-supplied). Decide before submitting:
  - **Ship with Drive OFF** → the Data Safety "no data shared" story is clean.
  - **Ship with Drive ON** → register the OAuth client (package + signing SHA-1)
    and disclose Google Drive as a user-controlled destination in Data Safety.

## Final checks — [repo]
- [ ] Bump `version.properties` for the store build if needed (currently
      `1.0.0` / code `1`)
- [ ] Verify a release build installs and runs from the AAB (bundletool or an
      internal-testing track)
- [ ] Confirm no committed secrets (D-027)
