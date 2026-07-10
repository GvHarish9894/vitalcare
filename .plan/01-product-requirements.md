# 01 — Product Requirements

Everything the MVP must do, stated precisely enough to build and test against.
Screens and visuals live in [03-ui-ux-design.md](03-ui-ux-design.md); this doc is *what*, that doc is *how it looks*.

> **Scope (2026-07-04):** VitalCare is a **local-only, no-account** app (D-018). You open it and
> record — no sign-up, no login. Backing up is **optional** (D-020): export to CSV, or connect
> your own Google Drive. See [02-design-decisions.md](02-design-decisions.md) D-018…D-027.

## 1. Feature list (MVP)

| # | Feature | Summary |
|---|---|---|
| F2 | Dashboard | Today's summary, latest reading, quick actions |
| F3 | Record Vitals | Validated entry form; saved locally, instantly |
| F4 | History | All past records grouped by date; search + filter |
| F5 | Record Details | Full view of one record; edit/delete if today's |
| F6 | Analytics | Daily / weekly / monthly trend charts per vital |
| F7 | Backup & Export | Optional CSV export and Google Drive backup/restore |
| F8 | Settings | Profile name, appearance (dark mode), backup controls, about |
| F9 | Fluid Balance | Log water intake & urine output; daily totals, net balance, goal (D-032) |

*(F1 Authentication was removed — the app has no accounts, D-018. Numbering is kept for
continuity with the roadmap.)*

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

### 2a. Fluid entry fields (F9, separate from vitals — D-032)

Fluid tracking is a **separate** concept from the vitals above: each entry is one discrete
event (a drink, or a void), and the app **sums** them per day into totals + net balance. Entries
are **not** part of a `VitalRecord`.

| Field | Type | Entry | Rules |
|---|---|---|---|
| Date | Date | **Auto (today), non-editable** | Set at creation (BR-4) |
| Time | Time | Editable, defaults to now | Cannot be in the future |
| Type | Enum | Toggle **Intake / Output** | Output = urine |
| Amount | Integer (mL, canonical) | Numeric keypad, entered in the display unit | **1–5000 mL** per entry; stored as integer mL |
| Note | Text | Optional | Max 500 chars (e.g. "water", "coffee") |

**Units:** amounts are stored canonically in **millilitres**; a Settings preference chooses the
**display unit — mL or fluid ounces** (US fl oz = 29.5735 mL), converted only for display/entry.
**Daily intake goal:** a Settings value (default **2000 mL**) drives a progress indicator.

## 3. Business rules

| ID | Rule |
|---|---|
| BR-1 | Multiple records are allowed per day — no daily cap. |
| BR-2 | **Only today's records can be edited or deleted.** "Today" = record's date equals the current date in the device's local timezone. |
| BR-3 | Past records are strictly read-only in the UI (no edit/delete affordances shown). |
| BR-4 | Record date is set automatically at creation and can never be changed. |
| BR-5 | Every write (create/edit/delete) goes to the local DB and is applied immediately. Deletes are permanent hard deletes (D-025) — there is no undo and no cloud propagation. |
| BR-6 | Validation happens before save; a record failing validation is never persisted. |
| BR-7 | The app has a single local user; there is no account and no `patientId` (D-019). |

## 4. Functional requirements

### F2 Dashboard
- FR-D1: Show count of today's records.
- FR-D2: Show the latest reading (all vitals + time) or an empty state if none today.
- FR-D3: (Optional, only when Drive is connected) show a subtle backup hint: last backup time,
  or "unbacked-up changes" when `countSince(lastBackupAt) > 0`. No sync/pending status exists.
- FR-D4: Quick actions: Record Vitals (primary), History, Analytics.
- FR-D5: Data updates reactively (Flow from Room) — no manual refresh needed.

### F3 Record Vitals
- FR-R1: Form per §2 field table; inline validation errors on blur and on save.
- FR-R2: Save flow: **validate → insert into Room → update UI instantly**. No network involvement.
- FR-R3: On successful save, return to Dashboard (or previous screen) with confirmation.
- FR-R4: Editing (today only) pre-fills the form; save updates `updatedAt`.

### F4 History
- FR-H1: List all records, newest first, grouped under date headers (Today / Yesterday / `12 May 2026`).
- FR-H2: Each row shows time and the recorded vitals compactly. (No per-record sync indicator — there is no sync.)
- FR-H3: Filter chips: Today / This Week / This Month / All (default All).
- FR-H4: Search matches remarks text (Proposed: also matches exact vital values).
- FR-H5: Tapping a row opens Record Details.
- FR-H6: History overflow offers **Export to CSV** for the current filter scope (F7).

### F5 Record Details
- FR-RD1: Show all fields of one record plus createdAt / updatedAt.
- FR-RD2: If the record is today's: show Edit and Delete actions (BR-2). Delete requires confirmation and is permanent (BR-5).

