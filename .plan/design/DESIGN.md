# VitalCare — Design Brief (Stitch) · Soft Clinical

This is the design brief used to generate the VitalCare screen designs in **Google Stitch**.
It is derived from [03-ui-ux-design.md](../03-ui-ux-design.md) §4. The visual language was
retuned on 4 Jul 2026 to **"Soft Clinical"** (D-030) — a modern soft-pastel **bento** look
(muted sage / blue / lavender / peach / cream tiles, bold geometric hero numerals, an **indigo**
primary `#4849A1`, full-pill shapes, gentle diffuse depth) — replacing the earlier Material 3
Expressive blue brief. The **accessibility guardrails hold**: near-black text on every tint,
red + icon for out-of-range, ≥ 48–56 dp targets, grandparent-proof clarity. All screens must be
regenerated under this system, and the Stitch design systems below must be recreated to match
(they are still the old blue Expressive palette).

> **⚠ Scope change (2026-07-04, D-018…D-027).** The app dropped authentication and cloud sync
> and became local-only with **optional** CSV export / Google Drive backup. The **Splash, Login,
> Register, and Forgot Password** screens below are **deprecated** — no longer part of the product
> (their Stitch assets are kept only as an archive). The Dashboard has **no sync pill**, History
> rows have **no sync icon**, and Settings needs a **Backup & Export** section (CSV export, Drive
> connect/backup/restore, auto-backup cadence) with **no Logout**. These screens should be
> regenerated to match `.plan/03-ui-ux-design.md` before the designs are used for implementation.

**Stitch project:** `projects/12717657061170960523` — <https://stitch.withgoogle.com/projects/12717657061170960523>
**Design systems (to be recreated for Soft Clinical):** the current "VitalCare Light" (`assets/12295356851068699116`) · "VitalCare Dark" (`assets/16171113615350833674`) are the **old** blue Expressive palette (Inter). Replace with new Soft-Clinical systems (palette below, **Plus Jakarta Sans**, `roundness: ROUND_FULL`) before regenerating screens.

## Design personality

- **Calm, soft, human** — muted pastel bento tiles, generous whitespace, gentle depth; the
  vitals themselves are the hero (big bold numerals). No alarming colors except genuine
  out-of-range / errors.
- **Grandparent-proof (unchanged)** — touch targets ≥ 48 dp (primary 56–64 dp), huge legible
  numerals, one clear primary action per screen, high text contrast.
- **Local-first, quiet backup** — the app works instantly offline with no account; backup is
  optional and never nags (at most a subtle "backed up N ago" line when Drive is connected).

## Soft Clinical — design language rules

- **Soft-tint bento tiles** — group content in rounded pastel tiles (sage / blue / lavender /
  peach / cream) of varying sizes: a full-width hero tile + rows of two smaller tiles. Soft
  diffuse shadows; no hard borders (hairline only on white icon buttons/fields), no heavy shadows.
