# Architecture

Pattern
- Clean Architecture
- MVVM
- Repository

Platform Model
- Kotlin Multiplatform (Android + iOS)
- Shared code lives in commonMain; platform code via expect/actual in androidMain / iosMain
- No Android/iOS platform APIs in commonMain

Layers
- Presentation — Compose Multiplatform UI + ViewModels
- Domain — models, repository interfaces, use cases
- Data — repository implementations, Room, Firestore, sync

Modules
- shared — KMP module holding all layers (start here)
- androidApp — Android entry point (MainActivity hosts the shared App())
- iosApp — iOS entry point (SwiftUI + ComposeUIViewController)
Split `shared` into multiple KMP modules (core, database, network, sync, feature-*) later if it grows.

shared package layout
- core — common utilities, DI wiring, result types
- data — database, remote (Firestore), repositories, sync
- domain — models, repository interfaces, use cases
- feature-auth / dashboard / vitals / history / analytics / settings — UI + ViewModels

Libraries
- Koin — dependency injection (replaces Hilt; Hilt is Android-only)
- Room KMP — local database
- kotlinx.serialization
- Ktor client — future REST API
- kotlinx-datetime
- GitLive Firebase Kotlin SDK — Auth + Firestore
- Coroutines + Flow
- WorkManager (Android) / BGTaskScheduler (iOS) behind an expect/actual scheduler
