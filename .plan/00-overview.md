# 00 — VitalCare Overview

> **This directory (`.plan/`) is the source of truth for VitalCare.** Every feature, screen,
> and technical choice is specified here before it is built. If code and plan disagree, the
> plan wins — or the plan must be updated first (see [02-design-decisions.md](02-design-decisions.md)).

## 1. Vision

VitalCare is an **offline-first patient-vitals app** for home monitoring of critical patients.
Caregivers and patients record vitals (SpO₂, heart rate, blood pressure) multiple times per day.
Every reading is **saved locally first** — the app is fully usable with no network — and synced
automatically to the cloud (Firestore) when connectivity is available. The app provides history
and trend analytics so caregivers and (later) doctors can see how a patient is doing over time.

## 2. Problem statement

Home monitoring of critical patients today is done on paper or in generic note apps:

- Readings get lost, are hard to search, and impossible to chart.
- Rural / home environments often have poor connectivity — cloud-only apps fail there.
- Elderly users and stressed caregivers need a dead-simple, large-touch-target UI.

VitalCare solves this with a reliable local-first record book that syncs itself.

## 3. Target users

| User | Needs | Priority |
|---|---|---|
| **Critical patients** (often elderly) | Record own vitals with minimal taps; large text and controls | MVP |
| **Family caregivers** | Record vitals for the patient; review history and trends; trust that nothing is lost offline | MVP |
| **Doctors** | Review a patient's trends remotely | Future |

## 4. Goals

1. Record vitals multiple times per day, in under 30 seconds per entry.
2. **Always save locally first** — no network is ever required to record or view data.
3. Sync automatically and invisibly to Firestore when online; show sync status honestly.
4. Provide historical trends and daily/weekly/monthly charts.
5. Be simple and accessible enough for elderly users and stressed caregivers.
6. Share business logic **and UI** across Android and iOS from a single Kotlin codebase.

## 5. Non-goals (explicitly out of scope for MVP)

- Bluetooth / device integration (pulse oximeters, BP cuffs) — manual entry only.
- Multi-patient management per account — one account = one patient (see D-014).
- Doctor-facing portal, sharing, or export (PDF/CSV) — future.
- Medication tracking, appointment reminders, notifications.
- Web or desktop targets.

## 6. Core principles

- **Offline First** — the local database is the source of truth; the cloud is a replica.
- **Cross-Platform** — share everything possible; drop to platform code only when required.
- **Secure** — health data: encrypted at rest where the platform allows, HTTPS only, strict Firestore rules.
- **Reliable** — a saved reading is never lost; sync retries until it succeeds.
- **Fast** — instant save, instant UI update; sync happens in the background.
- **Accessible** — large touch targets, dynamic type, clear status colors.
- **Scalable** — clean layering so features and (later) modules can grow.

## 7. Technology stack (settled — see 02-design-decisions.md for rationale)

| Concern | Choice |
|---|---|
| Platform | Kotlin Multiplatform (Android + iOS) |
| UI | Compose Multiplatform + Material 3 (shared UI in `shared/`) |
| Architecture | Clean Architecture + MVVM + Repository |
| DI | Koin |
| Local DB | Room KMP (bundled SQLite driver, KSP) |
| Auth + Cloud | Firebase Auth + Firestore via GitLive Firebase Kotlin SDK |
| Background sync | Shared sync engine in `commonMain`; scheduled via `expect`/`actual` — WorkManager (Android) / BGTaskScheduler (iOS) |
| Async | Coroutines + Flow |
| Serialization | kotlinx.serialization |
| Date/time | kotlinx-datetime |
| Settings / secure storage | multiplatform-settings + EncryptedSharedPreferences (Android) / Keychain (iOS) |
| Navigation | Compose Multiplatform Navigation (`org.jetbrains.androidx.navigation`) |
| Future REST | Ktor client |

## 8. Document map — read in this order

| Doc | What it answers |
|---|---|
| **[00-overview.md](00-overview.md)** | What is VitalCare? Who is it for? What stack? (this file) |
| **[01-product-requirements.md](01-product-requirements.md)** | What exactly must the product do? Features, business rules, validation, NFRs |
| **[02-design-decisions.md](02-design-decisions.md)** | Why each technical/product decision was made (ADR log). New decisions go here first |
| **[03-ui-ux-design.md](03-ui-ux-design.md)** | Every screen in detail: layout, components, states, flows + the design system |
| **[04-architecture.md](04-architecture.md)** | How the code is structured: modules, layers, packages, DI, navigation, expect/actual |
| **[05-authentication.md](05-authentication.md)** | How auth works end-to-end: register, login, session, offline auth, logout |
| **[06-data-and-sync.md](06-data-and-sync.md)** | Data model, Room schema, Firestore schema, and the sync engine algorithm |
| **[07-security.md](07-security.md)** | Security posture: storage, transport, rules, privacy, logging hygiene |
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
| **Pending** | A record saved locally but not yet uploaded to Firestore |
| **Synced** | A record confirmed written to Firestore |
| **Tombstone** | A locally soft-deleted record kept until its deletion syncs to the cloud |
| **LWW** | Last-write-wins conflict resolution (by `updatedAt` timestamp) |
| **patientId** | Equal to the Firebase Auth UID of the account (one account = one patient) |
