# 06 — Data Model & Sync Engine

The complete data design: domain models, Room schema, Firestore schema, and the offline-first
sync engine. Room is the source of truth (D-005); Firestore is the replica.

## 1. Domain models (`domain/model`, pure Kotlin)

```kotlin
data class Patient(
    val id: String,          // Firebase UID (D-014)
    val name: String,
    val email: String,
)

data class VitalRecord(
    val id: String,                  // UUID v4, generated on device at creation
    val patientId: String,
    val date: LocalDate,             // civil date of reading — auto, immutable (BR-4, D-016)
    val time: LocalTime,             // civil time of reading — editable
    val spo2: Int?,                  // 70..100, null = not measured
    val heartRate: Int?,             // 20..250
    val systolic: Int?,              // 50..250   (systolic+diastolic always together)
    val diastolic: Int?,             // 30..180, < systolic
    val remarks: String?,            // ≤ 500 chars
    val createdAt: Long,             // epoch millis UTC
    val updatedAt: Long,             // epoch millis UTC — drives LWW (D-007)
    val syncStatus: SyncStatus,
)

enum class SyncStatus { PENDING, SYNCING, SYNCED, FAILED }
```

- **IDs are client-generated UUIDs** so records created offline have stable identities that
  become the Firestore document IDs — no re-keying on upload.
- Null vitals mean "not measured"; validation requires at least one vital present (§01/2).

## 2. Room schema (`data/local`)

Entities/DAOs in `commonMain`; builder per platform (D-003).

### `patients`
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | Firebase UID |
| `name` | TEXT | |
| `email` | TEXT | |

### `vital_records`
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | UUID |
| `patientId` | TEXT, **indexed** | FK → patients.id |
| `date` | TEXT, **indexed** | ISO-8601 `yyyy-MM-dd` (sorts correctly as text) |
| `time` | TEXT | ISO-8601 `HH:mm` |
| `spo2` / `heartRate` / `systolic` / `diastolic` | INTEGER, nullable | |
| `remarks` | TEXT, nullable | |
| `createdAt` / `updatedAt` | INTEGER | epoch millis UTC |
| `syncStatus` | TEXT | enum name |
| `deleted` | INTEGER (bool), default 0 | tombstone flag (D-009) |

Composite index `(patientId, date)` for history/analytics queries.
Type converters: `LocalDate`/`LocalTime` ↔ ISO strings, `SyncStatus` ↔ name.

### DAO surface (shape, not final signatures)

```kotlin
@Dao interface VitalRecordDao {
    @Upsert suspend fun upsert(record: VitalRecordEntity)
    @Query("… WHERE id = :id") suspend fun getById(id: String): VitalRecordEntity?
    // UI queries — always exclude tombstones (deleted = 0):
    fun observeToday(patientId: String, date: String): Flow<List<VitalRecordEntity>>
    fun observeHistory(patientId: String): Flow<List<VitalRecordEntity>>
    fun observeByDateRange(patientId: String, from: String, to: String): Flow<List<VitalRecordEntity>>
    fun observePendingCount(patientId: String): Flow<Int>
    // Sync queries — include tombstones:
    suspend fun getUnsynced(patientId: String): List<VitalRecordEntity>   // PENDING or FAILED
    suspend fun setStatus(ids: List<String>, status: SyncStatus)
    suspend fun softDelete(id: String, updatedAt: Long)      // deleted=1, PENDING
    suspend fun hardDelete(id: String)                        // purge (never-synced or post-sync)
    suspend fun wipeAll()                                     // D-015 different-user login
}
```

**Migrations:** never destructive in production. Every schema change ships a `Migration`;
`fallbackToDestructiveMigration` is allowed only in debug builds.

## 3. Firestore schema (`data/remote`, D-004)

```
patients/{patientId}                     // patientId == Firebase UID
├── name: string
├── email: string
├── createdAt: number (epoch millis)
└── vitals/{recordId}                    // recordId == client UUID
    ├── date: string  "2026-07-03"
    ├── time: string  "08:30"
    ├── spo2: number | null
    ├── heartRate: number | null
    ├── systolic: number | null
    ├── diastolic: number | null
    ├── remarks: string | null
    ├── createdAt: number
    └── updatedAt: number                // LWW key (D-007)
```

