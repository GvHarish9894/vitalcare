# Coding Guidelines

- Kotlin only
- Compose Multiplatform only (shared UI)
- MVVM
- SOLID
- Repository Pattern
- Immutable UI State
- Coroutines + Flow
- Small Composables
- No business logic in UI
- Unit tests required

Kotlin Multiplatform
- Put shared code in commonMain and keep it platform-agnostic
- No Android/iOS platform APIs in commonMain — use expect/actual
- Keep androidMain / iosMain thin (only what genuinely can't be shared)
- Use KMP-compatible libraries (Koin, Room KMP, Ktor, kotlinx-*, GitLive Firebase)
