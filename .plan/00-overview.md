# VitalCare - Overview

## Vision
VitalCare is an offline-first Kotlin Multiplatform application (Android + iOS) for home monitoring of critical patients, built on a shared Compose Multiplatform UI.

## Goals
- Record patient vitals multiple times per day.
- Always save locally first.
- Sync automatically to Google Cloud (Firestore) when online.
- Provide historical trends and charts.
- Be simple enough for elderly users and caregivers.
- Share business logic and UI across Android and iOS from a single codebase.

## Core Principles
- Offline First
- Cross-Platform (share everything possible; drop to platform code only when required)
- Secure
- Reliable
- Fast
- Accessible
- Scalable

## Initial Scope
- Kotlin Multiplatform (Android + iOS)
- Compose Multiplatform UI (shared)
- Koin (dependency injection)
- Room KMP (local database)
- Coroutines + Flow
- Background sync via an expect/actual scheduler (WorkManager on Android, BGTaskScheduler on iOS)
- Firebase Authentication + Firestore via the GitLive Firebase Kotlin SDK
