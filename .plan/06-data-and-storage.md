# 06 — Data Model & Storage

The complete data design: domain models, the Room schema (the app's **sole** store, D-005), and
the on-disk formats used by the backup/export feature (CSV + JSON, D-023). The mechanics of
*moving* data out — Drive OAuth, upload/download, scheduling, restore flows — live in
[05-backup-and-export.md](05-backup-and-export.md); this doc defines *what the data is* and
*how it is serialized*.

> **No cloud store.** There is no Firestore, no live sync, and no `patientId` (D-018/D-019/D-020).
> Room is the source of truth; backups are optional point-in-time snapshots.

## 1. Domain models (`domain/model`, pure Kotlin)

```kotlin
data class VitalRecord(
    val id: String,                  // UUID v4, generated on device at creation
    val date: LocalDate,             // civil date of reading — auto, immutable (BR-4, D-016)
    val time: LocalTime,             // civil time of reading — editable
    val spo2: Int?,                  // 70..100, null = not measured
    val heartRate: Int?,             // 20..250
    val systolic: Int?,              // 50..250   (systolic+diastolic always together)
    val diastolic: Int?,             // 30..180, < systolic
    val remarks: String?,            // ≤ 500 chars
    val createdAt: Long,             // epoch millis UTC
    val updatedAt: Long,             // epoch millis UTC — drives restore-merge LWW (D-024)
)

// Optional single local profile (D-019). Just a display name; lives in settings, not a table.
data class Profile(
    val name: String,                // may be blank / unset
)

// Fluid balance (F9, D-033) — a SEPARATE concept from VitalRecord. One row per discrete
// intake/output event; the app sums them per day.
data class FluidEntry(
    val id: String,                  // UUID v4
    val date: LocalDate,             // civil date — auto, immutable (BR-4)
    val time: LocalTime,             // civil time — editable
    val type: FluidType,             // INTAKE | OUTPUT (OUTPUT = urine)
    val amountMl: Int,               // canonical millilitres, 1..5000, always > 0
    val note: String?,               // optional (≤ 500 chars), null = none
    val createdAt: Long,             // epoch millis UTC
    val updatedAt: Long,             // epoch millis UTC — restore-merge LWW (D-024)
)
enum class FluidType { INTAKE, OUTPUT }
```

- **IDs are client-generated UUIDs** so records have stable identities independent of any server;
  the same id survives export → restore and drives merge deduplication (D-024).
- Null vitals mean "not measured"; validation requires at least one vital present (§01/2).
- There is **no `syncStatus` and no `deleted` flag** — deletes are hard deletes (D-025).

## 2. Room schema (`data/local`)

Entities/DAOs in `commonMain`; builder per platform (D-003).

### `vital_records`
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | UUID |
| `date` | TEXT, **indexed** | ISO-8601 `yyyy-MM-dd` (sorts correctly as text) |
| `time` | TEXT | ISO-8601 `HH:mm` |
| `spo2` / `heartRate` / `systolic` / `diastolic` | INTEGER, nullable | |
| `remarks` | TEXT, nullable | |
| `createdAt` / `updatedAt` | INTEGER | epoch millis UTC |

Index on `date` for history/analytics queries (single-user, so no `patientId` composite).
Type converters: `LocalDate`/`LocalTime` ↔ ISO strings.

There is no `patients` table (D-019). The optional profile name is a single value in
`multiplatform-settings`, not a Room entity.

### DAO surface (shape, not final signatures)

```kotlin
@Dao interface VitalRecordDao {
    @Upsert suspend fun upsert(record: VitalRecordEntity)
    @Query("… WHERE id = :id") suspend fun getById(id: String): VitalRecordEntity?
    // Reactive UI queries:
    fun observeToday(date: String): Flow<List<VitalRecordEntity>>
    fun observeHistory(): Flow<List<VitalRecordEntity>>
    fun observeByDateRange(from: String, to: String): Flow<List<VitalRecordEntity>>
    // Backup/export reads (snapshots, not Flows):
    suspend fun getAll(): List<VitalRecordEntity>                  // full snapshot for backup/CSV
    suspend fun getByDateRange(from: String, to: String): List<VitalRecordEntity>  // filtered CSV
    suspend fun upsertAll(records: List<VitalRecordEntity>)        // restore-merge writes
    // Commands:
    suspend fun hardDelete(id: String)                            // immediate delete (D-025)
    suspend fun countSince(updatedAt: Long): Int                  // "changes since last backup" hint
}
```

### `fluid_entries` (D-033)
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | UUID |
| `date` | TEXT, **indexed** | ISO-8601 `yyyy-MM-dd` |
| `time` | TEXT | ISO-8601 `HH:mm` |
| `type` | TEXT | `INTAKE` / `OUTPUT` (enum name) |
| `amountMl` | INTEGER | canonical millilitres, 1..5000 |
| `note` | TEXT, nullable | ≤ 500 chars |
| `createdAt` / `updatedAt` | INTEGER | epoch millis UTC |

`FluidEntryDao` mirrors `VitalRecordDao` (upsert/upsertAll, getById, observeByDate,
observeByDateRange, observeAll, snapshot getAll/getByDateRange, hardDelete). Index on `date`.

**Migrations:** never destructive in production. Every schema change ships a `Migration`;
`fallbackToDestructiveMigration` is allowed only in debug builds. **Adding `fluid_entries` bumps
`VitalCareDatabase` to `version = 2` with `MIGRATION_1_2`** (`CREATE TABLE fluid_entries …` +
`CREATE INDEX index_fluid_entries_date …`); `vital_records` is unchanged (D-033).

## 3. CSV export format (D-023, RFC 4180)

One header row + one row per record, UTF-8, `\n` line endings. Empty vitals are empty cells
(not `0`). Times/dates are the record's civil `date`/`time`; timestamps are epoch millis.

```
date,time,spo2,heart_rate,systolic,diastolic,remarks,created_at,updated_at
2026-07-03,08:30,98,72,120,80,"Morning, resting",1719990600000,1719990600000
2026-07-03,20:15,,68,118,79,,1720030500000,1720030500000
```

- Fields containing `,`, `"`, or a newline are wrapped in double quotes; embedded `"` is doubled
  (`""`). A small CSV encoder in `core/util` handles this — no external library (keeps the core
  dependency-free, D-027).
- Export scope follows the History filter the user chose (All / Today / Week / Month) — the
  file is produced from `getAll()` or `getByDateRange(...)`.
- CSV is **export-only** (human/spreadsheet consumption). Round-trip restore uses the JSON
  backup, not CSV.

### Fluids CSV (D-033)

Fluid entries export as a **separate** CSV (a fluid event has a different shape from a vitals
reading). Amounts are the canonical mL value.

```
date,time,type,amount_ml,note,created_at,updated_at
2026-07-09,09:00,INTAKE,250,water,1720515600000,1720515600000
2026-07-09,11:30,OUTPUT,300,,1720524600000,1720524600000
```

## 4. JSON backup format (D-023, kotlinx.serialization)

A single document representing a full snapshot. `schemaVersion` lets a future app read older
backups; restore rejects a `schemaVersion` newer than it understands with a clear message.

```jsonc
{
  "schemaVersion": 2,               // bumped 1→2 to add fluids (D-033)
  "exportedAt": 1720080000000,      // epoch millis UTC
  "appVersion": "1.0.0",
  "profileName": "Amma",            // optional (D-019), omitted if unset
  "records": [
    {
      "id": "b3f1…",                // UUID — merge key (D-024)
      "date": "2026-07-03",
      "time": "08:30",
      "spo2": 98, "heartRate": 72, "systolic": 120, "diastolic": 80,
      "remarks": "Morning, resting",
      "createdAt": 1719990600000,
      "updatedAt": 1719990600000
    }
  ],
  "fluids": [                       // D-033; defaulted to [] so v1 backups still decode
    {
      "id": "a91c…",
      "date": "2026-07-09", "time": "09:00",
      "type": "INTAKE", "amountMl": 250, "note": "water",
      "createdAt": 1720515600000, "updatedAt": 1720515600000
    }
  ]
}
```

- **Back-compat:** the serializer uses `ignoreUnknownKeys` + defaulted fields, so a v2 app reads a
  v1 backup (`fluids` defaults to empty) and restore rejects only `schemaVersion` **above** what it
  understands. Fluids merge by the same union-by-id / newer-`updatedAt`-wins rule (D-024).

- **Drive storage:** one file named e.g. `vitalcare-backup.json` in the Drive **`appDataFolder`**
  (hidden app space, D-021/D-023), overwritten on each backup (full snapshot).
- **Optional encryption (D-026, Proposed):** if the user enables it, the document body is
  AES-GCM encrypted from a password-derived key and wrapped as
  `{ schemaVersion, encrypted: true, kdf: {...}, ciphertext: "…" }`. Default is plaintext.
- DTOs + mappers (`data/backup`) translate entity ↔ backup JSON; the same mappers feed CSV.

## 5. Restore = non-destructive merge (D-024)

```
suspend fun restore(backup: BackupFile) {
    requireSupported(backup.schemaVersion)          // else fail with a clear message
    val incoming = backup.records.map { it.toEntity() }
    val local = dao.getAll().associateBy { it.id }
    val toWrite = incoming.filter { new ->
        val cur = local[new.id]
        cur == null || new.updatedAt > cur.updatedAt // add new, or overwrite older-local (LWW)
    }
    dao.upsertAll(toWrite)                           // local-only records are untouched
}
```

- Union by `id`; newer `updatedAt` wins (D-007/D-024). Never deletes local rows.
- Idempotent: restoring the same backup twice changes nothing the second time.
- Records deleted locally but present in an *older* backup reappear on restore — expected
  snapshot semantics (D-025).

## 6. Backup/export triggers & state

- **State kept in settings** (not per-record): `lastBackupAt` (epoch millis) and
  `driveConnected` (bool). No per-record backup flags exist.
- "You have unbacked-up changes" is derived cheaply: `dao.countSince(lastBackupAt) > 0` — no
  schema support needed.
- CSV export and Drive backup/restore flows, scheduling (manual + auto), and platform seams are
  specified in [05-backup-and-export.md](05-backup-and-export.md).

## 7. Analytics queries (F6)

Computed from Room (the only store):
- **Daily:** today's records ordered by time.
- **Weekly/Monthly:** `observeByDateRange` for last 7/30 days, grouped by `date` in the use case;
  per-day averages + range min/max/avg computed in `GetAnalytics` use case (pure, testable).

**Fluid analytics (F9, D-033)** use a **different aggregation** — daily **sums**, not averages.
`GetFluidBalanceToday` combines today's entries into intake/output/net totals + goal progress;
`GetFluidAnalytics` produces per-day **total** intake & output series and a net-balance series
over the range (weekly/monthly), with range totals. `GetAnalytics` (vitals) is left untouched.
