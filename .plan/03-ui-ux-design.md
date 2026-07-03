# 03 — UI / UX Design

Every screen, state, and flow, plus the design system. Built with **Compose Multiplatform +
Material 3**, one shared UI for Android and iOS. Requirements references (`FR-*`, `BR-*`) point
to [01-product-requirements.md](01-product-requirements.md).

## 1. Design principles

1. **Grandparent-proof** — the primary record flow must be completable by an elderly patient:
   large touch targets (≥ 48 dp), big numerals, one clear primary action per screen.
2. **Calm medical theme** — blue primary, generous whitespace, no alarming colors except for
   genuine errors/out-of-range hints.
3. **Honest offline UX** — never fake success or block on network. Sync state is always visible,
   never modal. Offline is a normal state, not an error state.
4. **Instant feedback** — saving is local, so every action responds immediately.

## 2. Navigation map

```
Splash (route: "splash")
 ├─ not authenticated ──► Auth graph
 │                         Login ⇄ Register
 │                         Login ─► ForgotPassword
 │                         (successful auth ─► Dashboard, auth graph popped)
 └─ authenticated ──────► Main graph
                           Dashboard ─► RecordVitals
                           Dashboard ─► History ─► RecordDetails ─► RecordVitals (edit mode)
                           Dashboard ─► Analytics
                           Dashboard ─► Settings
```

- Single shared `NavHost` in `App()`; type-safe `@Serializable` routes (D-008).
- Auth and Main are separate nested graphs; login/logout swaps graphs and clears back stack.
- **Bottom navigation bar** on the four top-level destinations: Dashboard · History · Analytics · Settings.
  Record Vitals opens as a full screen pushed on top (not a tab).
- System back: from any top-level tab returns to Dashboard; from Dashboard exits the app.

## 3. Screen specifications

### 3.1 Splash
| | |
|---|---|
| Purpose | Brand moment + route decision (session restore) |
| Content | Centered app logo + name on `background` color |
| Logic | Observe auth state (≤ ~500 ms): authenticated → Dashboard; else → Login. No spinner unless restore exceeds 500 ms |
| States | Single state; no user interaction |

### 3.2 Login (FR-A2)
| | |
|---|---|
| Layout | Logo small at top → "Welcome back" headline → Email field → Password field (show/hide toggle) → **Login** (filled, full-width) → "Forgot password?" (text button) → divider → "New here? **Create account**" |
| Validation | Email format; password non-empty. Inline errors below fields on submit |
| Loading | Button shows inline progress, form disabled |
| Errors | Auth failures as inline banner above button, mapped to friendly text (see 05-authentication.md §7). Offline: "No connection — login needs internet the first time." |
| Nav | Success → Dashboard (clear auth graph). Register / Forgot Password links |

### 3.3 Register (FR-A1)
| | |
|---|---|
| Layout | "Create account" headline → Name → Email → Password (helper: "Min 8 characters") → **Create account** (filled) → "Already have an account? **Login**" |
| Validation | Name non-empty; email format; password ≥ 8 chars |
| On success | Create Firebase user → create `patients/{uid}` profile doc → auto-login → Dashboard |
| Errors | email-already-in-use, weak-password, offline — inline banner |

### 3.4 Forgot Password (FR-A3)
| | |
|---|---|
| Layout | Explainer text → Email field → **Send reset link** |
| Success | Confirmation state: "Check your email" + back to Login |
| Note | Do not reveal whether the email exists (send generic success) — see 07-security.md |

### 3.5 Dashboard (F2)
Top-level tab. App bar: "VitalCare" + date ("Thursday, 3 July").

Vertical scroll of cards (16 dp gutters):

1. **Sync status strip** (FR-D3) — pill under the app bar:
   `✓ All synced` (success tint) · `↻ 3 pending` (warning tint) · `⚠ Sync failed — tap to retry` (error tint) · `⊘ Offline — will sync later` (neutral).
2. **Latest Reading card** (FR-D2) — time of reading + large vitals row: SpO₂ %, HR bpm,
   BP sys/dia. Values outside validation ranges get a warning tint. Empty state: "No readings
   yet today" + inline "Record now" button.
