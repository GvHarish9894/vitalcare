# 04 — Architecture

How the application is structured: modules, layers, packages, DI, navigation, and the
platform boundary. Decisions referenced as `D-xxx` live in [02-design-decisions.md](02-design-decisions.md).

## 1. Module structure (D-001, D-010)

```
VitalCare/
├── shared/        KMP module — ALL app code lives here (logic + Compose UI)
│   └── src/
│       ├── commonMain/       everything platform-agnostic (≈95 % of code)
│       ├── androidMain/      Android `actual`s (DB builder, Drive auth, file export, scheduler…)
│       ├── iosMain/          iOS `actual`s (DB path, Drive auth, file export, scheduler…)
│       ├── commonTest/       shared tests (preferred location for all tests)
│       ├── androidHostTest/  Android-host / JVM-specific tests
│       └── iosTest/          iOS-specific tests
├── androidApp/    thin Android entry — MainActivity hosts shared App()
└── iosApp/        thin iOS entry — Xcode project, ComposeUIViewController via `Shared` framework
```

- `shared` namespace: `com.techgv.vitalcare.shared`; base package `com.techgv.vitalcare`.
- `androidApp` applicationId and iOS bundle id: `com.techgv.vitalcare`.
- **No auth/data backend and no Firestore** (D-018/D-020); Firebase is **telemetry-only**
  (Analytics + Crashlytics, D-028). The app builds and runs with no secret configuration and no
  backend for user data (D-027/NFR-9) — even with the Firebase config absent.
- One Gradle module for now; the package layout below is designed so a later split into
  `core`/`data`/`domain`/`feature-*` modules is mechanical (D-010).

## 2. Layers (Clean Architecture + MVVM)

```
┌──────────────────────────────────────────────────────┐
│ PRESENTATION   Compose screens + ViewModels           │
│                knows: Domain. never: Data internals   │
├──────────────────────────────────────────────────────┤
│ DOMAIN         models, repository interfaces,         │
│                use cases, validation                   │
│                knows: nothing above/below (pure Kotlin)│
├──────────────────────────────────────────────────────┤
│ DATA           Room, backup/export (Drive + CSV),      │
│                repository impls, mappers               │
│                knows: Domain (implements its interfaces)│
└──────────────────────────────────────────────────────┘
```

**Dependency rule:** source dependencies point inward only. Domain is pure `commonMain` Kotlin —
no Room, no Ktor, no Compose imports. Presentation talks to repositories/use cases; it never
touches DAOs or the Drive client directly. No business logic in composables.

## 3. Package layout inside `shared/src/commonMain/kotlin/com/techgv/vitalcare/`

```
core/
├── di/                Koin modules + initKoin()
├── designsystem/      VitalCareTheme, tokens, shared components (03-ui-ux §4.5)
├── navigation/        route definitions (@Serializable), NavHost wiring
├── telemetry/         expect Telemetry (analytics events + crash reporting), PHI-free (D-028)
└── util/              AppResult, date/time helpers, dispatcher provider, CsvEncoder

domain/
├── model/             VitalRecord, Profile, VitalsValidationError, BackupCadence
├── repository/        VitalsRepository, BackupRepository, ProfileRepository (interfaces)
├── usecase/           SaveVitalRecord, GetTodaySummary, GetHistory, GetAnalytics,
│                      DeleteVitalRecord, ExportCsv, BackupNow, RestoreFromDrive,
│                      ConnectDrive, SetAutoBackup, …
└── validation/        VitalsValidator (ranges from 01-product-requirements §2)

data/
├── local/             RoomDB: VitalCareDatabase, entities, DAOs, converters,
│                      expect fun databaseBuilder() (actuals per platform)
├── backup/            BackupSerializer (JSON, kotlinx.serialization), DriveClient (Ktor),
│                      expect DriveAuthorizer, expect FileExporter, expect BackupScheduler,
│                      (optional) BackupCrypto (D-026), mappers entity↔DTO↔CSV
├── repository/        VitalsRepositoryImpl, BackupRepositoryImpl, ProfileRepositoryImpl
└── settings/          multiplatform-settings wrapper, expect SecureSettings (Drive token)

feature/
├── dashboard/         DashboardScreen/VM
├── vitals/            RecordVitalsScreen/VM (create + edit modes)
├── history/           HistoryScreen/VM, RecordDetailsScreen/VM
├── analytics/         AnalyticsScreen/VM
└── settings/          SettingsScreen/VM (includes Backup & Export section)

App.kt                 root composable: theme + Koin + NavHost
Platform.kt            expect platform info (existing)
```

*(No `feature/auth` and no `data/remote` — auth and Firestore are gone, D-018/D-020.)*

## 4. MVVM contract (uniform across features)

Every feature follows the same shape:

```kotlin
// UI state: single immutable data class per screen
data class RecordVitalsUiState(
    val time: LocalTime,
    val spo2: String = "",
    val fieldErrors: Map<VitalField, StringResource> = emptyMap(),
    val isSaving: Boolean = false,
    // …
)

class RecordVitalsViewModel(
    private val saveVitalRecord: SaveVitalRecord,   // use case, injected by Koin
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordVitalsUiState(...))
    val uiState: StateFlow<RecordVitalsUiState> = _uiState.asStateFlow()

    fun onEvent(event: RecordVitalsEvent) { /* single entry point for UI events */ }
}
```

