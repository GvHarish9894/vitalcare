# 01 — Product Requirements

Everything the MVP must do, stated precisely enough to build and test against.
Screens and visuals live in [03-ui-ux-design.md](03-ui-ux-design.md); this doc is *what*, that doc is *how it looks*.

## 1. Feature list (MVP)

| # | Feature | Summary |
|---|---|---|
| F1 | Authentication | Email/password register, login, forgot password, logout, session persistence |
| F2 | Dashboard | Today's summary, latest reading, sync status, quick actions |
| F3 | Record Vitals | Validated entry form; save-local-first |
| F4 | History | All past records grouped by date; search + filter |
| F5 | Record Details | Full view of one record; edit/delete if today's |
| F6 | Analytics | Daily / weekly / monthly trend charts per vital |
| F7 | Offline Sync | Background upload of pending records; retry; status surfaced in UI |
| F8 | Settings | Profile, appearance (dark mode), sync info, logout |

## 2. Vital fields

| Field | Type | Entry | Rules |
|---|---|---|---|
| Date | Date | **Auto (today), non-editable** | Set at record creation |
| Time | Time | Editable, defaults to now | Cannot be in the future |
| SpO₂ | Integer (%) | Numeric keypad | **70–100** inclusive |
| Heart Rate | Integer (bpm) | Numeric keypad | **20–250** inclusive |
| Blood Pressure — Systolic | Integer (mmHg) | Numeric keypad | **50–250** inclusive |
| Blood Pressure — Diastolic | Integer (mmHg) | Numeric keypad | **30–180** inclusive; must be **< systolic** |
| Remarks | Text | Optional, multiline | Max 500 chars |

**Field requiredness (Proposed):** at least one vital (SpO₂, HR, or BP) must be filled to save;
BP requires both systolic and diastolic together. Empty vitals are stored as null, not 0.

## 3. Business rules

| ID | Rule |
|---|---|
| BR-1 | Multiple records are allowed per day — no daily cap. |
| BR-2 | **Only today's records can be edited or deleted.** "Today" = record's date equals the current date in the device's local timezone. |
| BR-3 | Past records are strictly read-only in the UI (no edit/delete affordances shown). |
| BR-4 | Record date is set automatically at creation and can never be changed. |
| BR-5 | Every write (create/edit/delete) goes to the local DB first and marks the record for sync. Deletes of already-synced records become tombstones until the deletion reaches Firestore (see 06-data-and-sync.md §6). |
| BR-6 | Validation happens before save; a record failing validation is never persisted. |
| BR-7 | One account = one patient. `patientId` is the Firebase Auth UID. |

## 4. Functional requirements

### F1 Authentication (details in [05-authentication.md](05-authentication.md))
- FR-A1: Register with name, email, password (min 8 chars).
- FR-A2: Login with email + password.
- FR-A3: Forgot password → Firebase sends reset email.
- FR-A4: Session persists across app restarts; user lands on Dashboard while logged in.
- FR-A5: Logout returns to Login and stops sync. Local data handling on logout: see D-015.
- FR-A6: After first login, the app is fully usable offline (auth state cached).

### F2 Dashboard
- FR-D1: Show count of today's records.
- FR-D2: Show the latest reading (all vitals + time) or an empty state if none today.
- FR-D3: Show pending-sync count and overall sync state (All synced / N pending / Sync failed / Offline).
- FR-D4: Quick actions: Record Vitals (primary), History, Analytics.
- FR-D5: Data updates reactively (Flow from Room) — no manual refresh needed.

### F3 Record Vitals
- FR-R1: Form per §2 field table; inline validation errors on blur and on save.
- FR-R2: Save flow: **validate → insert into Room (`syncStatus = PENDING`) → update UI instantly → enqueue background sync**.
- FR-R3: On successful save, return to Dashboard (or previous screen) with confirmation.
- FR-R4: Editing (today only) pre-fills the form; save updates `updatedAt` and resets record to `PENDING`.

### F4 History
- FR-H1: List all records, newest first, grouped under date headers (Today / Yesterday / `12 May 2026`).
- FR-H2: Each row shows time, the recorded vitals compactly, and a sync-status indicator.
- FR-H3: Filter chips: Today / This Week / This Month / All (default All).
- FR-H4: Search matches remarks text (Proposed: also matches exact vital values).
- FR-H5: Tapping a row opens Record Details.

### F5 Record Details
- FR-RD1: Show all fields of one record plus createdAt / updatedAt / sync status.
- FR-RD2: If the record is today's: show Edit and Delete actions (BR-2). Delete requires confirmation.

### F6 Analytics
- FR-AN1: Line/trend chart per vital: SpO₂, Heart Rate, BP (systolic + diastolic on one chart).
- FR-AN2: Range switch: Daily (today's readings by time), Weekly (last 7 days, daily average), Monthly (last 30 days, daily average).
- FR-AN3: Show min / max / average for the selected range.
- FR-AN4: Empty state when the range has no data.

### F7 Offline Sync (details in [06-data-and-sync.md](06-data-and-sync.md))
- FR-S1: All `PENDING`/`FAILED` records upload automatically when the device is online.
- FR-S2: Sync runs in the background (WorkManager / BGTaskScheduler) and opportunistically on app foreground.
- FR-S3: Failed uploads retry with exponential backoff; permanent failures surface as `FAILED` with a manual "Retry sync" affordance in Settings/Dashboard.
- FR-S4: Conflict resolution: last-write-wins by `updatedAt`.

### F8 Settings
- FR-SE1: Show profile (name, email).
- FR-SE2: Theme: System / Light / Dark (Proposed default: System).
- FR-SE3: Sync section: last successful sync time, pending count, manual "Sync now".
- FR-SE4: Logout (confirmation dialog).
- FR-SE5: About: app version.

## 5. Non-functional requirements

| ID | Requirement | Target |
|---|---|---|
| NFR-1 | Save latency (tap Save → record visible in UI) | < 200 ms |
| NFR-2 | Cold start to Dashboard (logged in) | < 2.5 s on mid-range device |
| NFR-3 | Offline capability | 100 % of record/read features work with no network, indefinitely |
| NFR-4 | Data durability | A locally saved record survives app kill, reboot, and OS storage pressure |
| NFR-5 | Sync convergence | Pending records reach Firestore within 1 min of connectivity (foreground) / next scheduled slot (background) |
| NFR-6 | Accessibility | Touch targets ≥ 48 dp; supports dynamic type; passes basic TalkBack/VoiceOver labeling |
| NFR-7 | Localization | English-only MVP, but **all user-facing strings via Compose resources** — no hardcoded literals |
| NFR-8 | Privacy | Health data never leaves the device except to the user's own Firestore documents |

## 6. Acceptance criteria style

Each roadmap phase in [09-roadmap.md](09-roadmap.md) lists acceptance criteria referencing the
FR/BR/NFR IDs above. A feature is "done" only when its criteria pass on **both** Android and iOS.
