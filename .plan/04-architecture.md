# 04 — Architecture

How the application is structured: modules, layers, packages, DI, navigation, and the
platform boundary. Decisions referenced as `D-xxx` live in [02-design-decisions.md](02-design-decisions.md).

## 1. Module structure (D-001, D-010)

```
VitalCare/
├── shared/        KMP module — ALL app code lives here (logic + Compose UI)
│   └── src/
│       ├── commonMain/       everything platform-agnostic (≈95 % of code)
│       ├── androidMain/      Android `actual`s (DB builder, WorkManager, secure storage…)
│       ├── iosMain/          iOS `actual`s (DB path, BGTaskScheduler, Keychain…)
│       ├── commonTest/       shared tests (preferred location for all tests)
│       ├── androidHostTest/  Android-host / JVM-specific tests
│       └── iosTest/          iOS-specific tests
├── androidApp/    thin Android entry — MainActivity hosts shared App()
└── iosApp/        thin iOS entry — Xcode project, ComposeUIViewController via `Shared` framework
```

- `shared` namespace: `com.techgv.vitalcare.shared`; base package `com.techgv.vitalcare`.
- `androidApp` applicationId and iOS bundle id: `com.techgv.vitalcare`.
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
│ DATA           Room, Firestore (GitLive), repository   │
│                impls, sync engine, mappers             │
│                knows: Domain (implements its interfaces)│
└──────────────────────────────────────────────────────┘
```

**Dependency rule:** source dependencies point inward only. Domain is pure `commonMain` Kotlin —
no Room, no Firebase, no Compose imports. Presentation talks to repositories/use cases; it never
touches DAOs or Firestore directly. No business logic in composables.

## 3. Package layout inside `shared/src/commonMain/kotlin/com/techgv/vitalcare/`

```
core/
├── di/                Koin modules + initKoin()
├── designsystem/      VitalCareTheme, tokens, shared components (03-ui-ux §4.5)
├── navigation/        route definitions (@Serializable), NavHost wiring
└── util/              AppResult, date/time helpers, dispatcher provider

domain/
├── model/             Patient, VitalRecord, SyncStatus, VitalsValidationError
├── repository/        AuthRepository, VitalsRepository, SyncRepository (interfaces)
├── usecase/           SaveVitalRecord, GetTodaySummary, GetHistory, GetAnalytics,
│                      DeleteVitalRecord, Login, Register, Logout, …
└── validation/        VitalsValidator (ranges from 01-product-requirements §2)

data/
├── local/             RoomDB: VitalCareDatabase, entities, DAOs, converters,
│                      expect fun databaseBuilder() (actuals per platform)
├── remote/            GitLive wrappers: FirebaseAuthSource, FirestoreVitalsSource
├── repository/        AuthRepositoryImpl, VitalsRepositoryImpl, …
├── sync/              SyncEngine (shared), SyncScheduler (expect), backoff policy
└── settings/          multiplatform-settings wrapper, expect SecureSettings

feature/
├── auth/              LoginScreen/VM, RegisterScreen/VM, ForgotPasswordScreen/VM
├── dashboard/         DashboardScreen/VM
├── vitals/            RecordVitalsScreen/VM (create + edit modes)
├── history/           HistoryScreen/VM, RecordDetailsScreen/VM
├── analytics/         AnalyticsScreen/VM
└── settings/          SettingsScreen/VM

