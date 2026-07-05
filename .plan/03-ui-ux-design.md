# 03 — UI / UX Design

Every screen, state, and flow, plus the design system. Built with **Compose Multiplatform +
Material 3**, one shared UI for Android and iOS. Requirements references (`FR-*`, `BR-*`) point
to [01-product-requirements.md](01-product-requirements.md).

> **No auth screens.** There is no Splash-time login decision, no Login/Register/Forgot-password
> (D-018). The app opens straight to the Dashboard. Backup/export lives in Settings (§3.10) and is
> entirely optional.

## 1. Design principles

1. **Grandparent-proof** — the primary record flow must be completable by an elderly patient:
   large touch targets (≥ 48 dp), big numerals, one clear primary action per screen. No login to get past.
2. **Calm medical theme** — blue primary, generous whitespace, no alarming colors except for
   genuine errors/out-of-range hints.
3. **Instant feedback** — saving is local, so every action responds immediately.
4. **Backup is quiet and optional** — never nag; backup lives in Settings and, at most, a subtle
   Dashboard hint when Drive is connected.

## 2. Navigation map

```
App launch ──► Dashboard  (no splash gate, no auth)
                Dashboard ─► RecordVitals
                Dashboard ─► History ─► RecordDetails ─► RecordVitals (edit mode)
                Dashboard ─► Analytics
                Dashboard ─► Settings ─► (CSV export · Drive connect/backup/restore)
```

- Single shared `NavHost` in `App()`; type-safe `@Serializable` routes (D-008). One graph — no
  separate auth graph.
- **Bottom navigation bar** on the four top-level destinations: Dashboard · History · Analytics · Settings.
  Record Vitals opens as a full screen pushed on top (not a tab).
- System back: from any top-level tab returns to Dashboard; from Dashboard exits the app.
- An optional brief branded splash may show on cold start, but it makes **no** routing decision —
  it always lands on Dashboard. (Skippable; not required for MVP.)

## 3. Screen specifications

### 3.5 Dashboard (F2)
Top-level tab. App bar: "VitalCare" + date ("Thursday, 3 July").

Vertical scroll of cards (16 dp gutters):

1. **Latest Reading card** (FR-D2) — time of reading + large vitals row: SpO₂ %, HR bpm,
   BP sys/dia. Values outside validation ranges get a warning tint. Empty state: "No readings
   yet today" + inline "Record now" button.
2. **Today card** (FR-D1) — count of today's records; compact list of today's times; tap → History (Today filter).
3. **Quick actions** (FR-D4) — primary **"Record Vitals"** button (full-width, prominent);
   secondary row: History · Analytics.
4. **Backup hint (optional, FR-D3)** — shown **only when Drive is connected**: a slim line such as
   "Backed up 2 days ago" or "You have unbacked-up changes · Back up now →". Absent entirely when
   Drive isn't connected (no nagging to set it up). There is no sync/pending status.

Data is reactive from Room (FR-D5). No network is ever touched on this screen.

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
- **Save** (filled, full-width, sticky above keyboard). Save → Room insert →
  snackbar "Reading saved ✓" → pop back (FR-R2/R3). **No network involvement.**
- Edit mode (from Record Details, today-only per BR-2): pre-filled, same rules; save updates
  `updatedAt` (FR-R4).

### 3.7 History (F4)
Top-level tab. App bar: "History" + search icon (expands to search field, FR-H4) + overflow menu.

- Filter chips row: **All** · Today · This Week · This Month (FR-H3).
- Sticky date headers: "Today", "Yesterday", else "12 May 2026" (FR-H1).
- **Record row** (FR-H2): time (bold) · compact vitals (`98 % · 72 bpm · 120/80`) · chevron.
  Tap → Record Details (FR-H5). No sync indicator (there is no sync).
- **Overflow → Export to CSV** (FR-H6): exports the currently filtered set via the platform
  save/share sheet (05 §3).
- Empty states: no records at all ("Start by recording your first reading" + button);
  no matches for filter/search.
- List is paged/lazy (`LazyColumn`); grouped query from Room.

### 3.8 Record Details (F5)
Pushed from History. App bar: back + date/time title.

- All vitals as labeled large values; remarks block; metadata footer (created, last updated).
- **If record is today's (BR-2/3):** app-bar actions Edit (→ Record Vitals in edit mode) and
  Delete (confirmation dialog: "Delete this reading? This can't be undone.") → **permanent hard
  delete** (D-025) → pop back with snackbar.
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