- **Indigo primary** — primary CTAs are indigo (`#4849A1`) full-width pills with white text
  (like the reference's "Let's start!"); hero tiles/active states use indigo; text stays near-black
  ink; circular white icon buttons for back / search / calendar.
- **Shape** — buttons/chips full pills; cards 24 px radius (hero tiles 28 px); text fields 18 px;
  icons in circular tonal chips.
- **Type (Plus Jakarta Sans)** — hero numerals 44–56 px w800 · screen titles 30–34 px w800 ·
  card titles 17–18 px w700 · body 15–16 px w500 · labels 13 px w600.
- **Charts** — rounded-pill bar charts and line charts, one calm hue per series; the warm accent
  (`#F97A45`) is for energy/emphasis only, **never** a warning.
- **Text on tints is always near-black ink** — never pastel-on-pastel, never light-on-pastel
  (contrast guardrail, D-030).
- **Bottom nav** — floating pill bar with an indigo pill indicator behind the active icon.
- **Text fields** — filled on white/surface, rounded, floating labels, suffix units (%, bpm, mmHg).

## Color tokens

Near-black ink text sits on all of these; the tints are fills only.

| Token | Light | Dark |
|---|---|---|
| Primary (brand/action) | `#4849A1` | `#B9BAF0` |
| On-primary | `#FFFFFF` | `#1B1C52` |
| Primary-container | `#E4E4F4` | `#33346F` |
| Ink (text) / On-surface | `#161616` | `#EDEFEE` |
| Background | `#F3F5F4` | `#121513` |
| Surface | `#FFFFFF` | `#1C201D` |
| On-surface variant | `#6B7280` | `#9BA3A0` |
| Outline | `#E6EAE8` | `#333B37` |
| Tint — Sage | `#D2E4D9` | `#26332B` |
| Tint — Blue | `#D3E3EC` | `#243139` |
| Tint — Lavender | `#E2D9F0` | `#2E2A3D` |
| Tint — Peach | `#F6E1D7` | `#392B25` |
| Tint — Cream | `#EBEDD6` | `#2F3323` |
| Accent (energy, limited) | `#F97A45` | `#FB8A5A` |
| Success / in-range | `#2E9E6B` | `#3BB981` |
| Warning / caution | `#D98A24` | `#E7A73C` |
| Error / out-of-range | `#DC3B34` | `#F26D63` |

## Iconography

Material Symbols (rounded/filled for warmth) in circular tonal chips: favorite (HR),
water_drop (SpO₂), monitor_heart (BP), history, insights, settings, cloud_upload / cloud_done
(Drive backup), download (CSV export), restore, add (record FAB), warning. Small emoji-style
chips (🏅 streak) may accent celebratory moments, sparingly.

## Active screen set (current Stitch assets — old blue Expressive palette)

In-scope screens per the current plan. **All** of these must be regenerated under **Soft Clinical**
(D-030) — the existing assets are the old blue Expressive palette. On top of the restyle,
**Dashboard** drops the sync pill and **Settings** adds the Backup & Export section and removes
Logout (2026-07-04 scope change).

| Screen | Light ID | Dark ID |
|---|---|---|
| Dashboard | `5c5e2743016345f9992dbbe99325ebf1` | `95f79c963b5343549a5abba65f9fa74f` |
| Record Vitals | `ef441b839fb54745a1f40bbad3c7be88` | `443dcec591a344bf8c40d0872662d5be` |
| History | `544af28901204264803ec9054811fbea` | `8a6c4fc4a50344a5a644e3360fbdf3ee` |
| Record Details | `1c86431c17784a9cabc4468df957262c` | `d667e5cf8ae64fb49af7acb189a31552` |
| Analytics | `81442448911d4bcd92936c9914e87417` | `4dc948cf6f7b489396cc4846e82df55a` |
| Settings | `301f955d912d41758adc6a315b8f892a` | `0a50628262f3450da3263303ce5bc445` |

**To design (new):** a **Backup & Export** surface — CSV export, Google Drive connect/backup/
restore, auto-backup cadence (Off/Daily/Weekly/Monthly) — as a Settings section or its own screen.

## Deprecated screens (removed from scope — D-018)

Kept in the Stitch project only as an archive; delete when convenient. Not part of the product.

| Screen | Light ID | Dark ID |
|---|---|---|
| Splash (auth-routing) | `1b88470ea60241ada2aa3494332744e2` | `c8367804600844d2952183c3d388f10b` |
| Login | `a7f05f7faa1b45e5874ac6c73d51af88` | `df31397725d44294992d1fea475dd464` |
| Register | `02251bcab67546a59d7717e0d4dd22b6` | `2947002e52dd4bf6a1c1a849da6c01bc` |
| Forgot Password (form) | `aca9e2637bf1425cadd24a3394c33017` | `7d1c1d219d3a4bbd8f01a99b43acd0e6` |
| Forgot Password (success) | `49d793b036944b50887ea92dcad35665` | `b085903745494aa5be2ce9ad5897d08a` |

The original v1 screens (flat Material 3 look, titles without "(Expressive)") are also archived
and can be deleted in the Stitch UI — including the two v1 duplicates "VitalCare Analytics (Dark)"
(`3ad04bd8…`) and "VitalCare Settings (Dark)" (`badbd03a…`).

Screen URLs follow `https://stitch.withgoogle.com/projects/{project}/screens/{screen}`.