Rules:
- One `UiState` data class + one sealed `Event` type per screen; UI observes a single `StateFlow`.
- One-shot effects (navigation, snackbars, "share this CSV file") via a `Channel`/`SharedFlow` of
  `Effect`s — never flags in state that the UI must "reset".
- ViewModels use the multiplatform `androidx.lifecycle.ViewModel` and are injected with **Koin**
  (`koinViewModel()` in composables).
- Composables are dumb: render state, forward events. No repository/DAO access, no validation logic.

## 5. Dependency injection (Koin, D-002)

Koin modules mirror the package structure:

| Module | Provides |
|---|---|
| `coreModule` | dispatchers, settings, clock, CSV encoder |
| `telemetryModule` | platform `Telemetry` (Firebase Analytics + Crashlytics actuals, D-028); no-op impl when config absent |
| `databaseModule` | `VitalCareDatabase` (via platform builder), DAOs |
| `backupModule` | `BackupSerializer`, `DriveClient`, platform `DriveAuthorizer`/`FileExporter`/`BackupScheduler` |
| `repositoryModule` | repository impls bound to domain interfaces |
| `useCaseModule` | use cases (factories) |
| `viewModelModule` | all ViewModels |

- `initKoin(platformModule)` lives in `core/di`, called from `MainActivity`/`MainViewController`
  (each passes its platform module with the `actual` bindings: DB builder, Drive authorizer,
  file exporter, backup scheduler, secure storage).

## 6. Navigation (D-008)

- Single `NavHost` in `App()`; routes are `@Serializable` classes in `core/navigation`:
  `Dashboard`, `RecordVitals(recordId: String? = null)`, `History`, `RecordDetails(recordId: String)`,
  `Analytics`, `Settings`. **No auth routes and no auth graph** (D-018) — `Dashboard` is the start
  destination.
- Bottom bar (Dashboard/History/Analytics/Settings) uses `saveState`/`restoreState` for per-tab back stacks.

## 7. expect/actual inventory

The complete list of platform seams — nothing else may be platform-specific:

| `expect` (commonMain) | Android `actual` | iOS `actual` |
|---|---|---|
| Room database builder | `Room.databaseBuilder(context, …)` | builder with documents-dir path |
| `DriveAuthorizer` (D-021) | Google Identity Services / `AuthorizationClient`, `drive.file` scope | GoogleSignIn SDK, `drive.file` scope |
| `FileExporter` (CSV save/share) | `ACTION_CREATE_DOCUMENT` / share `Intent` | `UIDocumentPicker` / `UIActivityViewController` |
| `BackupScheduler` (D-022) | WorkManager (periodic, network-constrained) | BGTaskScheduler (`BGAppRefreshTask`) |
| `SecureSettings` (Drive token) | EncryptedSharedPreferences | Keychain |
| `Telemetry` (D-028, PHI-free) | Firebase Analytics + Crashlytics (native SDK) | FirebaseAnalytics + FirebaseCrashlytics (native SDK) |
| `Platform` info (existing) | Build info | UIDevice info |
| Connectivity observer (Proposed, backup only) | `ConnectivityManager` | `NWPathMonitor` |

The **`DriveClient`** (Ktor calls to the Drive REST API) is fully shared `commonMain` code — only
the token acquisition (`DriveAuthorizer`) is platform-specific.

## 8. Error handling & threading

- **`AppResult<T>`** — sealed `Success/Failure` with a domain `AppError` enum
  (`Network`, `Validation(errors)`, `NotFound`, `Drive(reason)`, `Unknown(cause)`), returned by
  repositories and use cases. Exceptions are caught at the data layer and mapped; they do not
  cross into presentation.
- **Dispatchers** injected via a `DispatcherProvider` (main/io/default) so tests can substitute;
  DAO calls, file IO, and Ktor calls run on IO. ViewModels launch in `viewModelScope`.
- Reads are `Flow<T>` from Room (reactive UI, FR-D5); commands are `suspend fun … : AppResult<T>`.

## 9. Data-flow walkthroughs

### "Save a vital record" (the canonical local path)

```
RecordVitalsScreen
  └─ onEvent(SaveClicked)
      └─ RecordVitalsViewModel
          └─ SaveVitalRecord use case
              ├─ VitalsValidator.validate(input)          ← BR-6, ranges §01/2
              │    └─ invalid → AppResult.Failure(Validation) → field errors in UiState
              └─ VitalsRepository.save(record)
                  └─ VitalRecordDao.upsert(entity)         ← Room, the only store
                       └─ (Room Flow emits → Dashboard/History recompose automatically)
```

Everything is local; no network is ever touched. The UI updates the instant Room emits.

### "Back up to Drive" (opt-in, only when the user acts)

```
SettingsScreen (Back up now)
  └─ BackupNow use case
      └─ BackupRepository.backupNow()
          ├─ dao.getAll() → BackupSerializer → JSON            (06 §4)
          ├─ (optional) BackupCrypto.encrypt(...)              (D-026)
          ├─ DriveAuthorizer.accessToken()                     (platform)
          ├─ DriveClient.upsertAppDataFile(...)  (Ktor, shared)
          └─ settings.lastBackupAt = now
```

## 10. Future modularization (when needed, not now)

Trigger: build time pain or 2+ developers. Split order: `core` → `domain` → `data` →
`feature-*` Gradle modules; the package boundaries in §3 are the future module boundaries.