3. **Today card** (FR-D1) — count of today's records; compact list of today's times; tap → History (Today filter).
4. **Quick actions** (FR-D4) — primary **"Record Vitals"** button (full-width, prominent);
   secondary row: History · Analytics.

FAB: none (primary action is the Record Vitals button). Data is reactive from Room (FR-D5).

### 3.6 Record Vitals (F3, FR-R1..R4)
Pushed full-screen. App bar: "Record Vitals" / "Edit Record" (edit mode), close (✕) with
discard-confirmation if dirty.

Form (vertical, numeric keypads):

| Field | Control | Notes |
|---|---|---|
| Date | Read-only text ("Today, 3 July 2026") | BR-4; visually non-editable |
| Time | Time field, default now, tap → platform time picker | Future times rejected |
| SpO₂ | Numeric field, suffix "%" | Range hint under field: "70–100" |
| Heart Rate | Numeric field, suffix "bpm" | "20–250" |
| Blood Pressure | Two fields side-by-side: Systolic / Diastolic, suffix "mmHg" | Cross-field: dia < sys |
| Remarks | Multiline text, 3 rows, counter /500 | Optional |

- Inline error text under invalid fields on blur + on save (FR-R1, BR-6).
- At least one vital required — "Enter at least one vital" if all empty.
- **Save** (filled, full-width, sticky above keyboard). Save → Room insert (`PENDING`) →
  snackbar "Reading saved ✓" → pop back (FR-R2/R3). **No network involvement.**
- Edit mode (from Record Details, today-only per BR-2): pre-filled, same rules; save updates
  `updatedAt`, resets to `PENDING` (FR-R4).

### 3.7 History (F4)
Top-level tab. App bar: "History" + search icon (expands to search field, FR-H4).

- Filter chips row: **All** · Today · This Week · This Month (FR-H3).
- Sticky date headers: "Today", "Yesterday", else "12 May 2026" (FR-H1).
- **Record row** (FR-H2): time (bold) · compact vitals (`98 % · 72 bpm · 120/80`) ·
  sync-status icon (✓ synced / ↻ pending / ⚠ failed) · chevron. Tap → Record Details (FR-H5).
- Empty states: no records at all ("Start by recording your first reading" + button);
  no matches for filter/search.
- List is paged/lazy (`LazyColumn`); grouped query from Room.

### 3.8 Record Details (F5)
Pushed from History. App bar: back + date/time title.

- All vitals as labeled large values; remarks block; metadata footer (created, last updated, sync status).
- **If record is today's (BR-2/3):** app-bar actions Edit (→ Record Vitals in edit mode) and
  Delete (confirmation dialog: "Delete this reading? This can't be undone.") → tombstone
  soft-delete (D-009) → pop back with snackbar.
- If past: read-only, no actions shown.

### 3.9 Analytics (F6)
Top-level tab. App bar: "Analytics".

- Range selector (segmented): **Daily · Weekly · Monthly** (FR-AN2).
- One card per vital — SpO₂, Heart Rate, Blood Pressure (sys + dia as two lines, FR-AN1):
  chart title + trend chart (`VitalTrendChart`, D-012) + min / avg / max row (FR-AN3).
- Daily: today's readings by time-of-day. Weekly: last 7 days, one point per day (daily average).
  Monthly: last 30 days, daily averages.
- Empty state per card when the range has no data (FR-AN4).

### 3.10 Settings (F8)
Top-level tab. Sectioned list:

1. **Profile** — avatar initial, name, email (read-only MVP).
2. **Appearance** — Theme: System / Light / Dark (FR-SE2).
3. **Sync** — last successful sync time; pending count; **Sync now** button (spins while running); failed-count with retry (FR-SE3, FR-S3).
4. **About** — version.
5. **Logout** — destructive-styled row → confirmation dialog ("You'll need internet to log back in. Unsynced readings stay on this device." per D-015) (FR-SE4).

## 4. Design system

### 4.1 Color — light theme (Settled)