App.kt                 root composable: theme + Koin + NavHost
Platform.kt            expect platform info (existing)
```

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
- One-shot effects (navigation, snackbars) via a `Channel`/`SharedFlow` of `Effect`s — never flags in state that the UI must "reset".
- ViewModels use the multiplatform `androidx.lifecycle.ViewModel` (already a dependency) and
  are injected with **Koin** (`koinViewModel()` in composables).
- Composables are dumb: render state, forward events. No repository/DAO access, no validation logic.

## 5. Dependency injection (Koin, D-002)

Koin modules mirror the package structure:

| Module | Provides |
|---|---|
| `coreModule` | dispatchers, settings, clock |
| `databaseModule` | `VitalCareDatabase` (via platform builder), DAOs |
| `remoteModule` | GitLive Firebase Auth/Firestore sources |
| `repositoryModule` | repository impls bound to domain interfaces |
| `useCaseModule` | use cases (factories) |
| `syncModule` | `SyncEngine`, platform `SyncScheduler` |
| `viewModelModule` | all ViewModels |

- `initKoin(platformModule)` lives in `core/di`, called from `MainActivity`/`MainViewController`
  (each passes its platform module with the `actual` bindings: DB builder, scheduler, secure storage).

## 6. Navigation (D-008)

- Single `NavHost` in `App()`; routes are `@Serializable` classes in `core/navigation`:
  `Splash`, `Login`, `Register`, `ForgotPassword`, `Dashboard`, `RecordVitals(recordId: String? = null)`,
  `History`, `RecordDetails(recordId: String)`, `Analytics`, `Settings`.
- Nested graphs `AuthGraph` / `MainGraph`; auth success navigates to `MainGraph` with
  `popUpTo(AuthGraph) { inclusive = true }` (and the reverse on logout).
- Bottom bar (Dashboard/History/Analytics/Settings) uses `saveState`/`restoreState` for per-tab back stacks.

## 7. expect/actual inventory

The complete list of platform seams — nothing else may be platform-specific:

| `expect` (commonMain) | Android `actual` | iOS `actual` |
|---|---|---|
| Room database builder | `Room.databaseBuilder(context, …)` | builder with documents-dir path |
| `SyncScheduler` | WorkManager (periodic + expedited one-shot, network constraint) | BGTaskScheduler (`BGAppRefreshTask`) + foreground trigger |
| `SecureSettings` | EncryptedSharedPreferences | Keychain |
| `Platform` info (existing) | Build info | UIDevice info |
| Connectivity observer (Proposed) | `ConnectivityManager` callbacks | `NWPathMonitor` |

Firebase needs no expect/actual — GitLive provides the common API (D-004); each app target
carries its native config file (`google-services.json` / `GoogleService-Info.plist`).

## 8. Error handling & threading

- **`AppResult<T>`** — sealed `Success/Failure` with a domain `AppError` enum
  (`Network`, `Auth(reason)`, `Validation(errors)`, `NotFound`, `Unknown(cause)`), returned by
  repositories and use cases. Exceptions are caught at the data layer and mapped; they do not
  cross into presentation.
- **Dispatchers** injected via a `DispatcherProvider` (main/io/default) so tests can substitute;
  DAO calls and Firestore calls run on IO. ViewModels launch in `viewModelScope`.
- Reads are `Flow<T>` from Room (reactive UI, FR-D5); commands are `suspend fun … : AppResult<T>`.

## 9. Data-flow walkthrough — "Save a vital record" (the canonical path)

```
RecordVitalsScreen
  └─ onEvent(SaveClicked)
      └─ RecordVitalsViewModel
          └─ SaveVitalRecord use case
              ├─ VitalsValidator.validate(input)          ← BR-6, ranges §01/2
              │    └─ invalid → AppResult.Failure(Validation) → field errors in UiState
              └─ VitalsRepository.save(record)
                  ├─ VitalRecordDao.insert(entity, syncStatus = PENDING)   ← Room, source of truth
                  ├─ (Room Flow emits → Dashboard/History recompose automatically)
                  └─ SyncScheduler.requestSync()          ← expedited background sync
                       └─ SyncEngine.sync()  (background, later)
                            ├─ dao.getPendingRecords()
                            ├─ Firestore set patients/{uid}/vitals/{recordId}
                            └─ dao.markSynced(id)  /  backoff on failure
```

UI is updated at step 3 (local insert) — everything after that is invisible background work.

## 10. Future modularization (when needed, not now)

Trigger: build time pain or 2+ developers. Split order: `core` → `domain` → `data` →
`feature-*` Gradle modules; the package boundaries in §3 are the future module boundaries.