### F6 Analytics
- FR-AN1: Line/trend chart per vital: SpO₂, Heart Rate, BP (systolic + diastolic on one chart).
- FR-AN2: Range switch: Daily (today's readings by time), Weekly (last 7 days, daily average), Monthly (last 30 days, daily average).
- FR-AN3: Show min / max / average for the selected range.
- FR-AN4: Empty state when the range has no data.

### F7 Backup & Export (details in [05-backup-and-export.md](05-backup-and-export.md))
- FR-B1: **CSV export** — export all or filtered records to a `.csv` file via the platform save/share sheet. No account, no network (05 §3). Fluid entries export as a **separate fluids CSV** (D-032).
- FR-B2: **Connect Google Drive** — optional; runs Google authorization for the Drive scope only (D-021). The app is fully usable without it.
- FR-B3: **Back up now** — upload a full JSON snapshot (vital records **and** fluid entries, backup `schemaVersion` 2 — D-032) to the user's Drive `appDataFolder` (05 §5); records the last-backup time.
- FR-B4: **Auto-backup** — user picks Off (default) / Daily / Weekly / Monthly; scheduled via WorkManager / BGTaskScheduler (D-022).
- FR-B5: **Restore from Drive** — download the backup and **merge** it in (non-destructive, newer-wins, D-024); never wipes local data.
- FR-B6: **Disconnect Drive** — revokes the token, clears it from secure storage, cancels auto-backup.
- FR-B7: (Proposed, D-026) optional password-encrypted backup; default off.

### F8 Settings
- FR-SE1: Profile — optional local display name, editable (D-019).
- FR-SE2: Theme: System / Light / Dark (Proposed default: System).
- FR-SE3: Backup & Export section — CSV export; Drive connect/disconnect; Back up now; Restore; auto-backup cadence; last-backup time (F7).
- FR-SE4: About — app version; link to the open-source project.
- FR-SE5: Privacy — "Share anonymous usage & crash data" toggle controlling Firebase Analytics + Crashlytics; on by default, PHI-free either way (D-028/D-029).
- FR-SE6: Fluids — volume unit (mL / oz) selector and daily intake goal (mL), both persisted in settings (D-032).

### F9 Fluid Balance (D-032)
- FR-FL1: Log a fluid entry per §2a: type (Intake/Output), amount (in the display unit), time, optional note. Validate before save (BR-6); amounts stored canonically in mL.
- FR-FL2: Today view — today's **intake total**, **output total**, **net balance (intake − output)**, and **goal progress** toward the daily intake goal, updating reactively (Flow from Room).
- FR-FL3: Quick-add presets for common amounts (e.g. 250 mL) plus a full entry form for custom amounts/notes and editing.
- FR-FL4: Today's entries list; tap to edit, delete with confirmation. Edit/delete follow BR-2 (today only), BR-5 (hard delete).
- FR-FL5: Trend — daily intake/output **totals** (sum, not average) and net balance over Daily/Weekly/Monthly ranges; empty state when no data.
- FR-FL6: Fluid entries are included in Google Drive backup/restore and exported as a **separate fluids CSV** (F7).
- FR-FL7: The feature is reached from a Dashboard "Fluid balance today" card (no new bottom-nav tab).

## 5. Non-functional requirements

| ID | Requirement | Target |
|---|---|---|
| NFR-1 | Save latency (tap Save → record visible in UI) | < 200 ms |
| NFR-2 | Cold start to Dashboard | < 2.5 s on mid-range device |
| NFR-3 | Offline capability | 100 % of record/read/analytics features work with no network, indefinitely, forever (never any network dependency for core use) |
| NFR-4 | Data durability | A locally saved record survives app kill, reboot, and OS storage pressure |
| NFR-5 | Backup integrity | A backup round-trips losslessly (export → restore reproduces every record); restore is idempotent and non-destructive (D-024) |
| NFR-6 | Accessibility | Touch targets ≥ 48 dp; supports dynamic type; passes basic TalkBack/VoiceOver labeling |
| NFR-7 | Localization | English-only MVP, but **all user-facing strings via Compose resources** — no hardcoded literals |
| NFR-8 | Privacy | Health data never leaves the device unless the user explicitly exports it (CSV) or connects Drive; Drive data goes only to the user's own account (D-020/D-021). The only default outbound data is anonymous, **PHI-free** Analytics/Crashlytics telemetry (D-028), user-disableable (D-029) |
| NFR-9 | No-secret build | The full app builds and runs with no secret configuration and no backend for user data (D-027); telemetry config is committed (client identifiers) and the app still runs without it |

## 6. Acceptance criteria style

Each roadmap phase in [09-roadmap.md](09-roadmap.md) lists acceptance criteria referencing the
FR/BR/NFR IDs above. A feature is "done" only when its criteria pass on **both** Android and iOS.