Fields mirror the local schema 1:1 (minus `syncStatus`/`deleted`, which are local-only).
Writes use `set(merge = true)` so uploads are idempotent. kotlinx.serialization DTOs +
mappers translate entity ↔ document.

## 4. Sync status lifecycle

```
                    save/edit             upload ok
   (new/edited) ──► PENDING ──► SYNCING ──────────► SYNCED
                       ▲            │ upload fails (attempt < max)
                       │            ▼
                       └──────── PENDING (backoff, retry counter++)
                                    │ attempts exhausted
                                    ▼
                                  FAILED ──(manual retry / next connectivity)──► PENDING
```

- Any local edit of a `SYNCED` record resets it to `PENDING` with fresh `updatedAt` (FR-R4).
- `SYNCING` is transient; if the app dies mid-sync, startup demotes stale `SYNCING` rows to `PENDING`.

## 5. Sync engine algorithm (`data/sync`, shared)

```
suspend fun sync(): SyncResult {
    if (authState !is LoggedIn) return Skipped
    val batch = dao.getUnsynced(uid)                    // PENDING + FAILED, includes tombstones
    dao.setStatus(batch.ids, SYNCING)
    for (record in batch) {
        try {
            if (record.deleted) {
                firestore.delete("patients/$uid/vitals/${record.id}")
                dao.hardDelete(record.id)               // tombstone served its purpose
            } else {
                firestore.set("patients/$uid/vitals/${record.id}", record.toDto(), merge = true)
                dao.setStatus(record.id, SYNCED)
            }
        } catch (e: Exception) {
            dao.setStatus(record.id, PENDING or FAILED) // FAILED after max attempts
        }
    }
    settings.lastSyncTime = clock.now()                 // only if any success
}
```

- Records are processed individually — one poison record cannot block the batch.
- Never-synced deletes (still `PENDING`, no upload ever succeeded) are hard-deleted locally
  without any remote call (D-009).

### Pull direction (multi-device) — `Future`
MVP is effectively single-device-at-a-time; the engine's shape anticipates a pull step
(`where updatedAt > lastPulledAt`, apply LWW against local) but it is **not built in MVP**.
Until then, login on a fresh device does an initial full download of `patients/{uid}/vitals`.

## 6. Delete flow (BR-2, D-009)

1. User deletes a today-record → confirmation.
2. If record never reached Firestore → hard delete locally. Done.
3. Else → `softDelete(id)`: `deleted = 1`, `syncStatus = PENDING`, bump `updatedAt`.
   Row disappears from all UI queries immediately.
4. Next sync run deletes the remote doc, then purges the local row.

## 7. Scheduling & triggers (D-006)

| Trigger | Android | iOS |
|---|---|---|
| Record saved/edited/deleted | WorkManager expedited one-shot (network constraint) | immediate attempt if foreground; else next trigger |
| Periodic safety net | WorkManager periodic, 15 min, network-constrained | `BGAppRefreshTask` (opportunistic, OS-decided) |
| App foregrounded | catch-up sync if anything unsynced | same |
| Connectivity regained (Proposed) | WorkManager network constraint handles it | `NWPathMonitor` → trigger while foreground |
| Manual "Sync now" (FR-SE3) | direct `SyncEngine.sync()` call | same |

**Retry/backoff (FR-S3):** exponential — 30 s, 1 m, 2 m, 4 m, 8 m, 16 m (max 6 attempts, jittered);
then `FAILED` until manual retry or next connectivity/foreground event resets the counter.
On Android, WorkManager's own `BackoffPolicy.EXPONENTIAL` is used rather than hand-rolling.

## 8. Conflict resolution (D-007)

On any collision (same record id, differing content): compare `updatedAt`; **newer wins**,
loser is overwritten entirely (whole-record LWW, no field merging). Deletion vs edit:
tombstone wins if its `updatedAt` is newer, else the edit resurrects the record.

## 9. Analytics queries (F6)

Computed from Room (never Firestore):
- **Daily:** today's records ordered by time.
- **Weekly/Monthly:** `observeByDateRange` for last 7/30 days, grouped by `date` in the use case;
  per-day averages + range min/max/avg computed in `GetAnalytics` use case (pure, testable).
