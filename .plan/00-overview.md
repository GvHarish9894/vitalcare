# 00 — VitalCare Overview

> **This directory (`.plan/`) is the source of truth for VitalCare.** Every feature, screen,
> and technical choice is specified here before it is built. If code and plan disagree, the
> plan wins — or the plan must be updated first (see [02-design-decisions.md](02-design-decisions.md)).

## 1. Vision

VitalCare is a **local-first, no-account patient-vitals app** for home monitoring of critical
patients. Caregivers and patients record vitals (SpO₂, heart rate, blood pressure) multiple times
per day. Every reading is **saved on the device** — the app is fully usable with no network, no
sign-up, and no backend. Backing up is entirely optional: **export to CSV**, or connect your own
**Google Drive**. The app provides history and trend analytics so caregivers can see how a patient
is doing over time. It is **open-source** and builds and runs with no secret configuration.

## 2. Problem statement

Home monitoring of critical patients today is done on paper or in generic note apps:

- Readings get lost, are hard to search, and impossible to chart.
- Cloud-only apps demand an account and connectivity — friction for elderly users and useless in
  poor-connectivity homes.
- Elderly users and stressed caregivers need a dead-simple, large-touch-target UI with nothing
  standing between opening the app and recording a reading.

VitalCare solves this with a private, on-device record book that works instantly and offline,
and lets the user back up their data on their own terms.

## 3. Target users

| User | Needs | Priority |
|---|---|---|
| **Critical patients** (often elderly) | Record own vitals with minimal taps; large text and controls; no login | MVP |
| **Family caregivers** | Record vitals for the patient; review history and trends; keep their own backup | MVP |
| **Doctors** | Receive a patient's history (e.g. a CSV) | Via CSV export today; portal is Future |

## 4. Goals

1. Record vitals multiple times per day, in under 30 seconds per entry.
2. **Open, record, done** — no account, no login, no network ever required to use the app.
3. Keep data private by default — nothing leaves the device unless the user chooses to export or back up.
4. Provide historical trends and daily/weekly/monthly charts.
5. Let users own their data: portable **CSV export** and optional **Google Drive** backup/restore.
6. Be simple and accessible enough for elderly users and stressed caregivers.
7. Share business logic **and UI** across Android and iOS from a single Kotlin codebase.
8. Be a clean **open-source** project: zero-config build, no committed secrets (D-027).

## 5. Non-goals (explicitly out of scope for MVP)

- Accounts, login, or cloud user identity — there are none (D-018).
- A backend we operate for user/health data — none exists; backup goes to the *user's* Google
  Drive (D-020). (Firebase is used only for anonymous, PHI-free Analytics/Crashlytics, D-028.)
- Bluetooth / device integration (pulse oximeters, BP cuffs) — manual entry only.
- Multi-patient/multi-profile management — single local user (D-019); multi-profile is Future.
- Doctor-facing portal or sharing beyond CSV export — future.
- Medication tracking, appointment reminders, push notifications.
- Web or desktop targets.

## 6. Core principles

- **Local First** — the on-device database is the only source of truth; the cloud is an optional, user-owned backup.
- **Zero Friction** — no sign-up, no login, no setup; the app works the instant it opens.
- **Private by Default** — health data stays on the device unless the user explicitly exports or backs up (D-020); only anonymous, PHI-free usage/crash telemetry leaves by default, and it can be turned off (D-028/D-029).
- **User-Owned Data** — portable formats (CSV) and the user's own Drive; we hold nothing.
- **Cross-Platform** — share everything possible; drop to platform code only when required.
- **Reliable & Fast** — instant local save; a saved reading is never lost.
- **Accessible** — large touch targets, dynamic type, clear colors.
- **Open** — clonable and runnable by anyone with no secrets or config (D-027).

## 7. Technology stack (settled — see 02-design-decisions.md for rationale)

| Concern | Choice |
|---|---|
| Platform | Kotlin Multiplatform (Android + iOS) |
| UI | Compose Multiplatform + Material 3 (shared UI in `shared/`) |
| Architecture | Clean Architecture + MVVM + Repository |
| DI | Koin |
| Local DB | Room KMP (bundled SQLite driver, KSP) — the only store |
| Auth / user-data backend | **None** — no login, no Firestore (D-018/D-020) |
| Analytics / crash reporting | Firebase Analytics + Crashlytics only — PHI-free, opt-out (D-028/D-029). Never auth or data |
| Backup / export | Optional: CSV file export + Google Drive (Drive REST via Ktor; authorization-only Google Sign-In, D-020/D-021) |
| Background work | Optional auto-backup only; scheduled via `expect`/`actual` — WorkManager (Android) / BGTaskScheduler (iOS) (D-022) |
| Async | Coroutines + Flow |
| Serialization | kotlinx.serialization (backup JSON) |
| Date/time | kotlinx-datetime |
| Settings / secure storage | multiplatform-settings + EncryptedSharedPreferences (Android) / Keychain (iOS) — guards Drive tokens (D-011) |
| Navigation | Compose Multiplatform Navigation (`org.jetbrains.androidx.navigation`) |
| HTTP | Ktor client (Google Drive REST API) |

## 8. Document map — read in this order

| Doc | What it answers |
|---|---|
| **[00-overview.md](00-overview.md)** | What is VitalCare? Who is it for? What stack? (this file) |
| **[01-product-requirements.md](01-product-requirements.md)** | What exactly must the product do? Features, business rules, validation, NFRs |
| **[02-design-decisions.md](02-design-decisions.md)** | Why each technical/product decision was made (ADR log). New decisions go here first |
| **[03-ui-ux-design.md](03-ui-ux-design.md)** | Every screen in detail: layout, components, states, flows + the design system |
| **[04-architecture.md](04-architecture.md)** | How the code is structured: modules, layers, packages, DI, navigation, expect/actual |
| **[05-backup-and-export.md](05-backup-and-export.md)** | Optional backup/export: CSV, Google Drive authorization, backup/restore flows, scheduling |
| **[06-data-and-storage.md](06-data-and-storage.md)** | Data model, Room schema, and the CSV / JSON backup formats |
| **[07-security.md](07-security.md)** | Security & privacy posture: on-device storage, Drive scope, logging hygiene |
| **[08-testing.md](08-testing.md)** | Test strategy per layer, tooling, commands |
| **[09-roadmap.md](09-roadmap.md)** | Build order: phases with deliverables and acceptance criteria |
| **[10-guidelines.md](10-guidelines.md)** | Coding conventions + instructions for AI assistants working in this repo |

## 9. Status conventions used across these docs

- **Settled** — decided; change requires a new entry in 02-design-decisions.md.
- **Proposed** — recommended default filled in during planning; veto/confirm before the relevant phase.
- **Future** — explicitly deferred; do not build in MVP.

## 10. Glossary

| Term | Meaning |
|---|---|
| **Vital / reading / record** | One saved measurement set (SpO₂ + HR + BP + remarks) at a date/time |
| **Backup** | A full point-in-time JSON snapshot uploaded to the user's Google Drive (D-023) |
| **Restore** | Merging a downloaded backup into the local DB, non-destructively (D-024) |
| **Export** | Writing records to a CSV file the user saves or shares (06 §3) |
| **Profile** | The optional single local display name (D-019) — not an account |
| **appDataFolder** | Google Drive's hidden per-app storage space where backups live (D-021) |
| **LWW** | Last-write-wins (by `updatedAt`) — used only during a restore merge (D-024) |