1. **Profile** — optional local display name; tap to edit inline (D-019). No email, no account.
2. **Appearance** — Theme: System / Light / Dark (FR-SE2).
3. **Backup & Export** (F7) —
   - **Export to CSV** row → scope chooser → platform save/share (FR-B1). Always available.
   - **Google Drive** subsection:
     - Not connected: **Connect Google Drive** button (FR-B2). If the build has no OAuth client
       configured (D-027), the row is shown disabled with "Not set up in this build."
     - Connected: shows the connected state, **Back up now** (FR-B3, spins while running,
       shows last-backup time), **Restore from Drive** (FR-B5, with a "this merges, never
       deletes" note), **Auto-backup** selector Off/Daily/Weekly/Monthly (FR-B4), and
       **Disconnect** (FR-B6, confirmation).
   - (Proposed) **Encrypt backups** toggle + password (FR-B7, D-026).
4. **Privacy** — "Share anonymous usage & crash data" toggle (FR-SE5, D-029); a one-line note that
   it never includes vital values, remarks, or names. On by default.
5. **About** — version; link to the open-source repository (FR-SE4).

*(No Logout row — there is no session.)*

## 4. Design system — "Soft Clinical" (D-030)

A soft, modern wellness aesthetic tuned for clinical clarity and elderly accessibility: off-white
backgrounds, muted multi-pastel **bento** tiles, bold geometric type with big hero numerals, an
**indigo** primary (`#4849A1`) with near-black ink text, full-pill shapes, and gentle diffuse
depth. Built as a **custom Material 3 theme** — not a framework change (D-001). The §5 guardrails are non-negotiable and
**override the aesthetic** wherever they conflict (chiefly: near-black text on every tint; red +
icon for out-of-range).

### 4.1 Color — light (Proposed palette under D-030)

**Neutrals, ink & primary**
| Token | Hex | Usage |
|---|---|---|
| Background | `#F3F5F4` | Screen background (soft warm grey-green) |
| Surface | `#FFFFFF` | Base cards, sheets, app bar, circular icon buttons |
| **Primary (brand/action)** | `#4849A1` | Primary buttons/CTAs, active controls, nav indicator, links, hero tile, primary chart line |
| On-primary | `#FFFFFF` | Text/icons on primary (≈ 7.6:1 — passes AAA) |
| Primary-container | `#E4E4F4` | Soft indigo tint (selected chip, indigo bento tile); text on it `#26275F` |
| Ink (text) | `#161616` | All headings, values & body text — near-black, used on white and **every** pastel tint |
| On-surface variant | `#6B7280` | Secondary text, labels |
| Outline | `#E6EAE8` | Hairline on white icon buttons / fields |

**Bento tile tints** — soft fills, **always** paired with near-black text/values (never text-on-tint)
| Token | Hex | Typical use |
|---|---|---|
| Tint / Sage | `#D2E4D9` | Calm neutral tint, SpO₂ tiles |
| Tint / Blue | `#D3E3EC` | Heart-rate tiles |
| Tint / Lavender | `#E2D9F0` | Blood-pressure tiles |
| Tint / Peach | `#F6E1D7` | Highlights, streaks |
| Tint / Cream | `#EBEDD6` | Neutral stat tiles |

**Accent & semantic**
| Token | Hex | Usage |
|---|---|---|
| Accent (energy, limited) | `#F97A45` | Record FAB, celebratory/streak moments — **never** a warning |
| Success / in-range | `#2E9E6B` | In-range confirmation |
| Warning / caution | `#D98A24` | Amber caution |
| Error / out-of-range | `#DC3B34` | Out-of-range values, delete, failure — always with icon + label |

### 4.2 Color — dark (Proposed)
| Token | Hex |
|---|---|
| Background / Surface | `#121513` / `#1C201D` |
| Primary | `#B9BAF0` (light indigo; on-primary `#1B1C52`) |
| Primary-container | `#33346F` (on it `#E4E4F4`) |
| On-surface / variant | `#EDEFEE` / `#9BA3A0` |
| Outline | `#333B37` |
| Tint Sage / Blue / Lavender / Peach / Cream | `#26332B` / `#243139` / `#2E2A3D` / `#392B25` / `#2F3323` |
| Accent | `#FB8A5A` |
| Success / Warning / Error | `#3BB981` / `#E7A73C` / `#F26D63` |

Both palettes map through Material 3 `lightColorScheme()` / `darkColorScheme()` in `VitalCareTheme`,
with the tile tints + accent as theme-extension colors. Theme setting per FR-SE2.

### 4.3 Typography — bold geometric
- **Display font:** Plus Jakarta Sans (geometric, strong heavy weights) — Proposed; fallback Inter.
- Hero numerals are the star (like the reference's "88 %", "1200 kcal"): big and heavy.

| Role | Spec | Usage |
|---|---|---|
| Hero numeral | 44–56 / w800, tight | Big vital values (Latest Reading, Analytics) |
| Display | 30–34 / w800 | Screen titles ("Your vitals today") |
| Title | 17–18 / w700 | Card titles, date headers |
| Body | 15–16 / w500 | Form inputs, content |
| Label | 13 / w600 | Field labels, chips, unit suffixes, tab labels |

Respect platform font scaling (dynamic type) — never fixed-size text (NFR-6); hero numerals scale.

### 4.4 Shape, spacing, depth — soft & rounded
- **Radius:** cards 24 dp (hero tiles 28 dp); buttons & chips **full pill**; text fields 18 dp; icon buttons full circle.
- **Bento layout:** a full-width hero tile + rows of two smaller tiles of varying tints; 16 dp gaps, 20 dp screen margins.
- **Depth:** soft, diffuse shadows (low-opacity, large-blur) — cards gently float on the light background. No hard borders (hairline outline only on white icon buttons/fields), no heavy shadows.
- **Primary CTA:** full-width indigo (`#4849A1`) pill, white label, ≥ 56 dp tall (like the reference's "Let's start!").
- **Circular icon buttons:** white, 44–48 dp, subtle outline — back (←), calendar, search, etc.

### 4.5 Component inventory (shared, in `core/designsystem`)

| Component | Description |
|---|---|
| `VitalCareTheme` | Custom M3 theme: palettes + tile-tint/accent extensions + Plus Jakarta type + pill/rounded shapes |
| `PrimaryButton` / `SecondaryButton` | Indigo pill (filled) / outlined pill, full-width, loading state built in |
| `CircleIconButton` | White circular icon button (back, search, calendar…) |
| `BentoTile` | Soft-tint rounded tile with a corner icon chip, big value, label — the core building block |
| `VitalValueDisplay` | Hero numeral + unit + label (out-of-range → red + icon) |
| `VitalTextField` | Filled, pill/rounded field, suffix unit, error slot, numeric keypad option |
| `PillBarChart` | Rounded-pill bar chart (weekly/monthly), one tint per series |
| `VitalTrendChart` | Canvas line chart (D-012), per-vital calm hue |
| `RingProgress` | Circular progress ring (e.g., readings logged today) |
| `BackupStatusRow` | Optional last-backup / unbacked-up hint |
| `EmptyState` / `ConfirmDialog` / `SectionHeader` | As before, restyled to the system |
| `BottomNavBar` | Floating pill nav; indigo pill indicator behind the active icon |

*(The former `SyncStatusPill` / `SyncStatusIcon` are removed — there is no sync.)*

### 4.6 Iconography

Rounded/duotone Material Symbols inside circular tonal chips; consistent stroke. Core set:
`favorite` (HR), `water_drop` (SpO₂), `monitor_heart` (BP), `history`, `insights`, `settings`,
`cloud_upload`/`cloud_done` (Drive backup), `download` (CSV export), `restore`, `add` (record FAB),
`warning`. Small emoji-style chips (🏅 streak) may accent celebratory moments — sparingly.

## 5. Accessibility checklist (NFR-6)

- Touch targets ≥ 48 dp; primary Save/Record buttons ≥ 56 dp tall.
- All icons have `contentDescription`s; backup state announced as text, not color alone.
- Color contrast ≥ 4.5:1 for text (near-black ink passes on white and on every pastel tint).
- **Pastel tints are backgrounds only** — text and values on them are always near-black ink,
  never pastel-on-pastel or light-on-pastel (D-030 guardrail).
- Out-of-range warnings use a clear red + icon + label, never a pastel or the brand accent, never color alone.
- Form errors are announced (semantics `error()`), focus moves to first invalid field on save.
- Full dynamic-type support; layouts must not clip at 1.5× font scale.

## 6. Backup & network UX rules

1. The app never blocks on network. The only screens that touch the network are the Drive
   backup/restore actions in Settings, and only when the user taps them.
2. Saving, editing, viewing, and analytics always work offline and say nothing about connectivity.
3. Backup status is passive: at most a subtle Settings/Dashboard line, never a modal or toast storm.
4. A failed backup/restore is a friendly inline message (05 §9) and leaves local data untouched.
5. CSV export needs no account and no network — it's always available.