| Token | Hex | Usage |
|---|---|---|
| Primary | `#2563EB` | Buttons, active states, links, chart lines |
| Success | `#16A34A` | Synced status, in-range confirmations |
| Warning | `#F59E0B` | Pending sync, out-of-range value hints |
| Error | `#DC2626` | Validation errors, failed sync, delete |
| Background | `#F8FAFC` | Screen background |
| Surface | `#FFFFFF` | Cards, sheets, app bar |
| On-surface | `#0F172A` | Primary text |
| On-surface variant | `#64748B` | Secondary text, labels |
| Outline | `#E2E8F0` | Dividers, field borders |

### 4.2 Color — dark theme (Proposed, complements the settled light palette)

| Token | Hex |
|---|---|
| Primary | `#3B82F6` |
| Success | `#22C55E` |
| Warning | `#FBBF24` |
| Error | `#EF4444` |
| Background | `#0F172A` |
| Surface | `#1E293B` |
| On-surface | `#F1F5F9` |
| On-surface variant | `#94A3B8` |
| Outline | `#334155` |

Dark theme is supported from day one; theme setting per FR-SE2. Map both palettes through
Material 3 `lightColorScheme()` / `darkColorScheme()` in a shared `VitalCareTheme`.

### 4.3 Typography (Material 3 defaults, roles pinned)

| Role | Usage |
|---|---|
| `displaySmall` | Big vital values (Latest Reading, Record Details) |
| `headlineSmall` | Screen headlines ("Welcome back") |
| `titleMedium` | Card titles, date headers |
| `bodyLarge` | Form inputs, primary content |
| `bodyMedium` | Secondary content, remarks |
| `labelMedium` | Field labels, chips, status pills |

Respect platform font scaling (dynamic type) — never fixed-size text (NFR-6).

### 4.4 Shape, spacing, elevation

- Spacing scale: **4 / 8 / 16 / 24 / 32 dp** (8 dp grid; 4 dp only for icon-text gaps).
- Corner radius: **16 dp** cards & buttons; 12 dp text fields; full (pill) for chips/status.
- Elevation: cards 1 dp; prefer outline + tonal contrast over heavy shadows.
- Screen margins: 16 dp horizontal; 16 dp between cards.

### 4.5 Component inventory (shared, in `core/designsystem`)

| Component | Description |
|---|---|
| `VitalCareTheme` | M3 theme wrapper: palettes above + typography + shapes |
| `PrimaryButton` / `SecondaryButton` | Filled / outlined, full-width variants, loading state built in |
| `VitalTextField` | Labeled field with suffix unit, error slot, numeric keypad option |
| `VitalCard` | Surface card, 16 dp radius, standard padding |
| `SyncStatusPill` | The 4-state sync indicator (§3.5) |
| `SyncStatusIcon` | Row-level ✓/↻/⚠ indicator |
| `VitalValueDisplay` | Big value + unit + label (with out-of-range tint) |
| `EmptyState` | Icon + message + optional action button |
| `ConfirmDialog` | Standard confirmation (delete, logout, discard) |
| `VitalTrendChart` | Canvas line chart (D-012) |
| `SectionHeader` | Settings/History section titles |

### 4.6 Iconography

Material Symbols (outlined). Core set: `favorite` (HR), `spo2`/`water_drop` (SpO₂), `monitor_heart`
(BP), `history`, `insights` (analytics), `settings`, `sync`, `cloud_done`, `cloud_off`, `warning`.

## 5. Accessibility checklist (NFR-6)

- Touch targets ≥ 48 dp; primary Save/Record buttons ≥ 56 dp tall.
- All icons and status indicators have `contentDescription`s; sync state announced as text, not color alone.
- Color contrast ≥ 4.5:1 for text (both palettes above pass on their surfaces).
- Out-of-range warnings use icon + text, not color alone.
- Form errors are announced (semantics `error()`), focus moves to first invalid field on save.
- Full dynamic-type support; layouts must not clip at 1.5× font scale.

## 6. Offline & sync UX rules

1. Offline is shown passively (status pill), never as a blocking dialog or toast storm.
2. Saving always succeeds offline — messaging is "saved" (true), never "will be saved".
3. Pending records are individually marked in History; the aggregate count lives on Dashboard/Settings.
4. `FAILED` state (retries exhausted) is actionable: tappable pill → manual retry (FR-S3).
5. First-run login/register are the only network-required moments and say so explicitly (FR-A6).
